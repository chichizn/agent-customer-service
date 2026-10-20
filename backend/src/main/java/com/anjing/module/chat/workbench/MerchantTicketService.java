package com.anjing.module.chat.workbench;

import com.anjing.module.chat.entity.ChatExecution;
import com.anjing.module.chat.entity.ChatSession;
import com.anjing.module.chat.repository.ChatExecutionRepository;
import com.anjing.module.chat.repository.ChatSessionRepository;
import com.anjing.module.commerce.CommerceDTO.UpdateTicket;
import com.anjing.module.commerce.CommerceService;
import com.anjing.module.commerce.CommerceVO.Ticket;
import com.anjing.module.commerce.repository.MerchantRepository;
import com.anjing.module.commerce.repository.ServiceTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

/** Keeps merchant updates and their session execution record in the same transaction. */
@Service
@RequiredArgsConstructor
public class MerchantTicketService {
    private final CommerceService commerce;
    private final MerchantRepository merchants;
    private final ServiceTicketRepository tickets;
    private final ChatSessionRepository sessions;
    private final ChatExecutionRepository executions;
    private final PlatformTransactionManager transactionManager;

    public Ticket update(UpdateTicket request) {
        try {
            return transaction().execute(status -> {
                Ticket result = commerce.updateTicket(request);
                record(request, "SUCCESS", "工单状态已由 " + request.expectedStatus() + " 更新为 " + result.status());
                return result;
            });
        } catch (RuntimeException failure) {
            try {
                transaction().executeWithoutResult(status -> record(request, "FAILED", "工单状态更新失败，状态未变更，请刷新后重试"));
            } catch (RuntimeException recordFailure) {
                // If storage itself is unavailable, preserve the original failure for the HTTP handler.
                failure.addSuppressed(recordFailure);
            }
            throw failure;
        }
    }

    private TransactionTemplate transaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }

    private void record(UpdateTicket request, String status, String summary) {
        // Match the merchant -> session lock order used by message processing and reset.
        if (merchants.lockById(request.merchantId()).isEmpty()) return;
        tickets.findById(request.ticketId())
            .filter(ticket -> request.merchantId().equals(ticket.getMerchantId()) && request.shopId().equals(ticket.getShopId()))
            .filter(ticket -> ticket.getSessionId() != null)
            .ifPresent(ticket -> sessions.lockById(ticket.getSessionId())
                .filter(session -> owns(session, ticket.getMerchantId(), ticket.getShopId(), ticket.getCustomerId()))
                .ifPresent(session -> {
                    ChatExecution execution = new ChatExecution();
                    execution.setId(UUID.randomUUID().toString());
                    execution.setSessionId(session.getSessionId());
                    execution.setAction("update_service_ticket");
                    execution.setStatus(status);
                    execution.setSummary(summary);
                    execution.setRelatedId(ticket.getId());
                    execution.setCreatedAt(LocalDateTime.now());
                    executions.saveAndFlush(execution);
                    session.setUpdatedAt(execution.getCreatedAt());
                    sessions.save(session);
                }));
    }

    private boolean owns(ChatSession session, String merchant, String shop, String customer) {
        return merchant.equals(session.getMerchantId()) && shop.equals(session.getShopId()) && customer.equals(session.getCustomerId());
    }
}
