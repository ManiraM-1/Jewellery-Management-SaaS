package com.jewelerp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice_items")
public class InvoiceItem extends BaseEntity {

    @JsonIgnore
    @ManyToOne(optional = false)
    private Invoice invoice;

    private Long inventoryItemId;

    @Column(nullable = false)
    private String productName;

    @Column(length = 30)
    private String metalType;

    @Column(length = 20)
    private String purity;

    @Column(length = 30)
    private String huid;

    @Column(precision = 10, scale = 3)
    private BigDecimal grossWeight;

    @Column(precision = 10, scale = 3)
    private BigDecimal stoneWeight;

    @Column(precision = 10, scale = 3)
    private BigDecimal netWeight;

    @Column(precision = 12, scale = 2)
    private BigDecimal metalRate;

    @Column(precision = 14, scale = 2)
    private BigDecimal metalValue;

    @Column(precision = 5, scale = 2)
    private BigDecimal wastagePct;

    @Column(precision = 10, scale = 3)
    private BigDecimal wastageWeight;

    @Column(precision = 14, scale = 2)
    private BigDecimal wastageValue;

    @Column(length = 20)
    private String makingChargeType;

    @Column(precision = 12, scale = 2)
    private BigDecimal makingChargeRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal makingChargeAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal stoneCharge;

    @Column(precision = 12, scale = 2)
    private BigDecimal diamondCharge;

    @Column(precision = 10, scale = 2)
    private BigDecimal hallmarkCharge;

    @Column(precision = 10, scale = 2)
    private BigDecimal otherCharge;

    @Column(precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(precision = 14, scale = 2)
    private BigDecimal taxableValue;

    @Column(precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal gstAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal lineTotal;
}
