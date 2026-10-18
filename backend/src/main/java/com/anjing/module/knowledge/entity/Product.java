package com.anjing.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品知识实体
 */
@Data
@Entity
@Table(name = "cs_knowledge_product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String productName;

    @Column(length = 64)
    private String productCode;

    @Column(length = 64)
    private String category;

    @Column(length = 64)
    private String brand;

    private Double price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Column(length = 512)
    private String features;

    @Column(length = 256)
    private String imageUrl;

    @Column(length = 256)
    private String tags;

    private Integer viewCount = 0;

    @Column(length = 20)
    private String status = "草稿";

    private Boolean vectorized = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
