package com.anjing.module.chat.workbench;

import com.anjing.module.chat.LlmService;
import com.anjing.module.chat.entity.*;
import com.anjing.module.chat.repository.*;
import com.anjing.module.commerce.*;
import com.anjing.module.commerce.CommerceDTO.*;
import com.anjing.module.commerce.CommerceTypes.*;
import com.anjing.module.commerce.CommerceVO.*;
import com.anjing.module.commerce.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Single-merchant demo workbench. Model output is an intent hint, never an identity or write authority. */
@Service @Validated @RequiredArgsConstructor
public class WorkbenchService {
    private static final String MERCHANT = CommerceDemoService.MERCHANT_ID;
    private static final Pattern ORDER_TOKEN = Pattern.compile("(?i)(?:demo-order-[a-z0-9-]+|(?:TB|DY)[0-9]{6,})");
    private static final Pattern LABELLED_ORDER = Pattern.compile("订单(?:编号|号码|号)?[：:\\s]*([A-Za-z0-9][A-Za-z0-9_-]{2,63})(?![A-Za-z0-9_-])");
    private static final Pattern CANCEL = Pattern.compile("(?:取消(?:提交|申请|退货|退款)?|算了|不(?:退货|退款|申请)了|(?:先|暂时)?(?:别|不要|不)提交|暂不提交)[！!。]?");
    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final ChatExecutionRepository executions;
    private final ScopedKnowledgeRepository knowledgeRepository;
    private final CustomerRepository customers;
    private final ShopRepository shops;
    private final MerchantRepository merchants;
    private final CommerceService commerce;
    private final CommerceDemoService demo;
    private final WorkbenchKnowledgeService knowledge;
    private final LlmService llm;
    private final ObjectMapper mapper;
    private final PlatformTransactionManager transactionManager;

    public boolean isWorkbench(String id) {
        return id != null && sessions.findById(id).map(s -> s.getMerchantId()!=null).orElse(false);
    }

    public List<WorkbenchVO.Session> list() {
        return tx(() -> sessions.findByMerchantIdOrderByUpdatedAtDesc(MERCHANT).stream()
            .filter(s -> Boolean.TRUE.equals(s.getDemoData())).map(this::sessionView).toList());
    }

    public WorkbenchVO.Snapshot open(@Valid WorkbenchDTO.Open request) {
        return tx(() -> {
            lockSpace();
            var customer = customers.findById(request.customerId()).filter(c -> c.isDemoData() &&
                MERCHANT.equals(c.getMerchantId()) && request.shopId().equals(c.getShopId())).orElseThrow(WorkbenchService::notFound);
            shops.findById(request.shopId()).filter(s -> s.isDemoData() && MERCHANT.equals(s.getMerchantId()))
                .orElseThrow(WorkbenchService::notFound);
            knowledge.load(false);
            if (!request.forceNew()) {
                var previous = sessions.findByMerchantIdOrderByUpdatedAtDesc(MERCHANT).stream().filter(s ->
                    Boolean.TRUE.equals(s.getDemoData()) && request.shopId().equals(s.getShopId()) &&
                    request.customerId().equals(s.getCustomerId()) && "active".equals(s.getStatus())).findFirst();
                if (previous.isPresent()) return snapshot(previous.get());
            }
            ChatSession s = new ChatSession();
            s.setSessionId(id()); s.setMerchantId(MERCHANT); s.setShopId(request.shopId());
            s.setCustomerId(customer.getId()); s.setUserId(customer.getId()); s.setUserName(customer.getName());
            s.setChannel(shops.findById(request.shopId()).orElseThrow().getPlatform().name());
            s.setReceptionMode("AI"); s.setStatus("active"); s.setDemoData(true); s.setTurnVersion(0L);
            s.setAwaitingConfirmation(false); s = sessions.saveAndFlush(s);
            messages.save(addMessage(s,"system","演示会话已建立。服务工单仅记录本地诉求，不代表平台退货、退款或赔偿成功。",null,List.of(),null));
            return snapshot(s);
        });
    }

    public WorkbenchVO.Snapshot fetch(@Valid WorkbenchDTO.SessionId request) {
        return tx(() -> snapshot(requireSession(request.sessionId())));
    }

