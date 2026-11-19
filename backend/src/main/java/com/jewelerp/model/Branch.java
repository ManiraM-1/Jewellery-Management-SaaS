package com.jewelerp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "branches")
public class Branch extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    private String phone;
    private String email;

    @Column(length = 1000)
    private String address;

    private String city;
    private String state = "Andhra Pradesh";
    private String gstin;
    private boolean headOffice;
    private boolean active = true;
}
