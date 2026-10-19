package com.anjing.module.chat.workbench;

import java.util.List;
import java.util.Map;

public final class WorkbenchVO {
    private WorkbenchVO() {}
    public record Session(String sessionId, String shopId, String shopName, String platform,
        String customerId, String customerName, String receptionMode, String status,
        String lastMessage, String updatedAt, String handoffSummary) {}
    public record Message(String messageId, String sessionId, String clientMessageId, String role,
        String content, String createdAt, List<String> orderIds, String ticketId) {}
    public record Knowledge(String knowledgeId, String title, String source, String content, String scope) {}
    public record Execution(String executionId, String action, String status, String summary,
        String relatedId, String createdAt) {}
    public record Snapshot(Session session, List<Message> messages, List<Map<String,Object>> orders,
        List<Map<String,Object>> logistics, List<Map<String,Object>> tickets,
        List<Knowledge> knowledge, List<Execution> executions, String selectedOrderId,
        boolean awaitingConfirmation) {}
}