    public WorkbenchVO.Snapshot send(@Valid WorkbenchDTO.Send request) {
        Plan plan = tx(() -> prepare(request));
        if (plan == null) return fetch(new WorkbenchDTO.SessionId(request.sessionId()));
        // No database locks or transactions span a model request. Takeover is immediately available.
        Map<String,String> analysis;
        try { analysis = llm.analyzeUserInput(request.content()); }
        catch (RuntimeException ex) { analysis = null; }
        Map<String,String> result = analysis;
        try { return tx(() -> finish(plan, result)); }
        catch (RuntimeException ex) {
            try { recordFailure(plan, ex); }
            catch (RuntimeException recordingError) { ex.addSuppressed(recordingError); }
            throw ex;
        }
    }

    private Plan prepare(WorkbenchDTO.Send request) {
        ChatSession s = lockSession(request.sessionId());
        String messageId = clientId(s.getSessionId(),"customer",request.clientMessageId());
        var existing = messages.findById(messageId);
        if (existing.isPresent()) {
            if (!Objects.equals(existing.get().getContent(),request.content().trim())) throw conflict("同一消息编号的内容发生变化");
            if ("DONE".equals(existing.get().getDeliveryState())) return null;
            if (existing.get().getProcessingVersion()!=null && existing.get().getProcessingVersion()!=version(s))
                throw conflict("原消息的业务上下文已变化，请使用新消息重新确认当前诉求");
        }
        boolean wantsHuman = has(request.content(),"人工","十倍","全额赔偿","额外赔偿","额外补偿") ||
            Pattern.compile("(?:赔偿|赔付|补偿).{0,8}(?:[0-9]+|[一二三四五六七八九十百千万]+)\\s*(?:元|块)?").matcher(request.content()).find();
        if ("AI".equals(s.getReceptionMode()) && !wantsHuman && s.getInFlightMessageId()!=null &&
            s.getInFlightAt()!=null && s.getInFlightAt().isAfter(LocalDateTime.now().minusSeconds(60)))
            throw conflict("上一条消息仍在处理中，请稍后使用原消息编号重试");
        ChatMessage m = existing.orElseGet(() -> {
            ChatMessage created = addMessage(s,"customer",request.content().trim(),request.clientMessageId(),List.of(),null);
            created.setMessageId(messageId); return created;
        });
        m.setDeliveryState("PROCESSING"); m = messages.saveAndFlush(m);
        touch(s);
        if (!"AI".equals(s.getReceptionMode())) {
            m.setDeliveryState("DONE"); return null;
        }
        if (wantsHuman) {
            String value = orderToken(request.content());
            if (value!=null) {
                var owned = commerce.getOrder(new OrderQuery(scope(s),value.toLowerCase(Locale.ROOT).startsWith("demo-order-")?value:null,
                    value.toLowerCase(Locale.ROOT).startsWith("demo-order-")?null:value.toUpperCase(Locale.ROOT),null));
                s.setSelectedOrderId(owned.orders().size()==1?owned.orders().get(0).orderId():null);
                execution(s,"get_order","SUCCESS",owned.message(),s.getSelectedOrderId());
            }
            handoff(s,"顾客要求人工或提出需商家决定的补偿：" + request.content());
            m.setDeliveryState("DONE"); return null;
        }
        s.setTurnVersion(version(s)+1); s.setInFlightMessageId(messageId); s.setInFlightAt(LocalDateTime.now());
        m.setProcessingVersion(version(s));
        return new Plan(s.getSessionId(),messageId,version(s),request.content().trim());
    }

