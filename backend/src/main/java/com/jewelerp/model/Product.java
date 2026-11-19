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
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(optional = false)
    private Category category;

    @Column(nullable = false, length = 50)
    private String productType;

    @Column(nullable = false, length = 30)
    private String metalType;

    @Column(nullable = false, length = 20)
    private String purity;

    @Column(length = 20)
    private String makingChargeType = "per_gram";

    @Column(precision = 12, scale = 2)
    private BigDecimal makingChargeValue = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    private BigDecimal wastagePct = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    private BigDecimal gstRate = new BigDecimal("3.00");

    @Column(length = 20)
    private String status = "active";
}
