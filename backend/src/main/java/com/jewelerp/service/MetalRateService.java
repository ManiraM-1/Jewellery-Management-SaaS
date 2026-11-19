package com.jewelerp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jewelerp.config.ApiException;
import com.jewelerp.dto.Requests;
import com.jewelerp.model.AppUser;
import com.jewelerp.model.Branch;
import com.jewelerp.model.MetalRate;
import com.jewelerp.model.Role;
import com.jewelerp.pricing.JewelleryPricing;
import com.jewelerp.repository.BranchRepository;
import com.jewelerp.repository.MetalRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MetalRateService {

    public static final List<String[]> BOARD = List.of(
            new String[]{"Gold", "24K"}, new String[]{"Gold", "22K"}, new String[]{"Gold", "20K"},
            new String[]{"Gold", "18K"}, new String[]{"Gold", "14K"}, new String[]{"Silver", "999"},
            new String[]{"Silver", "925"}, new String[]{"Platinum", "950"});

    private static final BigDecimal GRAMS_PER_TROY_OUNCE = new BigDecimal("31.1034768");
    private static final BigDecimal GOLD_IMPORT_DUTY_FACTOR = new BigDecimal("1.168");
    private static final BigDecimal SILVER_LANDED_FACTOR = new BigDecimal("1.32");

    private final MetalRateRepository rates;
    private final BranchRepository branches;
    private final AuditService audit;

    public BigDecimal latestRate(String metal, String purity, Long branchId) {
        List<MetalRate> approved = rates.findApproved(metal, purity, LocalDateTime.now());
        if (branchId != null) {
            for (MetalRate r : approved) {
                if (r.getBranch() != null && Objects.equals(r.getBranch().getId(), branchId)) {
                    return r.getRatePerGram();
                }
            }
        }
        for (MetalRate r : approved) {
            if (r.getBranch() == null) {
                return r.getRatePerGram();
            }
        }
        return approved.isEmpty() ? BigDecimal.ZERO : approved.get(0).getRatePerGram();
    }

    public Map<String, BigDecimal> currentBoard() {
        Map<String, BigDecimal> board = new LinkedHashMap<>();
        for (String[] mp : BOARD) {
            board.put(mp[0] + " " + mp[1], latestRate(mp[0], mp[1], null));
        }
        return board;
    }

    public List<MetalRate> history() {
        return rates.findTop100ByOrderByEffectiveAtDescIdDesc();
    }

    @Transactional
    public List<MetalRate> create(Requests.CreateRate req, AppUser user) {
        boolean managerial = Role.isManagerial(user.getRole());
        String status = managerial ? "approved" : "pending";
        LocalDateTime effectiveAt = req.effectiveAt() == null ? LocalDateTime.now() : req.effectiveAt();
        Branch branch = req.branchId() == null ? null
                : branches.findById(req.branchId()).orElseThrow(() -> ApiException.notFound("Branch"));

        List<MetalRate> created = new java.util.ArrayList<>();
        created.add(save(branch, req.metalType(), req.purity(), req.ratePerGram(), effectiveAt, status, user,
                managerial, req.notes()));

        if ("Gold".equals(req.metalType()) && "24K".equals(req.purity()) && req.autoCalculatePurities()) {
            for (String purity : List.of("22K", "20K", "18K", "14K", "9K")) {
                created.add(save(branch, "Gold", purity, JewelleryPricing.deriveRateFrom24K(req.ratePerGram(), purity),
                        effectiveAt, status, user, managerial, "Auto-calculated from 24K rate"));
            }
        }
        audit.log(user, "MetalRates", "create", "Set " + req.metalType() + " " + req.purity() + " rate to Rs."
                + req.ratePerGram() + "/g" + (managerial ? "" : " (pending approval)"));
        return created;
    }

    @Transactional
    public MetalRate approve(Long id, AppUser user) {
        MetalRate rate = rates.findById(id).orElseThrow(() -> ApiException.notFound("Rate"));
        rate.setStatus("approved");
        rate.setApprovedBy(user);
        audit.log(user, "MetalRates", "approve", "Approved rate #" + id);
        return rate;
    }

    @Transactional
    public void delete(Long id, AppUser user) {
        MetalRate rate = rates.findById(id).orElseThrow(() -> ApiException.notFound("Rate"));
        rates.delete(rate);
        audit.log(user, "MetalRates", "delete", "Deleted rate " + rate.getMetalType() + " " + rate.getPurity()
                + " @ Rs." + rate.getRatePerGram());
    }

    @Transactional
    public Map<String, Object> syncLive(Long branchId, AppUser user) {
        Branch branch = branchId == null ? null
                : branches.findById(branchId).orElseThrow(() -> ApiException.notFound("Branch"));

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestClient client = RestClient.builder().requestFactory(factory).build();

        BigDecimal goldUsdPerOz;
        BigDecimal silverUsdPerOz;
        BigDecimal inrPerUsd;
        try {
            goldUsdPerOz = client.get().uri("https://api.gold-api.com/price/XAU").retrieve()
                    .body(JsonNode.class).get("price").decimalValue();
            silverUsdPerOz = client.get().uri("https://api.gold-api.com/price/XAG").retrieve()
                    .body(JsonNode.class).get("price").decimalValue();
            inrPerUsd = client.get().uri("https://open.er-api.com/v6/latest/USD").retrieve()
                    .body(JsonNode.class).get("rates").get("INR").decimalValue();
        } catch (Exception e) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Could not reach the live rate service. Enter today's rate manually.");
        }

        BigDecimal gold24 = JewelleryPricing.r2(goldUsdPerOz.multiply(inrPerUsd)
                .divide(GRAMS_PER_TROY_OUNCE, 10, RoundingMode.HALF_UP).multiply(GOLD_IMPORT_DUTY_FACTOR));
        BigDecimal silver999 = JewelleryPricing.r2(silverUsdPerOz.multiply(inrPerUsd)
                .divide(GRAMS_PER_TROY_OUNCE, 10, RoundingMode.HALF_UP).multiply(SILVER_LANDED_FACTOR));

        LocalDateTime now = LocalDateTime.now();
        String note = "Live: international spot + Indian import duty";
        save(branch, "Gold", "24K", gold24, now, "approved", user, true, note);
        for (String purity : List.of("22K", "20K", "18K", "14K", "9K")) {
            save(branch, "Gold", purity, JewelleryPricing.deriveRateFrom24K(gold24, purity), now, "approved", user,
                    true, "Derived from live 24K rate");
        }
        save(branch, "Silver", "999", silver999, now, "approved", user, true, note);

        audit.log(user, "MetalRates", "sync", "Live rates synced: 24K Rs." + gold24 + "/g, Silver Rs." + silver999 + "/g");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gold24k", gold24);
        result.put("gold22k", JewelleryPricing.deriveRateFrom24K(gold24, "22K"));
        result.put("silver999", silver999);
        return result;
    }

    private MetalRate save(Branch branch, String metal, String purity, BigDecimal value, LocalDateTime effectiveAt,
                           String status, AppUser user, boolean approved, String notes) {
        MetalRate rate = new MetalRate();
        rate.setBranch(branch);
        rate.setMetalType(metal);
        rate.setPurity(purity);
        rate.setRatePerGram(value);
        rate.setEffectiveAt(effectiveAt);
        rate.setStatus(status);
        rate.setCreatedBy(user);
        rate.setApprovedBy(approved ? user : null);
        rate.setNotes(notes);
        return rates.save(rate);
    }
}
