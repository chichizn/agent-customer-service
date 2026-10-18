package com.anjing.module.scene.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 提示词模板实体
 */
@Data
@Entity
@Table(name = "cs_scene_prompt")
public class Prompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String promptName;

    @Column(length = 64)
    private String promptCode;

    @Column(length = 32)
    private String promptType;

    @Column(length = 64)
    private String sceneType;

    @Column(columnDefinition = "TEXT")
    private String promptContent;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 512)
    private String variables;

    private Integer usageCount = 0;

    @Column(length = 20)
    private String version = "v1.0";

    @Column(length = 20)
    private String status = "草稿";

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
