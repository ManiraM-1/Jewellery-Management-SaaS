package com.jewelerp.web;

import com.jewelerp.dto.Requests;
import com.jewelerp.model.OldGoldPurchase;
import com.jewelerp.pricing.JewelleryPricing;
import com.jewelerp.service.CurrentUser;
import com.jewelerp.service.OldGoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/old-gold")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('admin','manager','cashier')")
public class OldGoldController {

    private final OldGoldService oldGold;
    private final CurrentUser currentUser;

    @GetMapping
    public List<OldGoldPurchase> list() {
        return oldGold.list();
    }

    @PostMapping("/calculate")
    public JewelleryPricing.OldGoldValuation calculate(@Valid @RequestBody Requests.OldGold req) {
        return oldGold.preview(req);
    }

    @PostMapping
    public OldGoldPurchase create(@Valid @RequestBody Requests.OldGold req) {
        return oldGold.create(req, currentUser.get());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public OldGoldPurchase approve(@PathVariable Long id) {
        return oldGold.approve(id, currentUser.get());
    }

    @PostMapping("/{id}/refinery")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public OldGoldPurchase refinery(@PathVariable Long id, @Valid @RequestBody Requests.Refinery req) {
        return oldGold.sendToRefinery(id, req.refineryName(), currentUser.get());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public void delete(@PathVariable Long id) {
        oldGold.delete(id, currentUser.get());
    }
}
