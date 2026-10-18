package com.anjing.module.scene.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 意图实体
 */
@Data
@Entity
@Table(name = "cs_scene_intent")
public class Intent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String intentName;

    @Column(length = 64)
    private String intentCode;

    @Column(length = 64)
    private String sceneType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 512)
    private String triggerKeywords;

    private Double confidenceThreshold = 0.8;

    private Integer hitCount = 0;

    private Integer priority = 5;

    @Column(length = 20)
    private String status = "启用";

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
