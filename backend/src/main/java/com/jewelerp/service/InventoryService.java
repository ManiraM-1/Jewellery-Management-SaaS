package com.jewelerp.service;

import com.jewelerp.config.ApiException;
import com.jewelerp.dto.Requests;
import com.jewelerp.model.AppUser;
import com.jewelerp.model.Category;
import com.jewelerp.model.Counter;
import com.jewelerp.model.InventoryItem;
import com.jewelerp.model.Product;
import com.jewelerp.model.StockMovement;
import com.jewelerp.repository.BranchRepository;
import com.jewelerp.repository.CategoryRepository;
import com.jewelerp.repository.CounterRepository;
import com.jewelerp.repository.InventoryItemRepository;
import com.jewelerp.repository.ProductRepository;
import com.jewelerp.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final InventoryItemRepository items;
    private final BranchRepository branches;
    private final CounterRepository counters;
    private final StockMovementRepository movements;
    private final AuditService audit;

    public record ProductRow(Product product, long availableCount) {
    }

    public List<ProductRow> listProducts(String q) {
        return products.search(q == null ? "" : q.trim()).stream()
                .map(p -> new ProductRow(p, items.countByProductIdAndStatus(p.getId(), "available")))
                .toList();
    }

    @Transactional
    public Product createProduct(Requests.CreateProduct req, AppUser user) {
        if (products.existsBySkuIgnoreCase(req.sku())) {
            throw new ApiException(HttpStatus.CONFLICT, "SKU " + req.sku() + " already exists");
        }
        Category category = categories.findById(req.categoryId()).orElseThrow(() -> ApiException.notFound("Category"));
        Product p = new Product();
        p.setSku(req.sku().trim().toUpperCase());
        p.setName(req.name().trim());
        p.setCategory(category);
        p.setProductType(req.productType());
        p.setMetalType(req.metalType());
        p.setPurity(req.purity());
        p.setMakingChargeType(req.makingChargeType());
        p.setMakingChargeValue(req.makingChargeValue());
        p.setWastagePct(orZero(req.wastagePct()));
        p.setGstRate(req.gstRate() == null ? new BigDecimal("3.00") : req.gstRate());
        p.setDescription(req.description());
        products.save(p);
        audit.log(user, "Products", "create", "Created product " + p.getSku() + " - " + p.getName());
        return p;
    }

    @Transactional
    public void deleteProduct(Long id, AppUser user) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
        products.delete(p);
        products.flush();
        audit.log(user, "Products", "delete", "Deleted product " + p.getSku());
    }

    public List<InventoryItem> listItems(Long branchId, String status, String metal, String q) {
        return items.search(branchId, blank(status), blank(metal), blank(q));
    }

    @Transactional
    public InventoryItem createItem(Requests.CreateItem req, AppUser user) {
        String huid = req.huid() == null || req.huid().isBlank() ? null : req.huid().trim().toUpperCase();
        if (huid != null && items.existsByHuidIgnoreCase(huid)) {
            throw new ApiException(HttpStatus.CONFLICT, "HUID " + huid + " is already assigned to another piece");
        }
        Product product = products.findById(req.productId()).orElseThrow(() -> ApiException.notFound("Product"));
        Counter counter = req.counterId() == null ? null
                : counters.findById(req.counterId()).orElseThrow(() -> ApiException.notFound("Counter"));

        BigDecimal gross = req.grossWeight();
        BigDecimal stone = orZero(req.stoneWeight());
        BigDecimal net = gross.subtract(stone).max(BigDecimal.ZERO);

        long next = items.maxId() + 1;
        InventoryItem item = new InventoryItem();
        item.setItemCode(String.format("JW-%05d", next));
        item.setBarcode(String.format("8901000%05d", next));
        item.setProduct(product);
        item.setBranch(branches.findById(req.branchId()).orElseThrow(() -> ApiException.notFound("Branch")));
        item.setCounter(counter);
        item.setGrossWeight(gross);
        item.setStoneWeight(stone);
        item.setDiamondWeight(orZero(req.diamondWeight()));
        item.setNetWeight(net);
        item.setPurity(req.purity());
        item.setHuid(huid);
        item.setHallmarkCenter(req.hallmarkCenter());
        item.setHallmarkCharge(req.hallmarkCharge() == null ? new BigDecimal("45") : req.hallmarkCharge());
        item.setPurchaseCost(orZero(req.purchaseCost()));
        item.setStoneCharge(orZero(req.stoneCharge()));
        item.setDiamondCharge(orZero(req.diamondCharge()));
        item.setNotes(req.notes());
        items.save(item);

        StockMovement m = new StockMovement();
        m.setInventoryItem(item);
        m.setBranch(item.getBranch());
        m.setType("stock_in");
        m.setReference(item.getItemCode());
        m.setNotes("Added to stock");
        m.setUser(user);
        movements.save(m);

        audit.log(user, "Inventory", "create", "Added " + item.getItemCode() + " (" + product.getName() + ", "
                + gross + "g)");
        return item;
    }

    @Transactional
    public void deleteItem(Long id, AppUser user) {
        InventoryItem item = items.findById(id).orElseThrow(() -> ApiException.notFound("Item"));
        if ("sold".equals(item.getStatus())) {
            throw ApiException.badRequest("A sold piece cannot be deleted; cancel its invoice first");
        }
        movements.deleteAll(movements.findByInventoryItemIdOrderByIdDesc(id));
        items.delete(item);
        audit.log(user, "Inventory", "delete", "Deleted item " + item.getItemCode());
    }

    public List<StockMovement> history(Long itemId) {
        return movements.findByInventoryItemIdOrderByIdDesc(itemId);
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String blank(String v) {
        return v == null ? "" : v.trim();
    }
}
