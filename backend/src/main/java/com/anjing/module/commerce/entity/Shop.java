package com.anjing.module.commerce.entity;

import com.anjing.module.commerce.CommerceTypes.Platform;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "commerce_shop", indexes = @Index(name = "idx_commerce_shop_merchant", columnList = "merchantId"))
@Getter @Setter
public class Shop {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false, length = 64) private String merchantId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Platform platform;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private boolean demoData;
}
