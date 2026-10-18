package com.anjing.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 活动知识实体
 */
@Data
@Entity
@Table(name = "cs_knowledge_activity")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String activityName;

    @Column(length = 64)
    private String activityCode;

    @Column(length = 32)
    private String activityType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 32)
    private String startTime;

    @Column(length = 32)
    private String endTime;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Column(length = 256)
    private String applicableProducts;

    private Double discountRate;

    private Integer usageCount = 0;

    @Column(length = 20)
    private String status = "未开始";

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
