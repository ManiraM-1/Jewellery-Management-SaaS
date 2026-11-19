package com.jewelerp.service;

import com.jewelerp.model.InventoryItem;
import com.jewelerp.model.Invoice;
import com.jewelerp.model.InvoiceItem;
import com.jewelerp.model.MetalRate;
import com.jewelerp.repository.InventoryItemRepository;
import com.jewelerp.repository.InvoiceRepository;
import com.jewelerp.repository.MetalRateRepository;
import com.jewelerp.repository.OldGoldPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository invoices;
    private final InventoryItemRepository inventory;
    private final MetalRateRepository rateHistory;
    private final MetalRateService rates;
    private final OldGoldPurchaseRepository oldGold;
    private final CustomerService customers;

    public record DayValue(LocalDate date, BigDecimal value) {
    }

    public record StaffSales(String name, BigDecimal total, int invoices) {
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(Long branchId) {
        LocalDate today = LocalDate.now();
        List<Invoice> billed = invoices.findByStatusNot("cancelled").stream()
                .filter(i -> !"estimate".equals(i.getStatus()))
                .filter(i -> branchId == null || Objects.equals(i.getBranch().getId(), branchId))
                .toList();

        BigDecimal todaySales = sum(billed.stream().filter(i -> i.getBilledAt().toLocalDate().equals(today))
                .map(Invoice::getGrandTotal).toList());
        BigDecimal totalSales = sum(billed.stream().map(Invoice::getGrandTotal).toList());
        BigDecimal outstanding = sum(billed.stream().map(Invoice::getBalanceDue).toList());

        Map<String, BigDecimal> metalSales = new TreeMap<>();
        Map<String, BigDecimal> metalWeightSold = new TreeMap<>();
        BigDecimal todayGrossWeight = BigDecimal.ZERO;
        for (Invoice inv : billed) {
            for (InvoiceItem item : inv.getItems()) {
                metalSales.merge(item.getMetalType(), item.getLineTotal(), BigDecimal::add);
                metalWeightSold.merge(item.getMetalType(), item.getNetWeight(), BigDecimal::add);
                if (inv.getBilledAt().toLocalDate().equals(today)) {
                    todayGrossWeight = todayGrossWeight.add(item.getGrossWeight());
                }
            }
        }

        BigDecimal goldWeight = BigDecimal.ZERO;
        BigDecimal silverWeight = BigDecimal.ZERO;
        BigDecimal stockValue = BigDecimal.ZERO;
        int pieces = 0;
        for (InventoryItem item : inventory.findByStatus("available")) {
            if (branchId != null && !Objects.equals(item.getBranch().getId(), branchId)) {
                continue;
            }
            pieces++;
            String metal = item.getProduct().getMetalType();
            if ("Gold".equals(metal)) {
                goldWeight = goldWeight.add(item.getNetWeight());
            } else if ("Silver".equals(metal)) {
                silverWeight = silverWeight.add(item.getNetWeight());
            }
            BigDecimal rate = rates.latestRate(metal, item.getPurity(), item.getBranch().getId());
            stockValue = stockValue.add(item.getNetWeight().multiply(rate))
                    .add(item.getStoneCharge()).add(item.getDiamondCharge());
        }

        List<DayValue> last7 = new ArrayList<>();
        for (int d = 6; d >= 0; d--) {
            LocalDate day = today.minusDays(d);
            last7.add(new DayValue(day, sum(billed.stream().filter(i -> i.getBilledAt().toLocalDate().equals(day))
                    .map(Invoice::getGrandTotal).toList())));
        }

        Map<LocalDate, BigDecimal> goldTrend = new TreeMap<>();
        for (MetalRate r : rateHistory.gold24History(today.minusDays(13).atStartOfDay())) {
            if (r.getBranch() == null) {
                goldTrend.put(r.getEffectiveAt().toLocalDate(), r.getRatePerGram());
            }
        }

        Map<String, StaffSales> staff = new LinkedHashMap<>();
        for (Invoice inv : billed) {
            if (inv.getSalesperson() == null) {
                continue;
            }
            staff.merge(inv.getSalesperson().getName(),
                    new StaffSales(inv.getSalesperson().getName(), inv.getGrandTotal(), 1),
                    (a, b) -> new StaffSales(a.name(), a.total().add(b.total()), a.invoices() + 1));
        }
        List<StaffSales> leaderboard = staff.values().stream()
                .sorted((a, b) -> b.total().compareTo(a.total())).toList();

        long pendingOldGold = oldGold.findAll().stream().filter(o -> "pending_approval".equals(o.getStatus())).count();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("todaySales", todaySales);
        out.put("todayGrossWeightSold", todayGrossWeight);
        out.put("totalSales", totalSales);
        out.put("customerOutstanding", outstanding);
        out.put("metalSales", metalSales);
        out.put("metalWeightSold", metalWeightSold);
        out.put("stockPieces", pieces);
        out.put("goldWeightInStock", goldWeight);
        out.put("silverWeightInStock", silverWeight);
        out.put("stockValuation", stockValue.setScale(2, java.math.RoundingMode.HALF_UP));
        out.put("pendingOldGoldApprovals", pendingOldGold);
        out.put("rates", rates.currentBoard());
        out.put("salesLast7Days", last7);
        out.put("gold24Trend", goldTrend.entrySet().stream().map(e -> new DayValue(e.getKey(), e.getValue())).toList());
        out.put("staffLeaderboard", leaderboard);
        out.put("recentInvoices", invoices.findTop10ByOrderByBilledAtDesc());
        out.put("upcomingEvents", customers.upcomingEvents(7));
        return out;
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
