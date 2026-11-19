package com.jewelerp.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class JewelleryPricing {

    public static final Map<String, BigDecimal> PURITY_FACTORS = Map.of(
            "24K", new BigDecimal("1.0000"),
            "22K", new BigDecimal("0.9167"),
            "20K", new BigDecimal("0.8333"),
            "18K", new BigDecimal("0.7500"),
            "14K", new BigDecimal("0.5833"),
            "9K", new BigDecimal("0.3750"),
            "999", new BigDecimal("0.9990"),
            "925", new BigDecimal("0.9250"),
            "950", new BigDecimal("0.9500")
    );

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public record ItemInput(
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

    public record ItemBreakdown(
            BigDecimal grossWeight,
            BigDecimal stoneWeight,
            BigDecimal netWeight,
            BigDecimal metalRate,
            BigDecimal metalValue,
            BigDecimal wastagePct,
            BigDecimal wastageWeight,
            BigDecimal wastageValue,
            String makingChargeType,
            BigDecimal makingChargeRate,
            BigDecimal makingChargeAmount,
            BigDecimal stoneCharge,
            BigDecimal diamondCharge,
            BigDecimal hallmarkCharge,
            BigDecimal otherCharge,
            BigDecimal discount,
            BigDecimal taxableValue,
            BigDecimal gstRate,
            BigDecimal gstAmount,
            BigDecimal lineTotal
    ) {
    }

    public record InvoiceOptions(
            BigDecimal discountAmount,
            String gstType,
            String roundingMode,
            BigDecimal oldGoldDeduction,
            BigDecimal advanceAdjusted,
            BigDecimal schemeAdjusted
    ) {
    }

    public record InvoiceBreakdown(
            List<ItemBreakdown> items,
            BigDecimal subtotal,
            BigDecimal makingTotal,
            BigDecimal stoneTotal,
            BigDecimal discountAmount,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal roundOff,
            BigDecimal oldGoldDeduction,
            BigDecimal advanceAdjusted,
            BigDecimal schemeAdjusted,
            BigDecimal grandTotal
    ) {
    }

    public record OldGoldInput(
            BigDecimal grossWeight,
            BigDecimal stoneWeight,
            BigDecimal purityPct,
            BigDecimal deductionPct,
            BigDecimal meltingLossPct,
            BigDecimal appliedRate
    ) {
    }

    public record OldGoldValuation(
            BigDecimal grossWeight,
            BigDecimal stoneWeight,
            BigDecimal netWeight,
            BigDecimal purityPct,
            BigDecimal fineGoldWeight,
            BigDecimal deductionPct,
            BigDecimal meltingLossPct,
            BigDecimal effectiveWeight,
            BigDecimal appliedRate,
            BigDecimal valuationAmount
    ) {
    }

    public ItemBreakdown calculateItem(ItemInput in) {
        BigDecimal gross = nonNegative(in.grossWeight());
        BigDecimal stone = nonNegative(in.stoneWeight());
        BigDecimal net = positive(in.netWeight()) ? in.netWeight() : nonNegative(gross.subtract(stone));

        BigDecimal rate = nonNegative(in.metalRate());
        BigDecimal metalValue = r2(net.multiply(rate));

        BigDecimal wastagePct = nonNegative(in.wastagePct());
        BigDecimal wastageWeight = positive(in.wastageWeight())
                ? in.wastageWeight()
                : r3(net.multiply(wastagePct).divide(HUNDRED));
        BigDecimal wastageValue = r2(wastageWeight.multiply(rate));

        String makingType = in.makingChargeType() == null ? "per_gram" : in.makingChargeType();
        BigDecimal makingRate = nonNegative(in.makingChargeRate());
        BigDecimal makingAmount = switch (makingType) {
            case "percentage" -> r2(metalValue.multiply(makingRate).divide(HUNDRED));
            case "fixed" -> r2(makingRate);
            default -> r2(net.multiply(makingRate));
        };

        BigDecimal stoneCharge = nonNegative(in.stoneCharge());
        BigDecimal diamondCharge = nonNegative(in.diamondCharge());
        BigDecimal hallmarkCharge = nonNegative(in.hallmarkCharge());
        BigDecimal otherCharge = nonNegative(in.otherCharge());
        BigDecimal discount = nonNegative(in.discount());

        BigDecimal beforeDiscount = metalValue.add(wastageValue).add(makingAmount).add(stoneCharge)
                .add(diamondCharge).add(hallmarkCharge).add(otherCharge);
        BigDecimal taxable = nonNegative(r2(beforeDiscount.subtract(discount)));

        BigDecimal gstRate = in.gstRate() == null ? new BigDecimal("3.00") : nonNegative(in.gstRate());
        BigDecimal gstAmount = r2(taxable.multiply(gstRate).divide(HUNDRED));
        BigDecimal lineTotal = r2(taxable.add(gstAmount));

        return new ItemBreakdown(
                r3(gross), r3(stone), r3(net), r2(rate), metalValue,
                wastagePct, wastageWeight, wastageValue,
                makingType, makingRate, makingAmount,
                r2(stoneCharge), r2(diamondCharge), r2(hallmarkCharge), r2(otherCharge), r2(discount),
                taxable, gstRate, gstAmount, lineTotal
        );
    }

    public InvoiceBreakdown calculateInvoice(List<ItemInput> inputs, InvoiceOptions options) {
        List<ItemBreakdown> items = inputs.stream().map(this::calculateItem).toList();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal makingTotal = BigDecimal.ZERO;
        BigDecimal stoneTotal = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;
        for (ItemBreakdown item : items) {
            subtotal = subtotal.add(item.taxableValue());
            makingTotal = makingTotal.add(item.makingChargeAmount());
            stoneTotal = stoneTotal.add(item.stoneCharge()).add(item.diamondCharge());
            totalGst = totalGst.add(item.gstAmount());
        }

        BigDecimal invoiceDiscount = nonNegative(options.discountAmount());
        BigDecimal netTaxable = nonNegative(subtotal.subtract(invoiceDiscount));

        if (subtotal.signum() > 0 && invoiceDiscount.signum() > 0) {
            totalGst = r2(totalGst.multiply(netTaxable).divide(subtotal, 10, RoundingMode.HALF_UP));
        }

        String gstType = options.gstType() == null ? "intra" : options.gstType();
        BigDecimal cgst = BigDecimal.ZERO.setScale(2);
        BigDecimal sgst = BigDecimal.ZERO.setScale(2);
        BigDecimal igst = BigDecimal.ZERO.setScale(2);
        if ("intra".equals(gstType)) {
            cgst = r2(totalGst.divide(new BigDecimal("2")));
            sgst = r2(totalGst.subtract(cgst));
        } else if ("inter".equals(gstType)) {
            igst = r2(totalGst);
        }

        BigDecimal grossTotal = netTaxable.add(cgst).add(sgst).add(igst);
        String roundingMode = options.roundingMode() == null ? "nearest_rupee" : options.roundingMode();
        BigDecimal rounded = switch (roundingMode) {
            case "nearest_5" -> roundToStep(grossTotal, new BigDecimal("5"));
            case "nearest_10" -> roundToStep(grossTotal, new BigDecimal("10"));
            case "none" -> r2(grossTotal);
            default -> grossTotal.setScale(0, RoundingMode.HALF_UP);
        };
        BigDecimal roundOff = r2(rounded.subtract(grossTotal));

        BigDecimal oldGold = nonNegative(options.oldGoldDeduction());
        BigDecimal advance = nonNegative(options.advanceAdjusted());
        BigDecimal scheme = nonNegative(options.schemeAdjusted());
        BigDecimal grandTotal = nonNegative(r2(rounded.subtract(oldGold).subtract(advance).subtract(scheme)));

        return new InvoiceBreakdown(
                items, r2(subtotal), r2(makingTotal), r2(stoneTotal), r2(invoiceDiscount),
                cgst, sgst, igst, roundOff, r2(oldGold), r2(advance), r2(scheme), grandTotal
        );
    }

    public OldGoldValuation calculateOldGold(OldGoldInput in) {
        BigDecimal gross = nonNegative(in.grossWeight());
        BigDecimal stone = nonNegative(in.stoneWeight());
        BigDecimal net = nonNegative(gross.subtract(stone));

        BigDecimal purityPct = clamp(in.purityPct(), new BigDecimal("91.60"), HUNDRED);
        BigDecimal deductionPct = clamp(in.deductionPct(), new BigDecimal("2.00"), new BigDecimal("50"));
        BigDecimal meltingLossPct = clamp(in.meltingLossPct(), new BigDecimal("1.00"), new BigDecimal("25"));

        BigDecimal fine = r3(net.multiply(purityPct).divide(HUNDRED));
        BigDecimal keepFactor = BigDecimal.ONE.subtract(deductionPct.add(meltingLossPct).divide(HUNDRED));
        BigDecimal effective = r3(fine.multiply(keepFactor));
        BigDecimal rate = nonNegative(in.appliedRate());
        BigDecimal valuation = r2(effective.multiply(rate));

        return new OldGoldValuation(r3(gross), r3(stone), r3(net), purityPct, fine,
                deductionPct, meltingLossPct, effective, r2(rate), valuation);
    }

    public static BigDecimal deriveRateFrom24K(BigDecimal rate24k, String purity) {
        BigDecimal factor = PURITY_FACTORS.getOrDefault(purity, BigDecimal.ONE);
        return r2(rate24k.multiply(factor));
    }

    private static BigDecimal roundToStep(BigDecimal value, BigDecimal step) {
        return value.divide(step, 0, RoundingMode.HALF_UP).multiply(step);
    }

    private static BigDecimal clamp(BigDecimal value, BigDecimal fallback, BigDecimal max) {
        BigDecimal v = value == null ? fallback : value;
        if (v.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return v.compareTo(max) > 0 ? max : v;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    public static BigDecimal r2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal r3(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP);
    }
}
