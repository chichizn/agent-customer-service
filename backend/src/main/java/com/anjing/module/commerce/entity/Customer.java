package com.anjing.module.commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "commerce_customer", indexes = @Index(name = "idx_commerce_customer_scope", columnList = "merchantId,shopId"))
@Getter @Setter
public class Customer {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false, length = 64) private String merchantId;
    @Column(nullable = false, length = 64) private String shopId;
    @Column(nullable = false, length = 64) private String platformUserId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private boolean demoData;
}
