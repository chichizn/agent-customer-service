package com.anjing.module.commerce;

import com.anjing.module.commerce.CommerceTypes.*;
import com.anjing.module.demo.DemoBusinessTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class CommerceVO {
    private CommerceVO() {}
    public record Item(String itemId, String productId, String productName, String specification, int quantity, BigDecimal unitPrice) {}
    public record Order(String orderId, String orderNumber, String merchantId, String shopId, String shopName,
        Platform platform, String customerId, List<Item> items, int totalQuantity, BigDecimal actualPaid,
        OrderStatus status, LocalDateTime orderedAt) {}
    public record OrderMatches(MatchStatus matchStatus, List<Order> orders, String message) {}
    public record LogisticsNode(String nodeId, LocalDateTime occurredAt, String description) {}
    public record LogisticsResult(String orderId, boolean available, String unavailableReason,
        String carrier, String trackingNumber, String status, List<LogisticsNode> nodes) {}
    public record HandlingRecord(String historyId, TicketStatus fromStatus, TicketStatus toStatus,
        String note, String operatorId, LocalDateTime occurredAt) {}
    public record Ticket(String ticketId, String ticketNumber, String merchantId, String shopId,
        String customerId, String orderId, String orderNumber, String sessionId, TicketType ticketType,
        String reason, TicketStatus status, String platformAfterSaleStatus, LocalDateTime createdAt,
        LocalDateTime updatedAt, List<HandlingRecord> history) {}
    public record TicketCreated(Ticket ticket, boolean created, String reuseReason, String message) {}
    public record CustomerIdentity(String customerId, String platformUserId, String name) {}
    public record ShopIdentity(String shopId, String name, Platform platform, List<CustomerIdentity> customers) {}
    public record DemoIdentities(String merchantId, String merchantName, List<ShopIdentity> shops,
        DemoBusinessTime.Info businessTime) {}
    public record DemoRestored(String merchantId, int shops, int customers, int orders, String message) {}
}
