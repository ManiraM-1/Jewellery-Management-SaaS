package com.jewelerp.web;

import com.jewelerp.dto.Requests;
import com.jewelerp.model.Invoice;
import com.jewelerp.model.OldGoldPurchase;
import com.jewelerp.pricing.JewelleryPricing;
import com.jewelerp.service.CurrentUser;
import com.jewelerp.service.PosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class PosController {

    private final PosService pos;
    private final CurrentUser currentUser;

    @GetMapping("/pos/catalog")
    @PreAuthorize("hasAnyRole('admin','manager','cashier','salesperson')")
    public List<PosService.CatalogItem> catalog() {
        return pos.availableItems();
    }

    @GetMapping("/pos/old-gold")
    @PreAuthorize("hasAnyRole('admin','manager','cashier','salesperson')")
    public List<OldGoldPurchase> oldGold() {
        return pos.unusedOldGold();
    }

    @PostMapping("/pos/calculate")
    @PreAuthorize("hasAnyRole('admin','manager','cashier','salesperson')")
    public JewelleryPricing.InvoiceBreakdown calculate(@Valid @RequestBody Requests.PosPreview req) {
        return pos.preview(req);
    }

    @PostMapping("/pos/checkout")
    @PreAuthorize("hasAnyRole('admin','manager','cashier','salesperson')")
    public Invoice checkout(@Valid @RequestBody Requests.Checkout req) {
        return pos.checkout(req, currentUser.get());
    }

    @GetMapping("/invoices")
    public List<Invoice> invoices(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) String status) {
        return pos.search(q, status);
    }

    @GetMapping("/invoices/{id}")
    public Invoice invoice(@PathVariable Long id) {
        return pos.get(id);
    }

    @PostMapping("/invoices/{id}/cancel")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public Invoice cancel(@PathVariable Long id, @RequestBody(required = false) Requests.Cancel req) {
        return pos.cancel(id, req == null ? null : req.reason(), currentUser.get());
    }
}
