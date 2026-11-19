package com.jewelerp.service;

import com.jewelerp.config.ApiException;
import com.jewelerp.dto.Requests;
import com.jewelerp.model.AppUser;
import com.jewelerp.model.Customer;
import com.jewelerp.model.InventoryItem;
import com.jewelerp.model.Invoice;
import com.jewelerp.model.InvoiceItem;
import com.jewelerp.model.InvoicePayment;
import com.jewelerp.model.LoyaltyTransaction;
import com.jewelerp.model.OldGoldPurchase;
import com.jewelerp.model.StockMovement;
import com.jewelerp.pricing.JewelleryPricing;
import com.jewelerp.pricing.JewelleryPricing.InvoiceBreakdown;
import com.jewelerp.pricing.JewelleryPricing.ItemBreakdown;
import com.jewelerp.pricing.JewelleryPricing.ItemInput;
import com.jewelerp.repository.BranchRepository;
import com.jewelerp.repository.CustomerRepository;
import com.jewelerp.repository.InventoryItemRepository;
import com.jewelerp.repository.InvoiceRepository;
import com.jewelerp.repository.LoyaltyTransactionRepository;
import com.jewelerp.repository.OldGoldPurchaseRepository;
import com.jewelerp.repository.StockMovementRepository;
import com.jewelerp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PosService {

    private static final BigDecimal LOYALTY_RUPEES_PER_POINT = new BigDecimal("500");

    private final JewelleryPricing pricing;
    private final MetalRateService rates;
    private final InventoryItemRepository inventory;
    private final InvoiceRepository invoices;
    private final CustomerRepository customers;
    private final BranchRepository branches;
    private final UserRepository users;
    private final OldGoldPurchaseRepository oldGold;
    private final StockMovementRepository movements;
    private final LoyaltyTransactionRepository loyalty;
    private final AuditService audit;

    public record CatalogItem(
            Long id, String itemCode, String barcode, String huid, String name, String metalType, String purity,
            BigDecimal grossWeight, BigDecimal stoneWeight, BigDecimal netWeight, BigDecimal metalRate,
            String makingChargeType, BigDecimal makingChargeRate, BigDecimal wastagePct,
            BigDecimal stoneCharge, BigDecimal diamondCharge, BigDecimal hallmarkCharge, BigDecimal otherCharge,
            BigDecimal gstRate, Long branchId
    ) {
    }

    public List<CatalogItem> availableItems() {
        return inventory.findByStatus("available").stream().map(i -> new CatalogItem(
                i.getId(), i.getItemCode(), i.getBarcode(), i.getHuid(), i.getProduct().getName(),
                i.getProduct().getMetalType(), i.getPurity(), i.getGrossWeight(), i.getStoneWeight(),
                i.getNetWeight(), rates.latestRate(i.getProduct().getMetalType(), i.getPurity(), i.getBranch().getId()),
                i.getProduct().getMakingChargeType(), i.getProduct().getMakingChargeValue(),
                i.getProduct().getWastagePct(), i.getStoneCharge(), i.getDiamondCharge(), i.getHallmarkCharge(),
                i.getOtherCharge(), i.getProduct().getGstRate(), i.getBranch().getId()
        )).toList();
    }

    public List<OldGoldPurchase> unusedOldGold() {
        return oldGold.findByStatusAndInvoiceIdIsNullOrderByIdDesc("approved");
    }

    public InvoiceBreakdown preview(Requests.PosPreview req) {
        return pricing.calculateInvoice(toInputs(req.items()), new JewelleryPricing.InvoiceOptions(
                req.discountAmount(), req.gstType(), req.roundingMode(), oldGoldTotal(req.oldGoldIds()),
                req.advanceAdjusted(), req.schemeAdjusted()));
    }

    @Transactional
    public Invoice checkout(Requests.Checkout req, AppUser cashier) {
        List<Requests.PosItem> lines = new ArrayList<>();
        for (Requests.PosItem line : req.items()) {
            if (line.inventoryItemId() == null) {
                lines.add(line);
                continue;
            }
            InventoryItem stock = inventory.findById(line.inventoryItemId())
                    .orElseThrow(() -> ApiException.notFound("Inventory item " + line.inventoryItemId()));
            if (!"available".equals(stock.getStatus())) {
                throw ApiException.badRequest(stock.getItemCode() + " is not available (status: " + stock.getStatus() + ")");
            }
            lines.add(new Requests.PosItem(stock.getId(), stock.getProduct().getName(),
                    stock.getProduct().getMetalType(), stock.getPurity(), stock.getHuid(), stock.getGrossWeight(),
                    stock.getStoneWeight(), stock.getNetWeight(), line.metalRate(), line.wastagePct(),
                    line.wastageWeight(), line.makingChargeType(), line.makingChargeRate(), line.stoneCharge(),
                    line.diamondCharge(), line.hallmarkCharge(), line.otherCharge(), line.discount(), line.gstRate()));
        }

        List<OldGoldPurchase> exchanged = resolveOldGold(req.oldGoldIds());
        BigDecimal oldGoldValue = exchanged.stream().map(OldGoldPurchase::getValuationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        InvoiceBreakdown calc = pricing.calculateInvoice(toInputs(lines), new JewelleryPricing.InvoiceOptions(
                req.discountAmount(), req.gstType(), req.roundingMode(), oldGoldValue,
                req.advanceAdjusted(), req.schemeAdjusted()));

        String invoiceType = req.invoiceType() == null ? "sale" : req.invoiceType();
        boolean estimate = "estimate".equals(invoiceType);
        BigDecimal paid = req.paidAmount();
        BigDecimal grand = calc.grandTotal();
        BigDecimal balance = JewelleryPricing.r2(grand.subtract(paid)).max(BigDecimal.ZERO);
        String status = estimate ? "estimate"
                : balance.signum() <= 0 ? "paid" : paid.signum() > 0 ? "partial" : "credit";

        Customer customer = req.customerId() == null ? null
                : customers.findById(req.customerId()).orElseThrow(() -> ApiException.notFound("Customer"));

        Invoice inv = new Invoice();
        inv.setInvoiceNo(String.format("INV-%d-%04d", Year.now().getValue(), invoices.maxId() + 1));
        inv.setBranch(branches.findById(req.branchId()).orElseThrow(() -> ApiException.notFound("Branch")));
        inv.setCustomer(customer);
        inv.setSalesperson(req.salespersonId() == null ? cashier
                : users.findById(req.salespersonId()).orElseThrow(() -> ApiException.notFound("Salesperson")));
        inv.setCashier(cashier);
        inv.setInvoiceType(invoiceType);
        inv.setGstType(req.gstType() == null ? "intra" : req.gstType());
        inv.setSubtotal(calc.subtotal());
        inv.setMakingTotal(calc.makingTotal());
        inv.setStoneTotal(calc.stoneTotal());
        inv.setDiscountAmount(calc.discountAmount());
        inv.setCgstAmount(calc.cgstAmount());
        inv.setSgstAmount(calc.sgstAmount());
        inv.setIgstAmount(calc.igstAmount());
        inv.setRoundOff(calc.roundOff());
        inv.setOldGoldDeduction(calc.oldGoldDeduction());
        inv.setAdvanceAdjusted(calc.advanceAdjusted());
        inv.setSchemeAdjusted(calc.schemeAdjusted());
        inv.setGrandTotal(grand);
        inv.setPaidAmount(paid);
        inv.setBalanceDue(balance);
        inv.setStatus(status);
        inv.setNotes(req.notes());
        inv.setBilledAt(LocalDateTime.now());

        for (int i = 0; i < lines.size(); i++) {
            inv.getItems().add(toInvoiceItem(inv, lines.get(i), calc.items().get(i)));
        }
        if (paid.signum() > 0) {
            InvoicePayment payment = new InvoicePayment();
            payment.setInvoice(inv);
            payment.setMethod(req.paymentMethod());
            payment.setAmount(paid);
            payment.setReferenceNo(req.paymentReference());
            payment.setPaidAt(LocalDateTime.now());
            inv.getPayments().add(payment);
        }
        invoices.save(inv);

        if (!estimate) {
            for (Requests.PosItem line : lines) {
                if (line.inventoryItemId() != null) {
                    InventoryItem stock = inventory.findById(line.inventoryItemId()).orElseThrow();
                    stock.setStatus("sold");
                    StockMovement m = new StockMovement();
                    m.setInventoryItem(stock);
                    m.setBranch(inv.getBranch());
                    m.setType("sale");
                    m.setReference(inv.getInvoiceNo());
                    m.setNotes("Sold via POS " + inv.getInvoiceNo());
                    m.setUser(cashier);
                    movements.save(m);
                }
            }
            for (OldGoldPurchase og : exchanged) {
                og.setInvoiceId(inv.getId());
            }
            if (customer != null) {
                if (balance.signum() > 0) {
                    customer.setOutstandingBalance(customer.getOutstandingBalance().add(balance));
                }
                int points = grand.divide(LOYALTY_RUPEES_PER_POINT, 0, RoundingMode.FLOOR).intValue();
                if (points > 0) {
                    customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
                    LoyaltyTransaction t = new LoyaltyTransaction();
                    t.setCustomer(customer);
                    t.setPoints(points);
                    t.setType("earn");
                    t.setReference(inv.getInvoiceNo());
                    t.setDescription("Earned on invoice " + inv.getInvoiceNo());
                    loyalty.save(t);
                }
            }
        }

        audit.log(cashier, "POS", "create_invoice", "Generated invoice " + inv.getInvoiceNo() + " for Rs." + grand);
        return inv;
    }

    @Transactional
    public Invoice cancel(Long id, String reason, AppUser user) {
        Invoice inv = invoices.findById(id).orElseThrow(() -> ApiException.notFound("Invoice"));
        if ("cancelled".equals(inv.getStatus())) {
            throw ApiException.badRequest("Invoice is already cancelled");
        }
        boolean wasEstimate = "estimate".equals(inv.getStatus());
        inv.setStatus("cancelled");
        if (!wasEstimate) {
            for (InvoiceItem item : inv.getItems()) {
                if (item.getInventoryItemId() != null) {
                    inventory.findById(item.getInventoryItemId()).ifPresent(stock -> {
                        stock.setStatus("available");
                        StockMovement m = new StockMovement();
                        m.setInventoryItem(stock);
                        m.setBranch(inv.getBranch());
                        m.setType("return_in");
                        m.setReference(inv.getInvoiceNo());
                        m.setNotes("Restocked after cancelling " + inv.getInvoiceNo());
                        m.setUser(user);
                        movements.save(m);
                    });
                }
            }
            oldGold.findAll().stream().filter(og -> inv.getId().equals(og.getInvoiceId()))
                    .forEach(og -> og.setInvoiceId(null));
            Customer c = inv.getCustomer();
            if (c != null && inv.getBalanceDue().signum() > 0) {
                c.setOutstandingBalance(c.getOutstandingBalance().subtract(inv.getBalanceDue()).max(BigDecimal.ZERO));
            }
        }
        audit.log(user, "POS", "cancel", "Cancelled invoice " + inv.getInvoiceNo()
                + (reason == null || reason.isBlank() ? "" : ": " + reason));
        return inv;
    }

    public List<Invoice> search(String q, String status) {
        return invoices.search(q == null ? "" : q.trim(), status == null ? "" : status.trim());
    }

    public Invoice get(Long id) {
        return invoices.findById(id).orElseThrow(() -> ApiException.notFound("Invoice"));
    }

    private BigDecimal oldGoldTotal(List<Long> ids) {
        return resolveOldGold(ids).stream().map(OldGoldPurchase::getValuationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<OldGoldPurchase> resolveOldGold(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<OldGoldPurchase> found = oldGold.findAllById(ids);
        for (OldGoldPurchase og : found) {
            if (!"approved".equals(og.getStatus()) || og.getInvoiceId() != null) {
                throw ApiException.badRequest("Old gold receipt " + og.getReceiptNo() + " is not available for exchange");
            }
        }
        return found;
    }

    private static List<ItemInput> toInputs(List<Requests.PosItem> items) {
        return items.stream().map(i -> new ItemInput(i.grossWeight(), i.stoneWeight(), i.netWeight(), i.metalRate(),
                i.wastagePct(), i.wastageWeight(), i.makingChargeType(), i.makingChargeRate(), i.stoneCharge(),
                i.diamondCharge(), i.hallmarkCharge(), i.otherCharge(), i.discount(), i.gstRate())).toList();
    }

    private static InvoiceItem toInvoiceItem(Invoice inv, Requests.PosItem raw, ItemBreakdown c) {
        InvoiceItem it = new InvoiceItem();
        it.setInvoice(inv);
        it.setInventoryItemId(raw.inventoryItemId());
        it.setProductName(raw.productName() == null || raw.productName().isBlank() ? "Jewellery Item" : raw.productName());
        it.setMetalType(raw.metalType() == null ? "Gold" : raw.metalType());
        it.setPurity(raw.purity() == null ? "22K" : raw.purity());
        it.setHuid(raw.huid());
        it.setGrossWeight(c.grossWeight());
        it.setStoneWeight(c.stoneWeight());
        it.setNetWeight(c.netWeight());
        it.setMetalRate(c.metalRate());
        it.setMetalValue(c.metalValue());
        it.setWastagePct(c.wastagePct());
        it.setWastageWeight(c.wastageWeight());
        it.setWastageValue(c.wastageValue());
        it.setMakingChargeType(c.makingChargeType());
        it.setMakingChargeRate(c.makingChargeRate());
        it.setMakingChargeAmount(c.makingChargeAmount());
        it.setStoneCharge(c.stoneCharge());
        it.setDiamondCharge(c.diamondCharge());
        it.setHallmarkCharge(c.hallmarkCharge());
        it.setOtherCharge(c.otherCharge());
        it.setDiscount(c.discount());
        it.setTaxableValue(c.taxableValue());
        it.setGstRate(c.gstRate());
        it.setGstAmount(c.gstAmount());
        it.setLineTotal(c.lineTotal());
        return it;
    }
}
