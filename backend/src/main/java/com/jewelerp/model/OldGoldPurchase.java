package com.jewelerp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "old_gold_purchases")
public class OldGoldPurchase extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String receiptNo;

    @ManyToOne(optional = false)
    private Branch branch;

    @ManyToOne
    private Customer customer;

    private Long invoiceId;

    @Column(nullable = false)
    private String itemDescription;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal grossWeight;

    @Column(precision = 10, scale = 3)
    private BigDecimal stoneWeight = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal netWeight;

    @Column(nullable = false, length = 20)
    private String testedPurity;

    @Column(precision = 5, scale = 2)
    private BigDecimal purityPct;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal fineGoldWeight;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal appliedRate;

    @Column(precision = 5, scale = 2)
    private BigDecimal deductionPct;

    @Column(precision = 5, scale = 2)
    private BigDecimal meltingLossPct;

    @Column(precision = 10, scale = 3)
    private BigDecimal effectiveWeight;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valuationAmount;

    @Column(length = 20)
    private String settlementType = "exchange";

    private boolean customerDeclaration = true;

    @Column(length = 30)
    private String status = "pending_approval";

    private String refineryName;
    private LocalDate refineryTransferDate;

    @ManyToOne
    private AppUser approvedBy;
}
