package com.jewelerp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String invoiceNo;

    @ManyToOne(optional = false)
    private Branch branch;

    @ManyToOne
    private Customer customer;

    @ManyToOne
    private AppUser salesperson;

    @ManyToOne
    private AppUser cashier;

    @Column(length = 20)
    private String invoiceType = "sale";

    @Column(length = 15)
    private String gstType = "intra";

    @Column(precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal makingTotal = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal stoneTotal = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(precision = 8, scale = 2)
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal oldGoldDeduction = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal advanceAdjusted = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal schemeAdjusted = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Column(length = 20)
    private String status = "paid";

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime billedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id")
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id")
    private List<InvoicePayment> payments = new ArrayList<>();
}
