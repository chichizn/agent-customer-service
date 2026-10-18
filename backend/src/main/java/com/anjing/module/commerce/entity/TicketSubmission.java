package com.anjing.module.commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 每次已提交消息均记录回执，包括返回旧工单的消息，保证终态后的重试仍幂等。 */
@Entity
@Table(name = "commerce_ticket_submission")
@Getter @Setter
public class TicketSubmission {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false, length = 64) private String merchantId;
    @Column(nullable = false, length = 64) private String shopId;
    @Column(nullable = false, length = 64) private String customerId;
    @Column(nullable = false, length = 64) private String sessionId;
    @Column(nullable = false, length = 64) private String messageId;
    @Column(nullable = false, length = 64) private String payloadHash;
    @Column(nullable = false, length = 64) private String ticketId;
    @Column(nullable = false) private boolean demoData;
}
