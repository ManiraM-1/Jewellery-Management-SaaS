package com.jewelerp.web;

import com.jewelerp.dto.Requests;
import com.jewelerp.model.InventoryItem;
import com.jewelerp.model.Product;
import com.jewelerp.model.StockMovement;
import com.jewelerp.service.CurrentUser;
import com.jewelerp.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventory;
    private final CurrentUser currentUser;

    @GetMapping("/products")
    public List<InventoryService.ProductRow> products(@RequestParam(required = false) String q) {
        return inventory.listProducts(q);
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public Product createProduct(@Valid @RequestBody Requests.CreateProduct req) {
        return inventory.createProduct(req, currentUser.get());
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public void deleteProduct(@PathVariable Long id) {
        inventory.deleteProduct(id, currentUser.get());
    }

    @GetMapping("/inventory")
    public List<InventoryItem> items(@RequestParam(required = false) Long branchId,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String metal,
                                     @RequestParam(required = false) String q) {
        return inventory.listItems(branchId, status, metal, q);
    }

    @PostMapping("/inventory")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public InventoryItem createItem(@Valid @RequestBody Requests.CreateItem req) {
        return inventory.createItem(req, currentUser.get());
    }

    @DeleteMapping("/inventory/{id}")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public void deleteItem(@PathVariable Long id) {
        inventory.deleteItem(id, currentUser.get());
    }

    @GetMapping("/inventory/{id}/movements")
    public List<StockMovement> movements(@PathVariable Long id) {
        return inventory.history(id);
    }
}