    private WorkbenchVO.Snapshot finish(Plan plan, Map<String,String> analysis) {
        ChatSession s = lockSession(plan.sessionId());
        if (!current(s,plan)) return snapshot(s);
        String text = plan.content();
        boolean modelAvailable = analysis != null && analysis.get("intentCode") != null &&
            Set.of("PRODUCT_DISCOUNT","RETURN_EXCHANGE","SIZE_CONSULT","LOGISTICS_QUERY","GENERAL_QUERY")
                .contains(analysis.get("intentCode"));
        String intent = modelAvailable ? analysis.get("intentCode") : "GENERAL_QUERY";
        var refs = knowledge.search(s.getMerchantId(),s.getShopId(),text);
        s.setKnowledgeIdsJson(json(refs.stream().map(ScopedKnowledge::getId).toList()));
        execution(s,"search_knowledge","SUCCESS","按当前商家与店铺检索，命中 " + refs.size() + " 条",null);
        String prefix = modelAvailable ? "" : "【模型不可用，规则降级】";
        if (CANCEL.matcher(text).matches()) {
            clearPending(s);
            reply(s,prefix+"已取消待确认诉求，没有创建工单。",List.of(),null);
        } else if (ConfirmationText.matches(text)) {
            confirm(s,plan,modelAvailable,prefix);
        } else if (has(text,"工单","进度","刚才的申请","申请怎么样") && !has(text,"我要退","申请退")) {
            var matcher = Pattern.compile("(?i)ST[A-Z0-9]{8,}").matcher(text);
            String number = matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : null;
            var found = commerce.getServiceTicket(new TicketQuery(scope(s),number,number==null?s.getSelectedOrderId():null,null));
            execution(s,"get_service_ticket","SUCCESS","找到 " + found.size() + " 张当前顾客工单",found.isEmpty()?null:found.get(0).ticketId());
            reply(s,prefix+(found.isEmpty()?"当前顾客未找到匹配工单。":found.stream().map(t ->
                "工单 " + t.ticketNumber() + "，状态：" + ticketStatus(t.status()) + "。" +
                (t.history().isEmpty()?"":t.history().get(t.history().size()-1).note())).reduce((a,b)->a+"\n"+b).orElseThrow())+
                " 本系统未执行平台退货或退款。",List.of(),found.isEmpty()?null:found.get(0).ticketId());
        } else {
            String token = orderToken(text);
            boolean selecting = token!=null && text.replace(token,"").matches("[\\s。.!！]*");
            boolean policyIntent = has(text,"几天","期限","规则","政策","运费","条件");
            boolean returning = has(text,"要退货","想退货","要退款","想退款","想退掉","帮我申请退货","帮我申请退款") ||
                has(text,"申请退货","申请退款") && !policyIntent ||
                "RETURN_EXCHANGE".equals(intent) && !selecting && !policyIntent;
            boolean compensation = has(text,"申请补偿","申请赔偿","我要补偿","要求补偿");
            boolean logistics = has(text,"物流","快递","到哪","发货","到货","配送") || "LOGISTICS_QUERY".equals(intent);
            boolean continuing = selecting && s.getPendingAction()!=null;
            boolean policyQuestion = !returning && !compensation && !logistics && token==null && !refs.isEmpty() &&
                has(text,"规则","期限","几天","运费","材质","尺寸","优惠","活动","容量");
            boolean capacity = !returning && !compensation && !logistics && has(text,"容量","多少毫升");
            if (policyQuestion || capacity) clearPending(s);
            boolean business = returning || compensation || logistics || token!=null || has(text,"订单") || s.getPendingAction()!=null;
            if (capacity) {
                answerCapacity(s,token,prefix);
            } else if (business && !policyQuestion) {
                if (returning || compensation) {
                    clearPending(s);
                    s.setPendingAction(compensation?"COMPENSATION":"RETURN");
                    String reason = extractReason(text);
                    if (reason!=null) s.setPendingReason(reason);
                } else if (logistics && !continuing) {
                    clearPending(s); s.setPendingAction("LOGISTICS");
                } else if (s.getPendingAction()!=null && !"LOGISTICS".equals(s.getPendingAction()) && token==null && !logistics) {
                    String reason = extractReason(text);
                    s.setPendingReason(reason==null?text:reason); s.setAwaitingConfirmation(false);
                }
                if (s.getPendingReason()!=null && s.getPendingReason().length()>1000) {
                    s.setPendingReason(null); s.setAwaitingConfirmation(false);
                    reply(s,"请将诉求原因控制在1000字以内，补齐后再确认提交。",List.of(),null);
                } else handleOrder(s,text,token,modelAvailable,prefix);
            } else {
                // Return exact retrieved evidence. A model cannot invent business status or policy commitments.
                reply(s,prefix+(refs.isEmpty()?"当前店铺缺少足够的知识依据，请补充问题或联系人工客服。":
                    refs.stream().map(k -> k.getTitle()+"："+k.getContent()).reduce((a,b)->a+"\n"+b).orElseThrow()),List.of(),null);
            }
        }
        messages.findById(plan.messageId()).orElseThrow().setDeliveryState("DONE");
        s.setInFlightMessageId(null); s.setInFlightAt(null); s.setConfirmationMessageId(null); touch(s);
        sessions.flush(); return snapshot(s);
    }

