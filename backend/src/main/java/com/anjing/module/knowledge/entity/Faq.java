package com.anjing.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * FAQ问答实体
 */
@Data
@Entity
@Table(name = "cs_knowledge_faq")
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 512, nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(length = 64)
    private String category;

    @Column(length = 512)
    private String relatedQuestions;

    @Column(length = 256)
    private String tags;

    private Integer hitCount = 0;

    private Integer priority = 5;

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
