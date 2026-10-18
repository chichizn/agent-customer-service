package com.anjing.module.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话实体
 */
@Data
@Entity
@Table(name = "cs_chat_session")
public class ChatSession {

    @Id
    @Column(length = 64)
    private String sessionId;

    @Column(length = 64)
    private String userId;

    @Column(length = 64)
    private String userName;

    @Column(length = 32)
    private String channel;

    @Column(length = 20)
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = "active";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
