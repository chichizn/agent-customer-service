package com.anjing.module.commerce;

public final class CommerceTypes {
    private CommerceTypes() {}
    public enum Platform { TAOBAO, DOUYIN }
    public enum OrderStatus { PENDING_SHIPMENT, SHIPPED, DELIVERED, CANCELLED }
    public enum TicketType { RETURN_REQUEST, EXCEPTION_COMPENSATION }
    public enum TicketStatus { PENDING_REVIEW, PROCESSING, COMPLETED, REJECTED }
    public enum MatchStatus { NOT_FOUND, MATCHED, NEED_SELECTION }
}
