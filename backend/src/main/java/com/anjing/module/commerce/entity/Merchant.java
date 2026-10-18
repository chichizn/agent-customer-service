package com.anjing.module.commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "commerce_merchant")
@Getter @Setter
public class Merchant {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private boolean demoData;
}
