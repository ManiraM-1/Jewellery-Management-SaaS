package com.jewelerp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class Requests {

    private Requests() {
    }

    public record Login(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record CreateRate(
            Long branchId,
            @NotBlank String metalType,
            @NotBlank String purity,
            @NotNull @DecimalMin("1") BigDecimal ratePerGram,
            LocalDateTime effectiveAt,
            boolean autoCalculatePurities,
            String notes
    ) {
    }

    public record SyncRates(Long branchId) {
    }

    public record CreateProduct(
            @NotBlank String sku,
            @NotBlank String name,
            @NotNull Long categoryId,
            @NotBlank String productType,
            @NotBlank String metalType,
            @NotBlank String purity,
            @NotBlank @Pattern(regexp = "per_gram|percentage|fixed") String makingChargeType,
            @NotNull @DecimalMin("0") BigDecimal makingChargeValue,
            @DecimalMin("0") BigDecimal wastagePct,
            @DecimalMin("0") BigDecimal gstRate,
            String description
    ) {
    }

    public record CreateItem(
            @NotNull Long productId,
            @NotNull Long branchId,
            Long counterId,
            @NotNull @DecimalMin("0.001") BigDecimal grossWeight,
            @DecimalMin("0") BigDecimal stoneWeight,
            @DecimalMin("0") BigDecimal diamondWeight,
            @NotBlank String purity,
            @Pattern(regexp = "^$|^[A-Za-z0-9]{6}$", message = "must be 6 letters/digits") String huid,
            String hallmarkCenter,
            @DecimalMin("0") BigDecimal hallmarkCharge,
            @DecimalMin("0") BigDecimal purchaseCost,
            @DecimalMin("0") BigDecimal stoneCharge,
            @DecimalMin("0") BigDecimal diamondCharge,
            String notes
    ) {
    }

    public record PosItem(
            Long inventoryItemId,
            String productName,
            String metalType,
            String purity,
            String huid,
            BigDecimal grossWeight,
            BigDecimal stoneWeight,
            BigDecimal netWeight,
            BigDecimal metalRate,
            BigDecimal wastagePct,
            BigDecimal wastageWeight,
            String makingChargeType,
            BigDecimal makingChargeRate,
            BigDecimal stoneCharge,
            BigDecimal diamondCharge,
            BigDecimal hallmarkCharge,
            BigDecimal otherCharge,
            BigDecimal discount,
            BigDecimal gstRate
    ) {
    }

    public record PosPreview(
            @NotEmpty List<@Valid PosItem> items,
            BigDecimal discountAmount,
            @Pattern(regexp = "intra|inter") String gstType,
            String roundingMode,
            List<Long> oldGoldIds,
            BigDecimal advanceAdjusted,
            BigDecimal schemeAdjusted
    ) {
    }

    public record Checkout(
            @NotNull Long branchId,
            Long customerId,
            Long salespersonId,
            @Pattern(regexp = "sale|credit|estimate") String invoiceType,
            @NotEmpty List<@Valid PosItem> items,
            BigDecimal discountAmount,
            @Pattern(regexp = "intra|inter") String gstType,
            String roundingMode,
            List<Long> oldGoldIds,
            BigDecimal advanceAdjusted,
            BigDecimal schemeAdjusted,
            @NotBlank String paymentMethod,
            String paymentReference,
            @NotNull @DecimalMin("0") BigDecimal paidAmount,
            String notes
    ) {
    }

    public record Cancel(String reason) {
    }

    public record CreateCustomer(
            @NotBlank String name,
            @NotBlank String phone,
            @Email String email,
            String address,
            LocalDate dob,
            LocalDate anniversary,
            String panNumber,
            @NotBlank @Pattern(regexp = "standard|silver|gold|vip") String groupTier,
            @DecimalMin("0") BigDecimal creditLimit,
            String preferences
    ) {
    }

    public record Loyalty(
            @NotBlank @Pattern(regexp = "earn|redeem|adjust") String type,
            @Min(1) int points,
            @NotBlank String description
    ) {
    }

    public record OldGold(
            @NotNull Long branchId,
            Long customerId,
            @NotBlank String itemDescription,
            @NotNull @DecimalMin("0.01") BigDecimal grossWeight,
            @DecimalMin("0") BigDecimal stoneWeight,
            @NotBlank String testedPurity,
            @NotNull @DecimalMin("1") @DecimalMax("100") BigDecimal purityPct,
            @NotNull @DecimalMin("1") BigDecimal appliedRate,
            @DecimalMin("0") @DecimalMax("50") BigDecimal deductionPct,
            @DecimalMin("0") @DecimalMax("25") BigDecimal meltingLossPct,
            @NotBlank @Pattern(regexp = "exchange|cash|bank") String settlementType,
            boolean customerDeclaration
    ) {
    }

    public record Refinery(@NotBlank String refineryName) {
    }
}