    private void answerCapacity(ChatSession s,String token,String prefix) {
        String explicitId = token!=null && token.toLowerCase(Locale.ROOT).startsWith("demo-order-") ? token : null;
        var found = commerce.getOrder(new OrderQuery(scope(s),explicitId!=null?explicitId:token==null?s.getSelectedOrderId():null,
            token!=null && explicitId==null?token.toUpperCase(Locale.ROOT):null,
            token==null && s.getSelectedOrderId()==null?"杯":null));
        execution(s,"get_order","SUCCESS",found.message(),found.orders().size()==1?found.orders().get(0).orderId():null);
        if (found.orders().isEmpty()) {
            if (token!=null) s.setSelectedOrderId(null);
            reply(s,prefix+"当前店铺和顾客下未找到匹配订单，请先提供本人保温杯订单或规格。",List.of(),null);
            return;
        }
        boolean single = found.orders().size()==1;
        if (single) s.setSelectedOrderId(found.orders().get(0).orderId());
        String details = found.orders().stream().map(o -> "订单 "+o.orderNumber()+"："+o.items().stream()
            .filter(i -> i.productName().contains("杯"))
            .map(i -> i.productName()+"，规格："+Objects.toString(i.specification(),"未提供，请联系商家确认"))
            .reduce((a,b)->a+"；"+b).orElse("该订单没有保温杯，请核对订单或提供规格"))
            .reduce((a,b)->a+"\n"+b).orElseThrow();
        reply(s,prefix+(single?"":"找到多笔杯子订单，请先选择具体订单：\n")+details,
            found.orders().stream().map(Order::orderId).toList(),null);
    }

    private void handleOrder(ChatSession s,String text,String token,boolean modelAvailable,String prefix) {
        String explicitId = token!=null && token.toLowerCase(Locale.ROOT).startsWith("demo-order-") ? token : null;
        String explicitNumber = token!=null && explicitId==null ? token.toUpperCase(Locale.ROOT) : null;
        String clue = token==null ? productClue(text) : null;
        String selected = token==null && clue==null ? s.getSelectedOrderId() : null;
        OrderMatches found = commerce.getOrder(new OrderQuery(scope(s),explicitId!=null?explicitId:selected,explicitNumber,clue));
        execution(s,"get_order","SUCCESS",found.message(),found.orders().size()==1?found.orders().get(0).orderId():null);
        if (found.orders().isEmpty()) {
            s.setSelectedOrderId(null); s.setAwaitingConfirmation(false);
            reply(s,prefix+"当前店铺和顾客下未找到匹配订单，请核对本人订单编号。",List.of(),null); return;
        }
        if (found.orders().size()>1) {
            s.setSelectedOrderId(null); s.setAwaitingConfirmation(false);
            reply(s,prefix+"找到多笔匹配订单，请先选择具体订单：\n"+
                found.orders().stream().map(o -> o.orderNumber()+"（"+o.items().stream().map(Item::productName).reduce((a,b)->a+"、"+b).orElse("")+"）")
                    .reduce((a,b)->a+"\n"+b).orElseThrow(),found.orders().stream().map(Order::orderId).toList(),null); return;
        }
        Order order = found.orders().get(0);
        s.setSelectedOrderId(order.orderId());
        if ("LOGISTICS".equals(s.getPendingAction())) {
            var l = commerce.getLogistics(new LogisticsQuery(scope(s),order.orderId()));
            execution(s,"get_logistics","SUCCESS",l.available()?l.status():l.unavailableReason(),order.orderId());
            reply(s,prefix+"订单 "+order.orderNumber()+"："+(l.available()?l.carrier()+"，运单 "+l.trackingNumber()+"，"+l.status()+"。\n"+
                l.nodes().stream().map(n -> iso(n.occurredAt())+" "+n.description()).reduce((a,b)->a+"\n"+b).orElse("")+
                "\n未提供预计送达时间，不作到货承诺。":l.unavailableReason()),List.of(order.orderId()),null);
            s.setPendingAction(null);
        } else if (s.getPendingAction()!=null) {
            if (!modelAvailable) { handoff(s,"模型不可用，写入诉求需商家处理；订单 "+order.orderNumber()); return; }
            if (order.status()==OrderStatus.CANCELLED) {
                clearPending(s); reply(s,"订单已取消，请联系商家核实需要处理的诉求。",List.of(order.orderId()),null); return;
            }
            if (s.getPendingReason()==null || s.getPendingReason().isBlank()) {
                reply(s,"已选订单 "+order.orderNumber()+"。请补充具体原因，补齐后再确认提交。",List.of(order.orderId()),null); return;
            }
            s.setAwaitingConfirmation(true);
            reply(s,"请确认：订单 "+order.orderNumber()+"；诉求："+(s.getPendingAction().equals("RETURN")?"退货":"异常/补偿")+
                "；原因："+s.getPendingReason()+"。回复“确认提交”后仅创建本地工单，待商家审核，不执行平台退款。",List.of(order.orderId()),null);
        } else reply(s,prefix+"已选择订单 "+order.orderNumber()+"。请说明要查询物流、退货还是查看工单。",List.of(order.orderId()),null);
    }

