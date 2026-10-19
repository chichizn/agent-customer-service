package com.anjing.module.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Data
@Entity
@Table(name = "cs_chat_message")
public class ChatMessage {

    @Id
    @Column(length = 64)
    private String messageId;

    @Column(length = 64, nullable = false)
    private String sessionId;

    @Column(length = 20, nullable = false)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 32)
    private String cardType;

    private LocalDateTime createdAt;

    @Column(length = 64) private String clientMessageId;
    @Column(columnDefinition = "TEXT") private String orderIdsJson;
    @Column(length = 64) private String ticketId;
    @Column(length = 16) private String deliveryState;
    private Long messageSequence;
    private Long processingVersion;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
