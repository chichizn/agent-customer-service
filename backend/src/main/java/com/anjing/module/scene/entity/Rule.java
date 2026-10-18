package com.anjing.module.scene.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 场景规则实体
 */
@Data
@Entity
@Table(name = "cs_scene_rule")
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String ruleName;

    @Column(length = 64)
    private String ruleCode;

    @Column(length = 64)
    private String ruleType;

    @Column(length = 64)
    private String sceneType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    @Column(columnDefinition = "TEXT")
    private String actions;

    private Integer priority = 5;

    private Integer triggerCount = 0;

    private Boolean enabled = true;

    @Column(length = 32)
    private String effectiveTime;

    @Column(length = 32)
    private String expireTime;

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
