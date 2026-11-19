package com.jewelerp.web;

import com.jewelerp.model.ActivityLog;
import com.jewelerp.model.Role;
import com.jewelerp.repository.ActivityLogRepository;
import com.jewelerp.repository.BranchRepository;
import com.jewelerp.repository.CategoryRepository;
import com.jewelerp.repository.CounterRepository;
import com.jewelerp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LookupController {

    private final BranchRepository branches;
    private final CounterRepository counters;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final ActivityLogRepository logs;

    @GetMapping("/lookups")
    public Map<String, Object> lookups() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("branches", branches.findByActiveTrueOrderByName());
        out.put("counters", counters.findByActiveTrue());
        out.put("categories", categories.findAll());
        out.put("salespersons", users.findByRoleInAndActiveTrueOrderByName(
                List.of(Role.SALESPERSON, Role.MANAGER, Role.ADMIN)));
        out.put("metalTypes", List.of("Gold", "Silver", "Platinum"));
        out.put("purities", Map.of(
                "Gold", List.of("24K", "22K", "20K", "18K", "14K", "9K"),
                "Silver", List.of("999", "925"),
                "Platinum", List.of("950")));
        out.put("productTypes", List.of("Ring", "Necklace", "Chain", "Bracelet", "Earrings", "Bangles",
                "Pendant", "Coins", "Anklet", "Nose Pin", "Mangalsutra"));
        out.put("paymentMethods", List.of("cash", "upi", "card", "bank_transfer"));
        return out;
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('admin')")
    public List<ActivityLog> auditLogs() {
        return logs.findTop50ByOrderByIdDesc();
    }
}
