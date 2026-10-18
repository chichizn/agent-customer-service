package com.anjing.module.commerce;

import com.anjing.module.commerce.CommerceDTO.*;
import com.anjing.module.commerce.CommerceTypes.*;
import com.anjing.module.commerce.CommerceVO.*;
import com.anjing.module.commerce.entity.*;
import com.anjing.module.commerce.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Validated
@RequiredArgsConstructor
public class CommerceService {
    private final MerchantRepository merchants;
    private final ShopRepository shops;
    private final CustomerRepository customers;
    private final CommerceOrderRepository orders;
    private final LogisticsRepository logistics;
    private final ServiceTicketRepository tickets;
    private final TicketSubmissionRepository submissions;
    private final ObjectProvider<AgentTicketGuard> agentGuard;

    @Transactional(readOnly = true)
    public OrderMatches getOrder(@NotNull @Valid OrderQuery request) {
        Shop shop = checkScope(request.scope());
        List<Order> found = orders.findByMerchantIdAndShopIdAndCustomerIdOrderByOrderedAtDescIdAsc(
            request.scope().merchantId(), request.scope().shopId(), request.scope().customerId()).stream()
            .filter(o -> blank(request.orderId()) || o.getId().equals(request.orderId().trim()))
            .filter(o -> blank(request.orderNumber()) || o.getOrderNumber().equals(request.orderNumber().trim()))
            .filter(o -> blank(request.productClue()) || o.getItems().stream().anyMatch(i ->
                (i.getProductName() + " " + Objects.toString(i.getSpecification(), "")).toLowerCase(Locale.ROOT)
                    .contains(request.productClue().trim().toLowerCase(Locale.ROOT))))
            .map(o -> orderView(o, shop)).toList();
        MatchStatus status = found.isEmpty() ? MatchStatus.NOT_FOUND : found.size() == 1 ? MatchStatus.MATCHED : MatchStatus.NEED_SELECTION;
        return new OrderMatches(status, found, switch (status) {
            case NOT_FOUND -> "当前店铺和顾客下未找到匹配订单";
            case MATCHED -> "已找到一笔匹配订单";
            case NEED_SELECTION -> "找到多笔订单，请顾客选择后再继续";
        });
    }

    @Transactional(readOnly = true)
    public LogisticsResult getLogistics(@NotNull @Valid LogisticsQuery request) {
        checkScope(request.scope());
        CommerceOrder order = ownedOrder(request.scope(), request.orderId());
        if (order.getStatus() == OrderStatus.PENDING_SHIPMENT)
            return noLogistics(order.getId(), "订单尚未发货，暂无物流");
        if (order.getStatus() == OrderStatus.CANCELLED)
            return noLogistics(order.getId(), "订单已取消，暂无可查询物流");
        return logistics.findByOrderId(order.getId()).filter(l -> l.getMerchantId().equals(request.scope().merchantId()))
            .map(l -> new LogisticsResult(order.getId(), true, null, l.getCarrier(), l.getTrackingNumber(), l.getStatus(),
                l.getNodes().stream().sorted(Comparator.comparing(Logistics.Node::getOccurredAt).thenComparing(Logistics.Node::getNodeId))
                    .map(n -> new LogisticsNode(n.getNodeId(), n.getOccurredAt(), n.getDescription())).toList()))
            .orElseGet(() -> noLogistics(order.getId(), "暂未提供物流信息，请联系商家核实"));
    }

    /** 独立商家演示接口；聊天 Agent 必须使用 createAgentTicket。 */
    @Transactional
    public TicketCreated createTicket(@NotNull @Valid CreateTicket request) {
        return persistTicket(request);
    }

    @Transactional
    public TicketCreated createAgentTicket(@NotNull @Valid CreateTicket request) {
        AgentTicketGuard guard = agentGuard.getIfAvailable();
        if (guard == null) throw error("AGENT_GUARD_UNAVAILABLE", "会话写入校验尚未接入，请转人工处理");
        guard.lockAndVerify(request);
        return persistTicket(request);
    }