    private void confirm(ChatSession s,Plan plan,boolean modelAvailable,String prefix) {
        if (!Boolean.TRUE.equals(s.getAwaitingConfirmation()) || s.getSelectedOrderId()==null ||
            s.getPendingReason()==null || !Set.of("RETURN","COMPENSATION").contains(Objects.toString(s.getPendingAction(),""))) {
            reply(s,prefix+"当前没有可提交的待确认诉求，请先明确订单、诉求和原因。",List.of(),null); return;
        }
        if (!modelAvailable) { handoff(s,"模型不可用，待确认诉求交商家审核"); return; }
        s.setConfirmationMessageId(plan.messageId()); sessions.flush();
        var created = commerce.createAgentTicket(new CreateTicket(scope(s),s.getSessionId(),plan.messageId(),
            s.getSelectedOrderId(),"RETURN".equals(s.getPendingAction())?TicketType.RETURN_REQUEST:TicketType.EXCEPTION_COMPENSATION,s.getPendingReason(),true));
        execution(s,"create_service_ticket","SUCCESS",created.message(),created.ticket().ticketId());
        reply(s,"工单 "+created.ticket().ticketNumber()+"："+created.message(),List.of(s.getSelectedOrderId()),created.ticket().ticketId());
        clearPending(s);
    }

    public WorkbenchVO.Snapshot human(@Valid WorkbenchDTO.Send request) {
        return tx(() -> {
            ChatSession s = lockSession(request.sessionId());
            String id = clientId(s.getSessionId(),"human",request.clientMessageId());
            var existing = messages.findById(id);
            if (existing.isPresent()) {
                if (!request.content().trim().equals(existing.get().getContent())) throw conflict("同一人工消息编号的内容发生变化");
                return snapshot(s);
            }
            if (!"HUMAN".equals(s.getReceptionMode())) throw conflict("请先接管会话，再发送人工消息");
            ChatMessage m = addMessage(s,"human",request.content().trim(),request.clientMessageId(),List.of(),null);
            m.setMessageId(id); m.setDeliveryState("DONE"); messages.save(m); touch(s);
            return snapshot(s);
        });
    }

    public WorkbenchVO.Snapshot reception(@Valid WorkbenchDTO.Reception request) {
        return tx(() -> {
            ChatSession s = lockSession(request.sessionId());
            if (!request.receptionMode().equals(s.getReceptionMode())) {
                // Capture pending details before invalidation, and keep the existing transfer brief when accepting it.
                if ("HUMAN".equals(request.receptionMode()) && (!"WAITING_HUMAN".equals(s.getReceptionMode()) ||
                    s.getHandoffSummary()==null || s.getHandoffSummary().isBlank()))
                    s.setHandoffSummary(summary(s,"商家主动接管"));
                invalidate(s); clearPending(s);
                s.setReceptionMode(request.receptionMode());
                messages.save(addMessage(s,"system","HUMAN".equals(request.receptionMode())?"商家已接管会话。":"已交回 AI，由下一条顾客消息触发处理。",null,List.of(),null));
                execution(s,"handoff_to_human","SUCCESS","接待模式切换为 "+request.receptionMode(),null); touch(s);
            }
            return snapshot(s);
        });
    }

    public void reset(@Valid WorkbenchDTO.Reset request) {
        if (!request.confirmed()) throw conflict("请明确确认演示重置");
        tx(() -> {
            lockSpace();
            for (ChatSession item : sessions.findByMerchantIdOrderByUpdatedAtDesc(MERCHANT)) {
                if (!Boolean.TRUE.equals(item.getDemoData())) continue;
                ChatSession s = sessions.lockById(item.getSessionId()).orElseThrow();
                executions.deleteBySessionId(s.getSessionId()); messages.deleteBySessionId(s.getSessionId()); sessions.delete(s);
            }
            sessions.flush(); demo.restore(true); knowledge.load(true); return null;
        });
    }

