package com.jewelerp.web;

import com.jewelerp.dto.Requests;
import com.jewelerp.model.Customer;
import com.jewelerp.service.CurrentUser;
import com.jewelerp.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('admin','manager','cashier','salesperson')")
public class CustomerController {

    private final CustomerService customers;
    private final CurrentUser currentUser;

    @GetMapping
    public List<CustomerService.CustomerRow> list(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) String tier) {
        return customers.list(q, tier);
    }

    @GetMapping("/events")
    public List<CustomerService.UpcomingEvent> events(@RequestParam(defaultValue = "30") int days) {
        return customers.upcomingEvents(days);
    }

    @GetMapping("/{id}")
    public CustomerService.Detail detail(@PathVariable Long id) {
        return customers.detail(id);
    }

    @PostMapping
    public Customer create(@Valid @RequestBody Requests.CreateCustomer req) {
        return customers.create(req, currentUser.get());
    }

    @PostMapping("/{id}/loyalty")
    @PreAuthorize("hasAnyRole('admin','manager','cashier')")
    public Customer loyalty(@PathVariable Long id, @Valid @RequestBody Requests.Loyalty req) {
        return customers.adjustLoyalty(id, req, currentUser.get());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public void delete(@PathVariable Long id) {
        customers.delete(id, currentUser.get());
    }
}
