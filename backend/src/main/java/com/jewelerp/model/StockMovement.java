package com.jewelerp.model;

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
@Table(name = "stock_movements")
public class StockMovement extends BaseEntity {

    @ManyToOne(optional = false)
    private InventoryItem inventoryItem;

    @ManyToOne(optional = false)
    private Branch branch;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(length = 80)
    private String reference;

    @Column(length = 1000)
    private String notes;

    @ManyToOne
    private AppUser user;
}