    private WorkbenchVO.Snapshot snapshot(ChatSession s) {
        var allOrders = commerce.getOrder(new OrderQuery(scope(s),null,null,null)).orders();
        var orderViews = allOrders.stream().map(o -> map("orderId",o.orderId(),"orderNumber",o.orderNumber(),"shopId",o.shopId(),
            "customerId",o.customerId(),"items",o.items().stream().map(i -> map("name",i.productName(),"quantity",i.quantity(),"specification",i.specification())).toList(),
            "paidAmount",o.actualPaid(),"status",o.status(),"createdAt",iso(o.orderedAt()))).toList();
        var logisticViews = allOrders.stream().map(o -> commerce.getLogistics(new LogisticsQuery(scope(s),o.orderId())))
            .map(l -> map("orderId",l.orderId(),"carrier",l.carrier(),"trackingNumber",l.trackingNumber(),
                "status",l.available()?l.status():l.unavailableReason(),"nodes",l.nodes().stream()
                    .map(n -> Map.of("time",iso(n.occurredAt()),"description",n.description())).toList())).toList();
        var ticketViews = commerce.getServiceTicket(new TicketQuery(scope(s),null,null,null)).stream().map(t ->
            map("ticketId",t.ticketId(),"ticketNumber",t.ticketNumber(),"orderNumber",t.orderNumber(),"sessionId",t.sessionId(),
                "orderId",t.orderId(),"shopId",t.shopId(),"customerId",t.customerId(),"type",t.ticketType()==TicketType.RETURN_REQUEST?"RETURN":"COMPENSATION","reason",t.reason(),
                "status",t.status(),"processingNote",t.history().isEmpty()?"":t.history().get(t.history().size()-1).note(),
                "createdAt",iso(t.createdAt()),"updatedAt",iso(t.updatedAt()),"history",t.history())).toList();
        Set<String> referenceIds = new HashSet<>(strings(s.getKnowledgeIdsJson()));
        var references = knowledge.applicable(s.getMerchantId(),s.getShopId()).stream().filter(k -> referenceIds.contains(k.getId()))
            .map(k -> new WorkbenchVO.Knowledge(k.getId(),k.getTitle(),k.getSource(),k.getContent(),k.getScope())).toList();
        return new WorkbenchVO.Snapshot(sessionView(s),messageViews(s),orderViews,logisticViews,ticketViews,references,
            executions.findBySessionIdOrderByCreatedAtAscIdAsc(s.getSessionId()).stream().map(e -> new WorkbenchVO.Execution(
                e.getId(),e.getAction(),e.getStatus(),e.getSummary(),e.getRelatedId(),iso(e.getCreatedAt()))).toList(),
            s.getSelectedOrderId(),Boolean.TRUE.equals(s.getAwaitingConfirmation()));
    }

    private WorkbenchVO.Session sessionView(ChatSession s) {
        var shop = shops.findById(s.getShopId()).orElseThrow(WorkbenchService::notFound);
        var customer = customers.findById(s.getCustomerId()).filter(c -> c.getShopId().equals(s.getShopId()) &&
            MERCHANT.equals(c.getMerchantId())).orElseThrow(WorkbenchService::notFound);
        var history = history(s);
        return new WorkbenchVO.Session(s.getSessionId(),s.getShopId(),shop.getName(),shop.getPlatform().name(),s.getCustomerId(),
            customer.getName(),s.getReceptionMode(),s.getStatus(),history.isEmpty()?"":history.get(history.size()-1).getContent(),
            iso(s.getUpdatedAt()),s.getHandoffSummary());
    }

    private List<WorkbenchVO.Message> messageViews(ChatSession s) {
        return history(s).stream().map(m -> new WorkbenchVO.Message(m.getMessageId(),m.getSessionId(),m.getClientMessageId(),
            m.getRole(),m.getContent(),iso(m.getCreatedAt()),strings(m.getOrderIdsJson()),m.getTicketId())).toList();
    }

