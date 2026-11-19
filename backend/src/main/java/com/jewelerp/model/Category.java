package com.jewelerp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(length = 20)
    private String defaultMakingChargeType = "per_gram";

    @Column(precision = 12, scale = 2)
    private BigDecimal defaultMakingChargeValue = new BigDecimal("600");

    @Column(precision = 5, scale = 2)
    private BigDecimal defaultWastagePct = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    private BigDecimal gstRate = new BigDecimal("3.00");
}