    private TicketCreated persistTicket(CreateTicket request) {
        if (!request.customerConfirmed()) throw error("CONFIRMATION_REQUIRED", "需顾客确认后才能提交诉求");
        Merchant merchant = lockMerchant(request.scope().merchantId());
        checkScope(request.scope());
        CommerceOrder order = ownedOrder(request.scope(), request.orderId());
        String submissionId = hash(request.scope().merchantId(), request.scope().shopId(), request.scope().customerId(), request.sessionId(), request.messageId());
        String payloadHash = hash(request.orderId(), request.ticketType().name(), request.reason().trim());
        Optional<TicketSubmission> previous = submissions.lockById(submissionId);
        if (previous.isPresent()) {
            if (!payloadHash.equals(previous.get().getPayloadHash()))
                throw error("IDEMPOTENCY_CONFLICT", "同一消息的提交内容发生变化，请使用新的消息编号");
            ServiceTicket ticket = tickets.findById(previous.get().getTicketId())
                .orElseThrow(() -> error("DATA_INCONSISTENT", "提交记录对应的工单不存在，请联系商家核实"));
            return createdResult(ticket, false, "MESSAGE_RETRY");
        }
        // 同一商家的写操作竞争同一数据库行锁，跨进程也串行；终态更新、恢复也使用该锁。
        List<ServiceTicket> open = tickets.findByMerchantIdAndShopIdAndCustomerIdAndOrderIdAndTicketTypeAndStatusInOrderByCreatedAtAsc(
            request.scope().merchantId(), request.scope().shopId(), request.scope().customerId(), order.getId(), request.ticketType(),
            List.of(TicketStatus.PENDING_REVIEW, TicketStatus.PROCESSING));
        boolean created = open.isEmpty();
        ServiceTicket ticket;
        if (created) {
            ticket = new ServiceTicket();
            ticket.setId(UUID.randomUUID().toString());
            ticket.setTicketNumber("ST" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT));
            ticket.setMerchantId(request.scope().merchantId());
            ticket.setShopId(request.scope().shopId());
            ticket.setCustomerId(request.scope().customerId());
            ticket.setOrderId(order.getId());
            ticket.setSessionId(request.sessionId());
            ticket.setMessageId(request.messageId());
            ticket.setTicketType(request.ticketType());
            ticket.setReason(request.reason().trim());
            ticket.setStatus(TicketStatus.PENDING_REVIEW);
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setUpdatedAt(ticket.getCreatedAt());
            ticket.setDemoData(merchant.isDemoData() && order.isDemoData());
            ticket.getHistory().add(history(null, TicketStatus.PENDING_REVIEW, "已记录诉求，待商家审核", request.scope().customerId()));
            tickets.save(ticket);
        } else ticket = open.get(0);
        TicketSubmission submission = new TicketSubmission();
        submission.setId(submissionId);
        submission.setMerchantId(request.scope().merchantId());
        submission.setShopId(request.scope().shopId());
        submission.setCustomerId(request.scope().customerId());
        submission.setSessionId(request.sessionId());
        submission.setMessageId(request.messageId());
        submission.setPayloadHash(payloadHash);
        submission.setTicketId(ticket.getId());
        submission.setDemoData(ticket.isDemoData());
        submissions.saveAndFlush(submission);
        return createdResult(ticket, created, created ? null : "OPEN_SAME_TYPE");
    }

    @Transactional(readOnly = true)
    public List<Ticket> getServiceTicket(@NotNull @Valid TicketQuery request) {
        checkScope(request.scope());
        // 所有过滤器取交集，外店工单号与不存在的工单号均返回空列表。
        return tickets.findByMerchantIdAndShopIdAndCustomerIdOrderByCreatedAtDescIdAsc(
            request.scope().merchantId(), request.scope().shopId(), request.scope().customerId()).stream()
            .filter(t -> blank(request.ticketNumber()) || t.getTicketNumber().equals(request.ticketNumber().trim()))
            .filter(t -> blank(request.orderId()) || t.getOrderId().equals(request.orderId().trim()))
            .filter(t -> blank(request.sessionId()) || t.getSessionId().equals(request.sessionId().trim()) ||
                submissions.existsByTicketIdAndSessionIdAndMerchantIdAndShopIdAndCustomerId(t.getId(), request.sessionId().trim(),
                    request.scope().merchantId(), request.scope().shopId(), request.scope().customerId()))
            .map(this::ticketView).toList();
    }

    @Transactional
    public Ticket updateTicket(@NotNull @Valid UpdateTicket request) {
        lockMerchant(request.merchantId());
        Shop shop = shops.findById(request.shopId()).filter(s -> s.getMerchantId().equals(request.merchantId()))
            .orElseThrow(CommerceService::notFound);
        ServiceTicket ticket = tickets.findById(request.ticketId())
            .filter(t -> t.getMerchantId().equals(request.merchantId()) && t.getShopId().equals(shop.getId()))
            .orElseThrow(CommerceService::notFound);
        if (ticket.getStatus() != request.expectedStatus()) throw error("STALE_STATUS", "工单已被更新，请重新加载后操作");
        boolean allowed = (ticket.getStatus() == TicketStatus.PENDING_REVIEW &&
            (request.status() == TicketStatus.PROCESSING || request.status() == TicketStatus.REJECTED)) ||
            (ticket.getStatus() == TicketStatus.PROCESSING && request.status() == TicketStatus.COMPLETED);
        if (!allowed) throw error("INVALID_TRANSITION", "不允许此工单状态变更，终态工单不能重新打开");
        ticket.getHistory().add(history(ticket.getStatus(), request.status(), request.note().trim(), request.operatorId()));
        ticket.setStatus(request.status());
        ticket.setUpdatedAt(LocalDateTime.now());
        tickets.saveAndFlush(ticket);
        return ticketView(ticket);
    }

    Shop checkScope(Scope scope) {
        if (!merchants.existsById(scope.merchantId())) throw notFound();
        Shop shop = shops.findById(scope.shopId()).filter(s -> s.getMerchantId().equals(scope.merchantId())).orElseThrow(CommerceService::notFound);
        customers.findById(scope.customerId()).filter(c -> c.getMerchantId().equals(scope.merchantId()) && c.getShopId().equals(scope.shopId()))
            .orElseThrow(CommerceService::notFound);
        return shop;
    }

    private Merchant lockMerchant(String id) { return merchants.lockById(id).orElseThrow(CommerceService::notFound); }
    private CommerceOrder ownedOrder(Scope scope, String orderId) {
        return orders.findById(orderId).filter(o -> o.getMerchantId().equals(scope.merchantId()) &&
            o.getShopId().equals(scope.shopId()) && o.getCustomerId().equals(scope.customerId())).orElseThrow(CommerceService::notFound);
    }
    private Order orderView(CommerceOrder o, Shop shop) {
        return new Order(o.getId(), o.getOrderNumber(), o.getMerchantId(), o.getShopId(), shop.getName(), shop.getPlatform(), o.getCustomerId(),
            o.getItems().stream().map(i -> new Item(i.getItemId(), i.getProductId(), i.getProductName(), i.getSpecification(), i.getQuantity(), i.getUnitPrice())).toList(),
            o.getItems().stream().mapToInt(CommerceOrder.Item::getQuantity).sum(), o.getActualPaid(), o.getStatus(), o.getOrderedAt());
    }
    private Ticket ticketView(ServiceTicket t) {
        String orderNumber = orders.findById(t.getOrderId()).map(CommerceOrder::getOrderNumber).orElse(null);
        return new Ticket(t.getId(), t.getTicketNumber(), t.getMerchantId(), t.getShopId(), t.getCustomerId(), t.getOrderId(), orderNumber,
            t.getSessionId(), t.getTicketType(), t.getReason(), t.getStatus(), "NOT_SUBMITTED_TO_PLATFORM", t.getCreatedAt(), t.getUpdatedAt(),
            t.getHistory().stream().map(h -> new HandlingRecord(h.getHistoryId(), h.getFromStatus(), h.getToStatus(), h.getNote(), h.getOperatorId(), h.getOccurredAt())).toList());
    }
    private TicketCreated createdResult(ServiceTicket ticket, boolean created, String reuseReason) {
        return new TicketCreated(ticketView(ticket), created, reuseReason, created ?
            "已记录诉求，待商家审核；本系统未执行平台退货或退款" : "已找到之前记录的诉求，请查看工单当前状态；本系统未执行平台退货或退款");
    }
    private static ServiceTicket.History history(TicketStatus from, TicketStatus to, String note, String operator) {
        ServiceTicket.History h = new ServiceTicket.History();
        h.setHistoryId(UUID.randomUUID().toString()); h.setFromStatus(from); h.setToStatus(to);
        h.setNote(note); h.setOperatorId(operator); h.setOccurredAt(LocalDateTime.now()); return h;
    }
    private static LogisticsResult noLogistics(String id, String reason) { return new LogisticsResult(id, false, reason, null, null, null, List.of()); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    static CommerceException notFound() { return error("NOT_FOUND", "当前范围内未找到资源"); }
    static CommerceException error(String code, String message) { return new CommerceException(code, message); }
    private static String hash(String... values) {
        try {
            // 长度前缀避免分隔符碰撞，不依赖默认字符集。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((bytes.length + ":").getBytes(StandardCharsets.UTF_8)); digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
