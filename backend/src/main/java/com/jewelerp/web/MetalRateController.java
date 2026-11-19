package com.jewelerp.web;

import com.jewelerp.dto.Requests;
import com.jewelerp.model.MetalRate;
import com.jewelerp.service.CurrentUser;
import com.jewelerp.service.MetalRateService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
public class MetalRateController {

    private final MetalRateService rates;
    private final CurrentUser currentUser;

    @GetMapping
    public Map<String, Object> index() {
        return Map.of("current", rates.currentBoard(), "history", rates.history());
    }

    @PostMapping
    public List<MetalRate> create(@Valid @RequestBody Requests.CreateRate req) {
        return rates.create(req, currentUser.get());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public MetalRate approve(@PathVariable Long id) {
        return rates.approve(id, currentUser.get());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public void delete(@PathVariable Long id) {
        rates.delete(id, currentUser.get());
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public Map<String, Object> sync(@RequestBody(required = false) Requests.SyncRates req) {
        return rates.syncLive(req == null ? null : req.branchId(), currentUser.get());
    }
}
