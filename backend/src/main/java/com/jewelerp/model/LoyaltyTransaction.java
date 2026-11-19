package com.jewelerp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loyalty_transactions")
public class LoyaltyTransaction extends BaseEntity {

    @JsonIgnore
    @ManyToOne(optional = false)
    private Customer customer;

    private int points;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 80)
    private String reference;

    private String description;
}
