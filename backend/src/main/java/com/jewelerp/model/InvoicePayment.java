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
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice_payments")
public class InvoicePayment extends BaseEntity {

    @JsonIgnore
    @ManyToOne(optional = false)
    private Invoice invoice;

    @Column(nullable = false, length = 30)
    private String method;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 80)
    private String referenceNo;

    @Column(nullable = false)
    private LocalDateTime paidAt;
}
