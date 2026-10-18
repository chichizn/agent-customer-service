package com.anjing.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 解决方案实体
 */
@Data
@Entity
@Table(name = "cs_knowledge_solution")
public class Solution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String solutionName;

    @Column(length = 64)
    private String solutionCode;

    @Column(length = 64)
    private String sceneType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 256)
    private String triggerCondition;

    @Column(columnDefinition = "TEXT")
    private String steps;

    @Column(length = 256)
    private String expectedOutcome;

    private Integer usageCount = 0;

    private Double successRate = 0.0;

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
