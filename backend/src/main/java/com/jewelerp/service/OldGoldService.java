package com.jewelerp.service;

import com.jewelerp.config.ApiException;
import com.jewelerp.dto.Requests;
import com.jewelerp.model.AppUser;
import com.jewelerp.model.OldGoldPurchase;
import com.jewelerp.model.Role;
import com.jewelerp.pricing.JewelleryPricing;
import com.jewelerp.pricing.JewelleryPricing.OldGoldValuation;
import com.jewelerp.repository.BranchRepository;
import com.jewelerp.repository.CustomerRepository;
import com.jewelerp.repository.OldGoldPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OldGoldService {

    private final JewelleryPricing pricing;
    private final OldGoldPurchaseRepository purchases;
    private final BranchRepository branches;
    private final CustomerRepository customers;
    private final AuditService audit;

    public List<OldGoldPurchase> list() {
        return purchases.findAllByOrderByIdDesc();
    }

    public OldGoldValuation preview(Requests.OldGold req) {
        return value(req);
    }

    @Transactional
    public OldGoldPurchase create(Requests.OldGold req, AppUser user) {
        if (!req.customerDeclaration()) {
            throw ApiException.badRequest("Customer declaration must be accepted before purchasing old gold");
        }
        OldGoldValuation v = value(req);
        boolean managerial = Role.isManagerial(user.getRole());

        OldGoldPurchase p = new OldGoldPurchase();
        p.setReceiptNo("OGP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        p.setBranch(branches.findById(req.branchId()).orElseThrow(() -> ApiException.notFound("Branch")));
        p.setCustomer(req.customerId() == null ? null
                : customers.findById(req.customerId()).orElseThrow(() -> ApiException.notFound("Customer")));
        p.setItemDescription(req.itemDescription());
        p.setGrossWeight(v.grossWeight());
        p.setStoneWeight(v.stoneWeight());
        p.setNetWeight(v.netWeight());
        p.setTestedPurity(req.testedPurity());
        p.setPurityPct(v.purityPct());
        p.setFineGoldWeight(v.fineGoldWeight());
        p.setAppliedRate(v.appliedRate());
        p.setDeductionPct(v.deductionPct());
        p.setMeltingLossPct(v.meltingLossPct());
        p.setEffectiveWeight(v.effectiveWeight());
        p.setValuationAmount(v.valuationAmount());
        p.setSettlementType(req.settlementType());
        p.setCustomerDeclaration(true);
        p.setStatus(managerial ? "approved" : "pending_approval");
        p.setApprovedBy(managerial ? user : null);
        purchases.save(p);
        audit.log(user, "OldGold", "create", "Registered old gold " + p.getReceiptNo() + " (Rs." + p.getValuationAmount() + ")");
        return p;
    }

    @Transactional
    public OldGoldPurchase approve(Long id, AppUser user) {
        OldGoldPurchase p = find(id);
        if (!"pending_approval".equals(p.getStatus())) {
            throw ApiException.badRequest("Only pending receipts can be approved");
        }
        p.setStatus("approved");
        p.setApprovedBy(user);
        audit.log(user, "OldGold", "approve", "Approved " + p.getReceiptNo());
        return p;
    }

    @Transactional
    public OldGoldPurchase sendToRefinery(Long id, String refinery, AppUser user) {
        OldGoldPurchase p = find(id);
        p.setStatus("refinery_transferred");
        p.setRefineryName(refinery);
        p.setRefineryTransferDate(LocalDate.now());
        audit.log(user, "OldGold", "refinery", p.getReceiptNo() + " sent to " + refinery);
        return p;
    }

    @Transactional
    public void delete(Long id, AppUser user) {
        OldGoldPurchase p = find(id);
        if (p.getInvoiceId() != null) {
            throw ApiException.badRequest("This receipt is already adjusted in an invoice");
        }
        purchases.delete(p);
        audit.log(user, "OldGold", "delete", "Deleted old gold " + p.getReceiptNo());
    }

    private OldGoldPurchase find(Long id) {
        return purchases.findById(id).orElseThrow(() -> ApiException.notFound("Old gold receipt"));
    }

    private OldGoldValuation value(Requests.OldGold req) {
        return pricing.calculateOldGold(new JewelleryPricing.OldGoldInput(req.grossWeight(), req.stoneWeight(),
                req.purityPct(), req.deductionPct(), req.meltingLossPct(), req.appliedRate()));
    }
}
