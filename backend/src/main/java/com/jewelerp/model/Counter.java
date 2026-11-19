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
@Table(name = "counters")
public class Counter extends BaseEntity {

    @ManyToOne(optional = false)
    private Branch branch;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String code;

    private boolean active = true;
}
