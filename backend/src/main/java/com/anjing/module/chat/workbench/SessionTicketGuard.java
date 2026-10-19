package com.anjing.module.chat.workbench;

import com.anjing.module.chat.repository.*;
import com.anjing.module.commerce.*;
import com.anjing.module.commerce.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Component @RequiredArgsConstructor
public class SessionTicketGuard implements AgentTicketGuard {
    private final MerchantRepository merchants;
    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;

    // Every writer uses merchant -> session lock order, including takeover/reset.
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public void lockAndVerify(CommerceDTO.CreateTicket request) {
        merchants.lockById(request.scope().merchantId()).orElseThrow(SessionTicketGuard::denied);
        var s = sessions.lockById(request.sessionId()).orElseThrow(SessionTicketGuard::denied);
        var m = messages.findById(request.messageId()).orElseThrow(SessionTicketGuard::denied);
        if (!"active".equals(s.getStatus()) || !"AI".equals(s.getReceptionMode()) ||
            !Objects.equals(s.getMerchantId(),request.scope().merchantId()) ||
            !Objects.equals(s.getShopId(),request.scope().shopId()) ||
            !Objects.equals(s.getCustomerId(),request.scope().customerId()) ||
            !Objects.equals(s.getSelectedOrderId(),request.orderId()) ||
            !Objects.equals(s.getPendingAction(),request.ticketType()==CommerceTypes.TicketType.RETURN_REQUEST?"RETURN":"COMPENSATION") ||
            !Objects.equals(s.getPendingReason(),request.reason()) ||
            !Boolean.TRUE.equals(s.getAwaitingConfirmation()) || !request.customerConfirmed() ||
            !Objects.equals(s.getConfirmationMessageId(),request.messageId()) ||
            !Objects.equals(s.getInFlightMessageId(),request.messageId()) ||
            !Objects.equals(m.getSessionId(),s.getSessionId()) || !"customer".equals(m.getRole()) ||
            !ConfirmationText.matches(m.getContent())) throw denied();
    }
    private static CommerceException denied() {
        return new CommerceException("SESSION_WRITE_REJECTED","会话身份、接待模式或顾客确认已变化，请重新确认或转人工");
    }
}
