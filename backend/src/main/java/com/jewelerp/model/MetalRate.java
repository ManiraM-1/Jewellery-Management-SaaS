package com.jewelerp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "metal_rates", indexes = @Index(columnList = "metalType,purity,effectiveAt"))
public class MetalRate extends BaseEntity {

    @ManyToOne
    private Branch branch;

    @Column(nullable = false, length = 30)
    private String metalType;

    @Column(nullable = false, length = 20)
    private String purity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerGram;

    @Column(nullable = false)
    private LocalDateTime effectiveAt;

    @Column(nullable = false, length = 20)
    private String status = "approved";

    @ManyToOne
    private AppUser createdBy;

    @ManyToOne
    private AppUser approvedBy;

    private String notes;
}
