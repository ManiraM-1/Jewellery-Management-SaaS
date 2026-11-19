package com.jewelerp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String customerCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 30)
    private String phone;

    private String email;

    @Column(length = 1000)
    private String address;

    private LocalDate dob;
    private LocalDate anniversary;

    @Column(length = 20)
    private String panNumber;

    @Column(length = 20)
    private String groupTier = "standard";

    @Column(precision = 14, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    private int loyaltyPoints;

    @Column(length = 2000)
    private String preferences;

    @Column(length = 2000)
    private String notes;
}
