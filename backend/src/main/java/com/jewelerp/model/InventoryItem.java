package com.jewelerp.model;

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
@Table(name = "inventory_items")
public class InventoryItem extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String itemCode;

    @Column(nullable = false, unique = true, length = 60)
    private String barcode;

    @ManyToOne(optional = false)
    private Product product;

    @ManyToOne(optional = false)
    private Branch branch;

    @ManyToOne
    private Counter counter;

    @Column(precision = 10, scale = 3)
    private BigDecimal grossWeight = BigDecimal.ZERO;

    @Column(precision = 10, scale = 3)
    private BigDecimal stoneWeight = BigDecimal.ZERO;

    @Column(precision = 10, scale = 3)
    private BigDecimal diamondWeight = BigDecimal.ZERO;

    @Column(precision = 10, scale = 3)
    private BigDecimal netWeight = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String purity;

    @Column(unique = true, length = 30)
    private String huid;

    private String hallmarkCenter;

    @Column(precision = 10, scale = 2)
    private BigDecimal hallmarkCharge = new BigDecimal("45");

    @Column(precision = 14, scale = 2)
    private BigDecimal purchaseCost = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal stoneCharge = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal diamondCharge = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal otherCharge = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    private String status = "available";

    @Column(length = 2000)
    private String notes;
}