    private ChatSession requireSession(String id) {
        return sessions.findById(id).filter(s -> MERCHANT.equals(s.getMerchantId()) && Boolean.TRUE.equals(s.getDemoData()))
            .orElseThrow(WorkbenchService::notFound);
    }
    private ChatSession lockSession(String id) {
        lockSpace();
        ChatSession s = sessions.lockById(id).filter(item -> MERCHANT.equals(item.getMerchantId()) && Boolean.TRUE.equals(item.getDemoData()))
            .orElseThrow(WorkbenchService::notFound);
        if (!"active".equals(s.getStatus())) throw conflict("会话已关闭，请重新打开会话");
        return s;
    }
    private void lockSpace() { merchants.lockById(MERCHANT).filter(m -> m.isDemoData()).orElseThrow(WorkbenchService::notFound); }
    private Scope scope(ChatSession s) { return new Scope(s.getMerchantId(),s.getShopId(),s.getCustomerId()); }
    private List<ChatMessage> history(ChatSession s) {
        return messages.findBySessionIdOrderByCreatedAtAsc(s.getSessionId()).stream()
            .sorted(Comparator.comparing(ChatMessage::getMessageSequence,Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ChatMessage::getCreatedAt).thenComparing(ChatMessage::getMessageId)).toList();
    }
    private ChatMessage addMessage(ChatSession s,String role,String content,String client,List<String> orders,String ticket) {
        ChatMessage m = new ChatMessage(); m.setMessageId(id()); m.setSessionId(s.getSessionId()); m.setRole(role);
        m.setContent(content); m.setClientMessageId(client); m.setOrderIdsJson(json(orders)); m.setTicketId(ticket);
        m.setMessageSequence(messages.countBySessionId(s.getSessionId())+1);
        m.setCreatedAt(LocalDateTime.now()); return m;
    }
    private void reply(ChatSession s,String text,List<String> orders,String ticket) {
        messages.save(addMessage(s,"ai",text,null,orders,ticket));
    }
    private void execution(ChatSession s,String action,String status,String summary,String related) {
        ChatExecution e = new ChatExecution(); e.setId(id()); e.setSessionId(s.getSessionId()); e.setAction(action);
        e.setStatus(status); e.setSummary(summary); e.setRelatedId(related); e.setCreatedAt(LocalDateTime.now()); executions.save(e);
    }
    private void handoff(ChatSession s,String reason) {
        s.setHandoffSummary(summary(s,reason)); invalidate(s); clearPending(s); s.setReceptionMode("WAITING_HUMAN");
        reply(s,"已转为等待人工，商家接管后继续处理。已完成的本地操作可在业务资料中查看。",List.of(),null);
        execution(s,"handoff_to_human","SUCCESS",s.getHandoffSummary(),s.getSelectedOrderId());
    }
    private String summary(ChatSession s,String reason) {
        String orderDetails = "未确认", logisticsDetails = "未查询";
        if (s.getSelectedOrderId()!=null) {
            var owned = commerce.getOrder(new OrderQuery(scope(s),s.getSelectedOrderId(),null,null));
            execution(s,"get_order","SUCCESS",owned.message(),s.getSelectedOrderId());
            if (owned.orders().size()==1) {
                var order = owned.orders().get(0);
                orderDetails = order.orderNumber()+" / "+order.orderId();
                var logistics = commerce.getLogistics(new LogisticsQuery(scope(s),order.orderId()));
                execution(s,"get_logistics","SUCCESS",logistics.available()?logistics.status():logistics.unavailableReason(),order.orderId());
                var latest = logistics.nodes().stream().max(Comparator.comparing(LogisticsNode::occurredAt));
                logisticsDetails = logistics.available()?logistics.status()+"；末节点："+
                    latest.map(n -> iso(n.occurredAt())+" "+n.description()).orElse("未提供") : logistics.unavailableReason();
            }
        }
        var tickets = commerce.getServiceTicket(new TicketQuery(scope(s),null,null,null));
        execution(s,"get_service_ticket","SUCCESS","找到 "+tickets.size()+" 张当前顾客工单",null);
        String queries = executions.findBySessionIdOrderByCreatedAtAscIdAsc(s.getSessionId()).stream()
            .filter(e -> "SUCCESS".equals(e.getStatus()) && Set.of("get_order","get_logistics","get_service_ticket","search_knowledge").contains(e.getAction()))
            .map(ChatExecution::getAction).distinct().reduce((a,b)->a+"、"+b).orElse("无");
        String latestCustomerMessage = history(s).stream().filter(m -> "customer".equals(m.getRole()))
            .reduce((previous,current) -> current).map(ChatMessage::getContent).orElse("未提供");
        return "转交原因："+reason+"；订单："+orderDetails+"；物流："+logisticsDetails+"；已完成查询："+queries+
            "；诉求："+Objects.toString(s.getPendingAction(),"待补充")+"；原因："+Objects.toString(s.getPendingReason(),"待补充")+
            "；顾客最近消息："+latestCustomerMessage+
            "；已有工单："+tickets.stream().map(t -> t.ticketNumber()+"/"+t.status()).reduce((a,b)->a+"、"+b).orElse("无")+
            "；待决定：请商家核实并回复，不代表已执行平台退款。";
    }
    private void invalidate(ChatSession s) {
        if (s.getInFlightMessageId()!=null) messages.findById(s.getInFlightMessageId()).ifPresent(m -> m.setDeliveryState("DONE"));
        s.setTurnVersion(version(s)+1); s.setInFlightMessageId(null); s.setInFlightAt(null); s.setConfirmationMessageId(null);
    }
    private void clearPending(ChatSession s) {
        s.setPendingAction(null); s.setPendingReason(null); s.setAwaitingConfirmation(false); s.setConfirmationMessageId(null);
    }
    private boolean current(ChatSession s,Plan p) {
        return "AI".equals(s.getReceptionMode()) && version(s)==p.version() && Objects.equals(s.getInFlightMessageId(),p.messageId());
    }
    private void recordFailure(Plan plan,RuntimeException error) {
        tx(() -> {
            if (!sessions.existsById(plan.sessionId())) return null;
            ChatSession s = lockSession(plan.sessionId());
            if (current(s,plan)) {
                messages.findById(plan.messageId()).ifPresent(m -> m.setDeliveryState("FAILED"));
                s.setInFlightMessageId(null); s.setInFlightAt(null); s.setConfirmationMessageId(null);
                execution(s,"process_customer_message","FAILED",error instanceof CommerceException c?c.getMessage():
                    "业务写入未成功，请使用原消息编号重试或联系商家核实",null); touch(s);
            }
            return null;
        });
    }
    private void touch(ChatSession s) { s.setUpdatedAt(LocalDateTime.now()); }
    private long version(ChatSession s) { return s.getTurnVersion()==null?0:s.getTurnVersion(); }
    private String extractReason(String text) {
        var matcher = Pattern.compile("(?:原因[是为：:]?|因为)[\\s：:，,]*(.+)").matcher(text);
        return matcher.find()?matcher.group(1).trim():null;
    }
    private String productClue(String text) {
        if (has(text,"杯")) return "杯";
        if (has(text,"收纳盒")) return "收纳盒";
        if (has(text,"毛巾")) return "毛巾";
        return null;
    }
    private String orderToken(String text) {
        // Prefer a labelled identifier even if it is unknown, rather than silently listing unrelated owned orders.
        var labelled = LABELLED_ORDER.matcher(text);
        if (labelled.find()) return labelled.group(1);
        var known = ORDER_TOKEN.matcher(text);
        return known.find()?known.group():null;
    }
    private static boolean has(String text,String... words) { return Arrays.stream(words).anyMatch(text::contains); }
    private static String ticketStatus(TicketStatus status) { return switch(status) {
        case PENDING_REVIEW -> "待审核"; case PROCESSING -> "处理中"; case COMPLETED -> "已完成"; case REJECTED -> "已拒绝";
    }; }
    private static String iso(LocalDateTime time) { return time==null?null:time.atOffset(ZoneOffset.ofHours(8)).toString(); }
    private static String id() { return UUID.randomUUID().toString(); }
    private String clientId(String session,String role,String client) {
        return UUID.nameUUIDFromBytes(json(List.of(session,role,client)).getBytes(StandardCharsets.UTF_8)).toString();
    }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch(Exception ex) { throw new IllegalStateException(ex); } }
    private List<String> strings(String value) {
        if (value==null) return List.of();
        try { return mapper.readValue(value,new TypeReference<List<String>>(){}); } catch(Exception ex) { throw new IllegalStateException(ex); }
    }
    private Map<String,Object> map(Object... values) {
        Map<String,Object> result = new LinkedHashMap<>();
        for(int i=0;i<values.length;i+=2) result.put((String)values[i],values[i+1]);
        return result;
    }
    private <T> T tx(Supplier<T> work) { return new TransactionTemplate(transactionManager).execute(status -> work.get()); }
    private static CommerceException notFound() { return new CommerceException("NOT_FOUND","演示会话或身份不存在，请重新加载演示数据和页面"); }
    private static CommerceException conflict(String message) { return new CommerceException("IDEMPOTENCY_CONFLICT",message); }
    private record Plan(String sessionId,String messageId,long version,String content) {}
}
