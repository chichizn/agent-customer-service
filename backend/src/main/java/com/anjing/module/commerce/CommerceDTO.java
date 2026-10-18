package com.anjing.module.commerce;

import com.anjing.module.commerce.CommerceTypes.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class CommerceDTO {
    private CommerceDTO() {}
    public record Scope(
        @NotBlank @Size(max = 64) String merchantId,
        @NotBlank @Size(max = 64) String shopId,
        @NotBlank @Size(max = 64) String customerId) {}

    /** 多个条件同时提供时按 AND 匹配；空条件列出该顾客的所有订单。 */
    public record OrderQuery(@NotNull @Valid Scope scope,
        @Size(max = 64) String orderId, @Size(max = 64) String orderNumber,
        @Size(max = 100) String productClue) {}
    public record LogisticsQuery(@NotNull @Valid Scope scope, @NotBlank @Size(max = 64) String orderId) {}
    public record CreateTicket(@NotNull @Valid Scope scope,
        @NotBlank @Size(max = 64) String sessionId, @NotBlank @Size(max = 64) String messageId,
        @NotBlank @Size(max = 64) String orderId, @NotNull TicketType ticketType,
        @NotBlank @Size(max = 1000) String reason, @AssertTrue boolean customerConfirmed) {}
    public record TicketQuery(@NotNull @Valid Scope scope,
        @Size(max = 64) String ticketNumber, @Size(max = 64) String orderId, @Size(max = 64) String sessionId) {}
    public record UpdateTicket(@NotBlank @Size(max = 64) String merchantId,
        @NotBlank @Size(max = 64) String shopId, @NotBlank @Size(max = 64) String ticketId,
        @NotNull TicketStatus expectedStatus, @NotNull TicketStatus status,
        @NotBlank @Size(max = 1000) String note, @NotBlank @Size(max = 64) String operatorId) {}
    public record RestoreDemo(@AssertTrue boolean confirmed) {}
}
