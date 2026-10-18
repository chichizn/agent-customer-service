package com.anjing.module.commerce.entity;

import com.anjing.module.commerce.CommerceTypes.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commerce_service_ticket", uniqueConstraints = @UniqueConstraint(name = "uk_commerce_ticket_number", columnNames = "ticketNumber"),
    indexes = {@Index(name = "idx_commerce_ticket_scope", columnList = "merchantId,shopId,customerId"),
        @Index(name = "idx_commerce_ticket_order", columnList = "orderId,ticketType,status"),
        @Index(name = "idx_commerce_ticket_session", columnList = "sessionId")})
@Getter @Setter
public class ServiceTicket {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false, length = 64) private String ticketNumber;
    @Column(nullable = false, length = 64) private String merchantId;
    @Column(nullable = false, length = 64) private String shopId;
    @Column(nullable = false, length = 64) private String customerId;
    @Column(nullable = false, length = 64) private String orderId;
    @Column(nullable = false, length = 64) private String sessionId;
    @Column(nullable = false, length = 64) private String messageId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private TicketType ticketType;
    @Column(nullable = false, length = 1000) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private TicketStatus status;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @Column(nullable = false) private boolean demoData;
    @ElementCollection
    @CollectionTable(name = "commerce_ticket_history", joinColumns = @JoinColumn(name = "ticket_id"))
    @OrderColumn(name = "history_position")
    private List<History> history = new ArrayList<>();

    @Embeddable @Getter @Setter
    public static class History {
        @Column(nullable = false, length = 64) private String historyId;
        @Enumerated(EnumType.STRING) @Column(length = 24) private TicketStatus fromStatus;
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private TicketStatus toStatus;
        @Column(nullable = false, length = 1000) private String note;
        @Column(nullable = false, length = 64) private String operatorId;
        @Column(nullable = false) private LocalDateTime occurredAt;
    }
}
