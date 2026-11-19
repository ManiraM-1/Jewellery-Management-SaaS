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
@Table(name = "activity_logs")
public class ActivityLog extends BaseEntity {

    @ManyToOne
    private AppUser user;

    @Column(nullable = false, length = 60)
    private String module;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(nullable = false, length = 2000)
    private String description;
}
