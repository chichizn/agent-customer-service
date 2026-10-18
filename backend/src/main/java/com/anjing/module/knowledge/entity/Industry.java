package com.anjing.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 行业知识实体
 */
@Data
@Entity
@Table(name = "cs_knowledge_industry")
public class Industry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 256, nullable = false)
    private String title;

    @Column(length = 64)
    private String industryType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 128)
    private String source;

    @Column(length = 256)
    private String keywords;

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
