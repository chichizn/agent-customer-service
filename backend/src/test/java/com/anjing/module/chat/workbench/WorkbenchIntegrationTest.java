package com.anjing.module.chat.workbench;

import com.anjing.module.chat.LlmService;
import com.anjing.module.chat.ChatController;
import com.anjing.module.chat.ChatService;
import com.anjing.module.chat.entity.*;
import com.anjing.module.chat.repository.*;
import com.anjing.module.commerce.*;
import com.anjing.module.demo.DemoBusinessTime;
import com.anjing.module.commerce.CommerceDTO.*;
import com.anjing.module.commerce.CommerceTypes.*;
import com.anjing.module.commerce.entity.Merchant;
import com.anjing.module.commerce.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DataJpaTest(showSql=false,properties={
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.show_sql=false"
})
@ContextConfiguration(classes=WorkbenchIntegrationTest.Config.class)
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class WorkbenchIntegrationTest {
    @Configuration(proxyBeanMethods=false)
    @EntityScan(basePackageClasses={ChatSession.class,Merchant.class})
    @EnableJpaRepositories(basePackageClasses={ChatSessionRepository.class,MerchantRepository.class})
    @Import({CommerceService.class,CommerceDemoService.class,WorkbenchService.class,
        WorkbenchKnowledgeService.class,SessionTicketGuard.class,DemoBusinessTime.class,MerchantTicketService.class})
    static class Config {
        @Bean ObjectMapper mapper() { return new ObjectMapper().registerModule(new JavaTimeModule()); }
        @Bean LlmService llm() { return mock(LlmService.class); }
        @Bean static MethodValidationPostProcessor validation() { return new MethodValidationPostProcessor(); }
    }
    @Autowired WorkbenchService service;
    @Autowired CommerceService commerce;
    @Autowired MerchantTicketService ticketWorkflow;
    @Autowired CommerceDemoService demo;
    @Autowired LlmService llm;
    @Autowired ChatSessionRepository sessions;
    @Autowired ChatMessageRepository messages;
    @Autowired ChatExecutionRepository executions;
    @Autowired ScopedKnowledgeRepository knowledge;
    @Autowired WorkbenchKnowledgeService knowledgeService;
    @Autowired ServiceTicketRepository tickets;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired DataSource dataSource;
    private MockMvc mvc;
    private ChatService originalChat;

    @BeforeEach void setup() {
        reset(llm);
        when(llm.analyzeUserInput(anyString(),anyList(),anyList())).thenReturn(Map.of("intentCode","GENERAL_QUERY"));
        demo.load(); service.reset(new WorkbenchDTO.Reset(true));
        originalChat = mock(ChatService.class);
        mvc = MockMvcBuilders.standaloneSetup(new WorkbenchController(service),new ChatController(originalChat,service))
            .setControllerAdvice(new CommerceExceptionHandler()).build();
    }
    private WorkbenchVO.Snapshot open(String suffix) {
        return service.open(new WorkbenchDTO.Open("demo-shop-"+suffix,"demo-customer-"+suffix+"-1",false));
    }
    private WorkbenchVO.Snapshot send(String session,String client,String content) {
        return service.send(new WorkbenchDTO.Send(session,client,content));
    }
    private WorkbenchVO.Snapshot fetch(String session) { return service.fetch(new WorkbenchDTO.SessionId(session)); }

    @ParameterizedTest
    @CsvSource(delimiter='|', value={
        "你好，在吗？|我在", "保温杯怎么清洗？|中性清洁剂", "收纳盒怎么清洗？|软布",
        "毛巾第一次用要怎么洗？|洗标", "支持开发票吗？|税号", "可以用什么付款？|支付",
        "有没有货？|实时库存", "保温杯多少钱？|结算页", "怎么下单？|收货地址",
        "优惠券怎么用？|有效期", "包邮吗？|包邮范围", "什么时候发货？|发货时间",
        "发什么快递？|承运快递", "怎么修改地址？|修改收货地址", "怎么取消订单？|取消订单",
        "怎么退货？|准备凭证", "退款多久到账？|支付渠道", "退货地址在哪？|退货地址",
        "杯盖漏水怎么办？|清晰照片", "漏发了怎么办？|外包装", "工单是什么？|只记录诉求",
        "人工客服几点上班？|值班时间", "不用转人工|常见商品", "谢谢|不客气"
    })
    void commonQuestionsAreAnsweredByRulesWithoutHandoffOrWrites(String question,String expected) {
        when(llm.analyzeUserInput(anyString(),anyList(),anyList())).thenReturn(null);
        for (String shop : List.of("tb","dy")) {
            var result = send(open(shop).session().sessionId(),UUID.randomUUID().toString(),question);
            assertThat(last(result)).as(question).contains(expected);
            assertThat(result.session().receptionMode()).isEqualTo("AI");
            assertThat(result.awaitingConfirmation()).isFalse();
            assertThat(result.knowledge()).isNotEmpty();
            assertThat(result.analysis().analysisSource()).isEqualTo("RULE");
            assertThat(result.analysis().replySource()).isEqualTo("RULE_KNOWLEDGE");
            assertThat(result.executions()).noneSatisfy(e -> assertThat(e.action()).isIn("create_service_ticket","handoff_to_human"));
        }
        assertThat(tickets.count()).isZero();
    }

    @Test void modelIntentCannotTurnOrdinaryQuestionsIntoApplications() {
        when(llm.analyzeUserInput(anyString(),anyList(),anyList())).thenReturn(Map.of("intentCode","RETURN_EXCHANGE"));
        String id = open("dy").session().sessionId();
        for (String question : List.of("你好", "可以退吗", "怎么退货", "人工客服几点上班", "支持开票吗")) {
            var result = send(id,UUID.randomUUID().toString(),question);
            assertThat(result.session().receptionMode()).isEqualTo("AI");
            assertThat(result.awaitingConfirmation()).isFalse();
            assertThat(result.executions()).noneSatisfy(e -> assertThat(e.action()).isEqualTo("get_order"));
        }
        assertThat(last(send(id,"no-application","确认提交"))).contains("没有可提交");
    }

    @Test void semanticModelSelectionHasPriorityAndIsPersistedWithScopedHistory() {
        when(llm.analyzeUserInput(eq("劳驾能帮忙解答吗"),anyList(),anyList())).thenReturn(Map.of(
            "intentCode","GENERAL_QUERY", "scene","通用咨询", "emotion","中性",
            "knowledgeIds","[\"demo-knowledge-greeting\"]"));
        String id = open("tb").session().sessionId();
        send(id,"previous-product","保温杯有什么颜色");
        var result = send(id,"semantic-greeting","劳驾能帮忙解答吗");
        assertThat(last(result)).contains("你好，我在");
        assertThat(result.analysis().replySource()).isEqualTo("RULE_KNOWLEDGE");
        assertThat(fetch(id).analysis()).isEqualTo(result.analysis());
        verify(llm).analyzeUserInput(eq("劳驾能帮忙解答吗"),argThat(rows -> rows.stream()
            .noneMatch(row -> row.get("id").startsWith("demo-knowledge-dy-"))),
            argThat(rows -> rows.stream().anyMatch(row -> row.get("content").contains("保温杯有什么颜色"))));
    }

    @Test void generatedReplyUsesModelWordingWithoutKeywordHitAndPersistsReferences() {
        String question = "日常怎样打理这个杯子呀";
        when(llm.generateWorkbenchReply(eq(question),anyList(),anyList())).thenReturn(new LlmService.WorkbenchReply(
            "可以先拆开杯盖，用中性清洁剂轻柔清洗，再充分晾干。",List.of("demo-knowledge-cup-clean"),"GROUNDED"));
        var result = send(open("tb").session().sessionId(),"generated-1",question);
        assertThat(last(result)).startsWith("可以先拆开杯盖");
        assertThat(result.analysis().replySource()).isEqualTo("MODEL_GENERATED");
        assertThat(result.knowledge()).extracting(WorkbenchVO.Knowledge::knowledgeId).containsExactly("demo-knowledge-cup-clean");
        assertThat(fetch(result.session().sessionId()).analysis()).isEqualTo(result.analysis());
        send(result.session().sessionId(),"generated-1",question);
        verify(llm,times(1)).generateWorkbenchReply(eq(question),anyList(),anyList());
    }

    @Test void modelCanAnswerWithoutKnowledgeMatchEvenWhenAnalysisFails() {
        when(llm.analyzeUserInput(anyString(),anyList(),anyList())).thenReturn(null);
        when(llm.generateWorkbenchReply(anyString(),anyList(),anyList())).thenReturn(new LlmService.WorkbenchReply(
            "我理解你想先比较一下，可以告诉我平时的使用场景吗？",List.of(),"GENERAL"));
        var result = send(open("tb").session().sessionId(),"free-1","我有点纠结，陪我选选吧");
        assertThat(last(result)).contains("使用场景");
        assertThat(result.analysis().replySource()).isEqualTo("MODEL_GENERAL");
        assertThat(result.analysis().analysisSource()).isEqualTo("RULE");
        assertThat(result.knowledge()).isEmpty();
        assertThat(tickets.count()).isZero();
    }

    @Test void generationFailureAndFabricatedExecutionClaimsUseRules() {
        String id = open("tb").session().sessionId();
        when(llm.generateWorkbenchReply(anyString(),anyList(),anyList())).thenThrow(new IllegalStateException("gateway failed"));
        var result = send(id,"generation-fail","保温杯怎么清洗");
        assertThat(last(result)).contains("中性清洁剂");
        assertThat(result.analysis().replySource()).isEqualTo("RULE_KNOWLEDGE");
        doReturn(new LlmService.WorkbenchReply("已经为您退款，退款已到账。",List.of(),"GENERAL"))
            .when(llm).generateWorkbenchReply(anyString(),anyList(),anyList());
        result = send(id,"generation-claim","你好");
        assertThat(last(result)).contains("我在").doesNotContain("已到账");
        assertThat(result.executions()).anyMatch(e -> e.action().equals("generate_reply") && e.status().equals("FAILED"));
        assertThat(tickets.count()).isZero();
    }

    @Test void generationRunsOutsideTransactionAndTakeoverDropsLateReply() throws Exception {
        String id = open("tb").session().sessionId();
        CountDownLatch entered = new CountDownLatch(1),release = new CountDownLatch(1);
        when(llm.generateWorkbenchReply(anyString(),anyList(),anyList())).thenAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            entered.countDown(); release.await(10,TimeUnit.SECONDS);
            return new LlmService.WorkbenchReply("迟到的模型回复",List.of(),"GENERAL");
        });
        var pool = Executors.newSingleThreadExecutor();
        try {
            var future = pool.submit(() -> send(id,"generation-takeover","你好"));
            assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
            var taken = service.reception(new WorkbenchDTO.Reception(id,"HUMAN"));
            release.countDown();
            var result = future.get(10,TimeUnit.SECONDS);
            assertThat(result.messages()).hasSize(taken.messages().size()).noneMatch(m -> m.content().contains("迟到"));
            assertThat(result.session().receptionMode()).isEqualTo("HUMAN");
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    @Test void expiredOrChangedEvidenceDuringGenerationFallsBack() {
        when(llm.generateWorkbenchReply(anyString(),anyList(),anyList())).thenAnswer(invocation -> {
            var activity = knowledge.findById("demo-knowledge-tb-activity").orElseThrow();
            activity.setEndsAt(java.time.LocalDateTime.parse("2026-10-17T00:00:00")); knowledge.save(activity);
            return new LlmService.WorkbenchReply("收纳盒有9折优惠",List.of("demo-knowledge-tb-activity"),"GROUNDED");
        });
        var result = send(open("tb").session().sessionId(),"changed-evidence","收纳盒有什么优惠");
        assertThat(last(result)).doesNotContain("9折");
        assertThat(result.analysis().replySource()).isIn("RULE_KNOWLEDGE","CLARIFICATION");
    }

    @Test void foreignGeneratedReferencesAreRejectedBeforePublication() {
        when(llm.generateWorkbenchReply(anyString(),anyList(),anyList())).thenReturn(new LlmService.WorkbenchReply(
            "本店7天可退",List.of("demo-knowledge-dy-return"),"GROUNDED"));
        var result = send(open("tb").session().sessionId(),"foreign-generated","退货期限几天");
        assertThat(last(result)).contains("15天").doesNotContain("7天");
        assertThat(result.analysis().replySource()).isEqualTo("RULE_KNOWLEDGE");
    }

    @Test void resetDuringGenerationCannotRecreateSessionOrPublishLateAnswer() throws Exception {
        String id = open("tb").session().sessionId();
        CountDownLatch entered = new CountDownLatch(1),release = new CountDownLatch(1);
        when(llm.generateWorkbenchReply(anyString(),anyList(),anyList())).thenAnswer(invocation -> {
            entered.countDown(); release.await(10,TimeUnit.SECONDS);
            return new LlmService.WorkbenchReply("迟到回复",List.of(),"GENERAL");
        });
        var pool = Executors.newSingleThreadExecutor();
        try {
            var future = pool.submit(() -> send(id,"generation-reset","你好"));
            assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
            service.reset(new WorkbenchDTO.Reset(true)); release.countDown();
            assertThatThrownBy(() -> future.get(10,TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(CommerceException.class);
            assertThat(sessions.existsById(id)).isFalse();
            assertThat(tickets.count()).isZero();
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    @Test void businessToolsNeverUseGeneratedSuccessWording() {
        String id = open("tb").session().sessionId();
        send(id,"business-real","查询 TB202610100001 物流");
        send(id,"business-return","想退货 TB202610100001，原因是不喜欢");
        send(id,"business-confirm","确认提交");
        verify(llm,never()).generateWorkbenchReply(anyString(),anyList(),anyList());
    }

    @Test void rejectsForeignAndExpiredModelEvidenceAndUsesLocalRules() {
        when(llm.analyzeUserInput(anyString(),anyList(),anyList())).thenReturn(Map.of(
            "intentCode","GENERAL_QUERY","knowledgeIds","[\"demo-knowledge-dy-return\"]"));
        var result = send(open("tb").session().sessionId(),"bad-model-scope","退货期限几天");
        assertThat(last(result)).contains("15天").doesNotContain("7天");
        assertThat(result.analysis().replySource()).isEqualTo("RULE_KNOWLEDGE");
        var activity = knowledge.findById("demo-knowledge-tb-activity").orElseThrow();
        activity.setEndsAt(java.time.LocalDateTime.parse("2026-10-17T00:00:00")); knowledge.save(activity);
        when(llm.analyzeUserInput(anyString(),anyList(),anyList())).thenReturn(Map.of(
            "intentCode","GENERAL_QUERY","knowledgeIds","[\"demo-knowledge-tb-activity\"]"));
        result = send(result.session().sessionId(),"bad-model-expiry","有什么优惠活动");
        assertThat(last(result)).doesNotContain("9折");
        assertThat(result.session().receptionMode()).isEqualTo("AI");
    }

    @Test void productFollowupAndDiscountsDoNotMixUnrelatedProductsOrShops() {
        String id = open("tb").session().sessionId();
        send(id,"context-cup","保温杯有什么颜色");
        var cleaned = send(id,"context-clean","怎么清洗");
        assertThat(last(cleaned)).contains("杯盖").doesNotContain("毛巾","收纳盒");
        var box = send(id,"box-discount","收纳盒有什么优惠");
        assertThat(last(box)).contains("9折").doesNotContain("95折","保温杯");
        var dy = send(open("dy").session().sessionId(),"dy-cup-discount","保温杯有什么优惠");
        assertThat(last(dy)).contains("95折").doesNotContain("单件9折");
    }

    @Test void noOrderCustomerCanAskProductCapacityAndUnknownQuestionsStayInAiReception() {
        var opened = service.open(new WorkbenchDTO.Open("demo-shop-tb","demo-customer-tb-3",false));
        var result = send(opened.session().sessionId(),"presale-capacity","保温杯容量多大");
        assertThat(last(result)).contains("500ml","750ml").doesNotContain("未找到匹配订单");
        result = send(opened.session().sessionId(),"unknown-topic","火星基地在哪里");
        assertThat(last(result)).contains("补充具体问题").doesNotContain("联系人工");
        assertThat(result.session().receptionMode()).isEqualTo("AI");
        assertThat(result.analysis().replySource()).isEqualTo("CLARIFICATION");
        assertThat(send(opened.session().sessionId(),"explicit-human","我要转人工").session().receptionMode()).isEqualTo("WAITING_HUMAN");
    }

    @Test void identityBoundSessionRestoresAndRejectsCrossShopCustomer() {
        var s = open("tb");
        assertThat(open("tb").session().sessionId()).isEqualTo(s.session().sessionId());
        assertThat(s.messages()).hasSize(1);
        assertThat(s.orders()).hasSize(4).allSatisfy(o -> assertThat(o.get("shopId")).isEqualTo("demo-shop-tb"));
        assertThat(service.list()).hasSize(1);
        assertThatThrownBy(() -> service.open(new WorkbenchDTO.Open("demo-shop-dy","demo-customer-tb-1",false)))
            .isInstanceOf(CommerceException.class);
    }

    @Test void validCustomerWithoutOrdersGetsAnEmptyScopedResult() {
        var opened = service.open(new WorkbenchDTO.Open("demo-shop-tb","demo-customer-tb-3",false));
        assertThat(opened.session().customerName()).contains("无订单测试");
        assertThat(opened.orders()).isEmpty();
        var result = send(opened.session().sessionId(),"empty-order-1","查一下我的订单物流");
        assertThat(result.orders()).isEmpty();
        assertThat(result.logistics()).isEmpty();
        assertThat(last(result)).contains("未找到匹配订单");
        assertThat(result.executions()).anySatisfy(e -> {
            assertThat(e.action()).isEqualTo("get_order"); assertThat(e.status()).isEqualTo("SUCCESS");
        });
        assertThat(result.messages()).noneSatisfy(m -> assertThat(m.content()).contains("TB2026"));
    }

    @Test void logisticsClarifiesOrderAndEnforcesCrossShopAndCustomerIsolation() {
        String id = open("tb").session().sessionId();
        var multiple = send(id,"l1","我的杯子到哪了");
        assertThat(multiple.selectedOrderId()).isNull();
        assertThat(multiple.messages().get(multiple.messages().size()-1).orderIds()).hasSize(2);
        var selected = send(id,"l2","TB202610100001");
        assertThat(selected.selectedOrderId()).isEqualTo("demo-order-tb-01");
        assertThat(last(selected)).contains("DEMO-TB-0001","杭州");
        assertThat(last(send(id,"l3","查询 DY202610100001 物流"))).contains("未找到匹配订单");
        assertThat(last(send(id,"l4","查询 TB202610120005 物流"))).contains("未找到匹配订单");
    }

    @Test void confirmedTicketPersistsReusesAndTracksMerchantUpdate() {
        String id = open("dy").session().sessionId();
        var draft = send(id,"t1","我想退货，订单 DY202610100001，原因是不喜欢颜色");
        assertThat(draft.awaitingConfirmation()).isTrue();
        assertThat(tickets.count()).isZero();
        // A fresh read reloads persisted context rather than local frontend state.
        assertThat(fetch(id).awaitingConfirmation()).isTrue();
        var created = send(id,"t2","确认提交");
        assertThat(created.tickets()).hasSize(1);
        assertThat(created.awaitingConfirmation()).isFalse();
        String ticketId = (String)created.tickets().get(0).get("ticketId");
        String number = (String)created.tickets().get(0).get("ticketNumber");
        assertThat(last(created)).contains(number,"待商家审核");
        send(id,"t2","确认提交");
        assertThat(tickets.count()).isEqualTo(1);
        assertThatThrownBy(() -> send(id,"t2","更改提交内容")).isInstanceOf(CommerceException.class);
        ticketWorkflow.update(new UpdateTicket("demo-merchant","demo-shop-dy",ticketId,
            TicketStatus.PENDING_REVIEW,TicketStatus.PROCESSING,"商家核实中","operator"));
        assertThat(fetch(id).executions()).anySatisfy(e -> {
            assertThat(e.action()).isEqualTo("update_service_ticket");
            assertThat(e.status()).isEqualTo("SUCCESS");
            assertThat(e.relatedId()).isEqualTo(ticketId);
        });
        assertThat(last(send(id,"t3","刚才的工单进度怎么样"))).contains(number,"处理中","商家核实中");
        var newSession = service.open(new WorkbenchDTO.Open("demo-shop-dy","demo-customer-dy-1",true));
        assertThat(newSession.tickets()).hasSize(1);
        assertThat(last(send(newSession.session().sessionId(),"t4","工单进度"))).contains(number);
    }

    @Test void merchantUpdateRollsBackStatusAndHistoryWhenExecutionCannotPersistAndCanRetry() {
        String id = open("dy").session().sessionId();
        send(id,"update-draft","想退货 DY202610100001，原因是漏水");
        var created = send(id,"update-confirm","确认提交");
        String ticketId = (String) created.tickets().get(0).get("ticketId");
        var update = new UpdateTicket("demo-merchant","demo-shop-dy",ticketId,
            TicketStatus.PENDING_REVIEW,TicketStatus.REJECTED,"需要补充凭证","operator");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("alter table cs_chat_execution add constraint fail_merchant_execution check (action <> 'update_service_ticket' or status <> 'SUCCESS')");
        try {
            assertThatThrownBy(() -> ticketWorkflow.update(update)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(tickets.findById(ticketId).orElseThrow().getStatus()).isEqualTo(TicketStatus.PENDING_REVIEW);
            assertThat(jdbc.queryForObject("select count(*) from commerce_ticket_history where ticket_id = ?",Long.class,ticketId)).isEqualTo(1);
            assertThat(fetch(id).executions()).anySatisfy(e -> {
                assertThat(e.action()).isEqualTo("update_service_ticket");
                assertThat(e.status()).isEqualTo("FAILED");
            });
        } finally {
            jdbc.execute("alter table cs_chat_execution drop constraint fail_merchant_execution");
        }
        assertThat(ticketWorkflow.update(update).status()).isEqualTo(TicketStatus.REJECTED);
        assertThat(jdbc.queryForObject("select count(*) from commerce_ticket_history where ticket_id = ?",Long.class,ticketId)).isEqualTo(2);
        assertThatThrownBy(() -> ticketWorkflow.update(new UpdateTicket("demo-merchant","demo-shop-dy",ticketId,
            TicketStatus.REJECTED,TicketStatus.PROCESSING,"重新处理","operator"))).isInstanceOf(CommerceException.class);
        assertThat(tickets.findById(ticketId).orElseThrow().getStatus()).isEqualTo(TicketStatus.REJECTED);
    }

    @Test void forgedMerchantScopeCannotAppendExecutionToAnotherShopsSession() {
        String id = open("dy").session().sessionId();
        send(id,"scope-draft","想退货 DY202610100001，原因是漏水");
        var created = send(id,"scope-confirm","确认提交");
        String ticketId = (String) created.tickets().get(0).get("ticketId");
        int before = fetch(id).executions().size();
        assertThatThrownBy(() -> ticketWorkflow.update(new UpdateTicket("demo-merchant","demo-shop-tb",ticketId,
            TicketStatus.PENDING_REVIEW,TicketStatus.REJECTED,"伪造店铺","operator"))).isInstanceOf(CommerceException.class);
        assertThat(fetch(id).executions()).hasSize(before);
        assertThat(tickets.findById(ticketId).orElseThrow().getStatus()).isEqualTo(TicketStatus.PENDING_REVIEW);
    }

    @Test void askingReturnWindowAndPostageDoesNotStartAnApplicationEvenWithReturnIntent() {
        when(llm.analyzeUserInput(anyString(),anyList(),anyList())).thenReturn(Map.of("intentCode","RETURN_EXCHANGE"));
        for (String shop : List.of("tb","dy")) {
            String id = open(shop).session().sessionId();
            var result = send(id,"policy-"+shop,"签收后几天可以申请退货？寄回运费怎么办？");
            assertThat(last(result)).contains(shop.equals("tb")?"15天":"7天");
            assertThat(result.awaitingConfirmation()).isFalse();
            assertThat(result.selectedOrderId()).isNull();
            assertThat(fetch(id).knowledge()).isNotEmpty();
            assertThat(last(send(id,"policy-confirm-"+shop,"确认提交"))).contains("没有可提交");
        }
        assertThat(tickets.count()).isZero();
    }

    @Test void explicitUnknownOrderClearsOldSelectionAndNeverFallsBackToOtherOwnedOrders() {
        String id = open("tb").session().sessionId();
        send(id,"unknown-1","查询 TB202610100001 物流");
        var missing = send(id,"unknown-2","订单 DEMO-NOT-FOUND-000 的物流呢");
        assertThat(last(missing)).contains("未找到匹配订单").doesNotContain("多笔","杭州");
        assertThat(missing.selectedOrderId()).isNull();
        assertThat(missing.messages().get(missing.messages().size()-1).orderIds()).isEmpty();
        assertThat(last(send(id,"unknown-3","订单号：UNKNOWN-1234 的物流"))).contains("未找到匹配订单");
    }

    @Test void capacityAnswersUseOwnedSelectedSpecificationAndClarifyBeforeSelection() {
        String id = open("tb").session().sessionId();
        var ambiguous = send(id,"capacity-1","保温杯容量多大");
        assertThat(last(ambiguous)).contains("500ml","750ml","选择");
        assertThat(ambiguous.selectedOrderId()).isNull();
        send(id,"capacity-2","TB202610110002");
        assertThat(last(send(id,"capacity-3","这个保温杯容量多大"))).contains("750ml").doesNotContain("500ml");
        send(id,"capacity-4","TB202610100001");
        assertThat(last(send(id,"capacity-5","这个杯子多少毫升"))).contains("500ml").doesNotContain("750ml");
        assertThat(last(send(id,"capacity-6","订单 TB202610120005 的容量是多少"))).contains("未找到匹配订单");
        assertThat(fetch(id).selectedOrderId()).isNull();
    }

    @Test void handoffIncludesScopedOrderLatestLogisticsNodeAndCompletedQueries() {
        String id = service.open(new WorkbenchDTO.Open("demo-shop-tb","demo-customer-tb-2",true)).session().sessionId();
        var result = send(id,"handoff-context","订单 TB202610120005，快递一直没动，必须额外赔我500元，找人工");
        assertThat(result.session().receptionMode()).isEqualTo("WAITING_HUMAN");
        assertThat(result.session().handoffSummary()).contains("TB202610120005","demo-order-tb-05","运输延迟",
            "2026-10-14T16:00","转运异常","500元","已完成查询：","get_order","get_logistics","get_service_ticket","已有工单：无");
        assertThat(result.executions()).anySatisfy(e -> {
            assertThat(e.action()).isEqualTo("get_logistics");
            assertThat(e.relatedId()).isEqualTo("demo-order-tb-05");
        });
        assertThat(tickets.count()).isZero();
        verifyNoInteractions(llm);
    }

    @Test void askingNotToSubmitRevokesDraftAndRequiresFreshConfirmationContext() {
        String id = open("dy").session().sessionId();
        for (String cancellation : List.of("先别提交","不要提交。","先不提交","暂不提交")) {
            assertThat(send(id,UUID.randomUUID().toString(),"想退货 DY202610100001，原因是漏水").awaitingConfirmation()).isTrue();
            var cancelled = send(id,UUID.randomUUID().toString(),cancellation);
            assertThat(cancelled.awaitingConfirmation()).isFalse();
            assertThat(last(cancelled)).contains("已取消");
            assertThat(last(send(id,UUID.randomUUID().toString(),"确认提交"))).contains("没有可提交");
        }
        assertThat(tickets.count()).isZero();
    }

    @Test void guardCannotBeBypassedWithForgedConfirmationOrScope() {
        String id = open("dy").session().sessionId();
        assertThatThrownBy(() -> commerce.createAgentTicket(new CreateTicket(
            new Scope("demo-merchant","demo-shop-dy","demo-customer-dy-1"),id,"forged-message",
            "demo-order-dy-01",TicketType.RETURN_REQUEST,"不喜欢",true))).isInstanceOf(CommerceException.class);
        assertThat(tickets.count()).isZero();
        assertThat(last(send(id,"g1","确认提交"))).contains("没有可提交");
    }

    @Test void manualModeDeduplicatesRepliesAndResumesOnlyOnNewCustomerMessage() {
        String id = open("tb").session().sessionId();
        assertThat(send(id,"h1","我要人工").session().receptionMode()).isEqualTo("WAITING_HUMAN");
        service.reception(new WorkbenchDTO.Reception(id,"HUMAN"));
        var human = new WorkbenchDTO.Send(id,"human1","商家为您核实中");
        service.human(human); service.human(human);
        var s = send(id,"h2","我的杯子到哪了");
        assertThat(s.messages().stream().filter(m -> m.role().equals("human"))).hasSize(1);
        assertThat(last(s)).isEqualTo("我的杯子到哪了");
        service.reception(new WorkbenchDTO.Reception(id,"AI"));
        assertThat(last(fetch(id))).contains("下一条顾客消息");
        assertThat(last(send(id,"h3","我的杯子到哪了"))).contains("多笔");
    }

    @Test void takeoverDuringModelRequestDropsLateReplyAndWrite() throws Exception {
        String id = open("dy").session().sessionId();
        send(id,"r1","我想退货 DY202610100001，原因是不喜欢颜色");
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        when(llm.analyzeUserInput(eq("确认提交"),anyList(),anyList())).thenAnswer(invocation -> {
            entered.countDown(); if (!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
            return Map.of("intentCode","GENERAL_QUERY");
        });
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            var future = pool.submit(() -> send(id,"r2","确认提交"));
            assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> send(id,"r2","确认提交")).isInstanceOf(CommerceException.class);
            var taken = service.reception(new WorkbenchDTO.Reception(id,"HUMAN"));
            int count = taken.messages().size();
            release.countDown();
            var result = future.get(10,TimeUnit.SECONDS);
            assertThat(result.session().receptionMode()).isEqualTo("HUMAN");
            assertThat(result.messages()).hasSize(count);
            assertThat(result.awaitingConfirmation()).isFalse();
            assertThat(tickets.count()).isZero();
            service.reception(new WorkbenchDTO.Reception(id,"AI"));
            send(id,"r2","确认提交"); // canceled round must not execute on retry after returning to AI
            assertThat(tickets.count()).isZero();
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    @Test void modelFailureAllowsQueriesButTransfersWritesWithoutFalseSuccess() {
        when(llm.analyzeUserInput(anyString(),anyList(),anyList())).thenReturn(null);
        String id = open("tb").session().sessionId();
        assertThat(last(send(id,"f1","查询 TB202610100001 物流"))).contains("规则降级","DEMO-TB-0001");
        var write = send(id,"f2","我想退货 TB202610100001，原因是不喜欢");
        assertThat(write.session().receptionMode()).isEqualTo("WAITING_HUMAN");
        assertThat(tickets.count()).isZero();
        assertThat(last(write)).contains("等待人工");
    }

    @Test void waitingHumanSummarySurvivesTakeoverAndRepeatedReception() {
        when(llm.analyzeUserInput(anyString(),anyList(),anyList())).thenReturn(null);
        String id = open("dy").session().sessionId();
        String complaint = "订单 DY202610100001 的杯子漏水，想退货";
        var waiting = send(id,"summary-1",complaint);
        String summary = waiting.session().handoffSummary();
        assertThat(summary).contains(complaint,"RETURN","demo-order-dy-01");

        var taken = service.reception(new WorkbenchDTO.Reception(id,"HUMAN"));
        assertThat(taken.session().handoffSummary()).isEqualTo(summary);
        assertThat(taken.awaitingConfirmation()).isFalse();
        var repeated = service.reception(new WorkbenchDTO.Reception(id,"HUMAN"));
        assertThat(repeated.messages()).hasSize(taken.messages().size());
        assertThat(fetch(id).session().handoffSummary()).isEqualTo(summary);
        assertThat(tickets.count()).isZero();
    }

    @Test void directTakeoverCapturesPendingRequestBeforeClearingAndRebuildsForLaterRound() {
        String id = open("dy").session().sessionId();
        String complaint = "想退货 DY202610100001，原因是杯盖漏水";
        assertThat(send(id,"direct-1",complaint).awaitingConfirmation()).isTrue();

        var taken = service.reception(new WorkbenchDTO.Reception(id,"HUMAN"));
        assertThat(taken.session().handoffSummary()).contains(complaint,"诉求：RETURN","原因：杯盖漏水");
        assertThat(taken.awaitingConfirmation()).isFalse();
        assertThat(sessions.findById(id).orElseThrow().getPendingAction()).isNull();

        service.reception(new WorkbenchDTO.Reception(id,"AI"));
        String query = "查询 DY202610100001 物流";
        send(id,"direct-2",query);
        var later = service.reception(new WorkbenchDTO.Reception(id,"HUMAN"));
        assertThat(later.session().handoffSummary()).contains(query).doesNotContain(complaint,"原因：杯盖漏水");
        assertThat(tickets.count()).isZero();
    }

    @Test void scopedKnowledgeIsPersistedAndCannotCrossShops() {
        var tb = send(open("tb").session().sessionId(),"k1","退货期限几天");
        var dy = send(open("dy").session().sessionId(),"k2","退货期限几天");
        assertThat(last(tb)).contains("15天").doesNotContain("7天");
        assertThat(last(dy)).contains("7天").doesNotContain("15天");
        assertThat(fetch(tb.session().sessionId()).knowledge()).extracting(WorkbenchVO.Knowledge::knowledgeId)
            .contains("demo-knowledge-tb-return").doesNotContain("demo-knowledge-dy-return");
        assertThat(knowledge.count()).isEqualTo(39);
    }

    @Test void openingSessionBackfillsLegacyDatesWithoutResetAndExpiredKnowledgeStaysHidden() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String activityId = "demo-knowledge-tb-activity";
        jdbc.update("update cs_scoped_knowledge set kind=null, starts_at=null, ends_at=null, content=?, status=? where id=?",
            "商家修改过的活动", "DISABLED", activityId);
        open("tb");
        var restored = knowledge.findById(activityId).orElseThrow();
        assertThat(restored.getKind()).isEqualTo("ACTIVITY");
        assertThat(restored.getStartsAt()).isEqualTo(java.time.LocalDateTime.parse("2026-10-01T00:00:00"));
        assertThat(restored.getEndsAt()).isEqualTo(java.time.LocalDateTime.parse("2026-10-31T23:59:59"));
        assertThat(restored.getContent()).isEqualTo("商家修改过的活动");
        assertThat(restored.getStatus()).isEqualTo("DISABLED");
        jdbc.update("update cs_scoped_knowledge set status='ACTIVE', ends_at=? where id=?",
            java.time.LocalDateTime.parse("2026-10-17T23:59:59"), activityId);
        open("tb");
        assertThat(knowledgeService.search("demo-merchant", "demo-shop-tb", "活动优惠")).isEmpty();
    }

    @Test void unifiedResetIsRepeatablePreservesOriginalChatAndInvalidatesOldSession() {
        String id = open("dy").session().sessionId();
        send(id,"x1","我想退货 DY202610100001，原因是不喜欢"); send(id,"x2","确认提交");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ChatSession original = new ChatSession(); original.setSessionId("original-chat");
            original.setUserId("original-user"); sessions.save(original);
        });
        service.reset(new WorkbenchDTO.Reset(true)); service.reset(new WorkbenchDTO.Reset(true));
        assertThat(service.list()).isEmpty(); assertThat(tickets.count()).isZero();
        assertThat(sessions.existsById("original-chat")).isTrue();
        assertThatThrownBy(() -> fetch(id)).isInstanceOf(CommerceException.class);
        assertThat(knowledge.count()).isEqualTo(39);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> sessions.deleteById("original-chat"));
    }

    @Test void failureAfterTicketInsertRollsBackBusinessAndReplyAndOriginalMessageCanRetry() {
        String id = open("dy").session().sessionId();
        send(id,"db1","我想退货 DY202610100001，原因是不喜欢颜色");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // Fail after commerce has flushed the ticket and submission, inside the outer message transaction.
        jdbc.execute("alter table cs_chat_execution add constraint fail_chat_execution check (action <> 'create_service_ticket')");
        try {
            assertThatThrownBy(() -> send(id,"db2","确认提交")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(tickets.count()).isZero();
            assertThat(jdbc.queryForObject("select count(*) from commerce_ticket_submission",Long.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from commerce_ticket_history",Long.class)).isZero();
            var failed = fetch(id);
            assertThat(failed.awaitingConfirmation()).isTrue();
            assertThat(failed.executions()).anySatisfy(e -> {
                assertThat(e.action()).isEqualTo("process_customer_message"); assertThat(e.status()).isEqualTo("FAILED");
            });
            assertThat(failed.messages()).noneSatisfy(m -> assertThat(m.ticketId()).isNotNull());
            assertThat(failed.messages().stream().filter(m -> "db2".equals(m.clientMessageId()))).hasSize(1);
        } finally { jdbc.execute("alter table cs_chat_execution drop constraint fail_chat_execution"); }
        var retried = send(id,"db2","确认提交");
        assertThat(retried.tickets()).hasSize(1);
        assertThat(last(retried)).contains("待商家审核");
        assertThat(retried.messages().stream().filter(m -> "db2".equals(m.clientMessageId()))).hasSize(1);
    }

    @Test void compensationAmountTransfersToHumanWithoutModelCallOrTicket() {
        String id = open("dy").session().sessionId();
        var result = send(id,"extra1","订单 DY202610100001，要求额外赔偿500元。补充情况："+"字".repeat(2100));
        assertThat(result.session().receptionMode()).isEqualTo("WAITING_HUMAN");
        assertThat(result.session().handoffSummary()).contains("500元");
        assertThat(result.selectedOrderId()).isEqualTo("demo-order-dy-01");
        assertThat(tickets.count()).isZero(); verifyNoInteractions(llm);
    }

    @Test void failedConfirmationCannotBeRetriedAgainstLaterChangedRequest() {
        String id = open("dy").session().sessionId();
        send(id,"stale1","想退货 DY202610100001，原因是不喜欢颜色");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("alter table cs_chat_execution add constraint fail_stale_confirmation check (action <> 'create_service_ticket')");
        try {
            assertThatThrownBy(() -> send(id,"stale2","确认提交")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        } finally { jdbc.execute("alter table cs_chat_execution drop constraint fail_stale_confirmation"); }
        var changed = send(id,"stale3","想退货 DY202610100001，原因是杯盖漏水");
        assertThat(changed.awaitingConfirmation()).isTrue();
        assertThatThrownBy(() -> send(id,"stale2","确认提交")).isInstanceOf(CommerceException.class)
            .hasMessageContaining("上下文已变化");
        assertThat(tickets.count()).isZero();
        assertThat(send(id,"stale4","确认提交").tickets()).hasSize(1);
    }

    @Test void explicitConfirmationWordingIsSharedByRouterAndGuardAndCancellationClearsDraft() {
        String id = open("dy").session().sessionId();
        assertThat(last(send(id,"long-reason","想退货，原因是"+"字".repeat(1001)))).contains("1000字以内");
        send(id,"word1","想退货 DY202610100001，原因是杯盖漏水");
        assertThat(last(send(id,"word2","不退货了"))).contains("已取消");
        assertThat(fetch(id).awaitingConfirmation()).isFalse();
        send(id,"word3","想退货 DY202610100001，原因是杯盖漏水");
        assertThat(send(id,"word4","确认提交退货诉求。").tickets()).hasSize(1);
    }

    @Test void changingToLogisticsOrPolicyCannotSubmitPreviousReturnDraft() {
        String id = open("dy").session().sessionId();
        send(id,"switch1","想退货 DY202610100001，原因是不喜欢");
        assertThat(last(send(id,"switch2","查询 DY202610100001 物流"))).contains("DEMO-DY-0001");
        assertThat(fetch(id).awaitingConfirmation()).isFalse();
        assertThat(last(send(id,"switch3","确认提交"))).contains("没有可提交");
        send(id,"switch4","想退货 DY202610100001，原因是不喜欢");
        assertThat(last(send(id,"switch5","退货期限几天"))).contains("7天");
        assertThat(last(send(id,"switch6","确认提交"))).contains("没有可提交");
        assertThat(tickets.count()).isZero();
    }

    @Test void resetDuringModelRequestInvalidatesPendingConfirmationAndLateWrite() throws Exception {
        String id = open("dy").session().sessionId();
        send(id,"reset1","想退货 DY202610100001，原因是不喜欢");
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        when(llm.analyzeUserInput(eq("确认提交"),anyList(),anyList())).thenAnswer(invocation -> {
            entered.countDown();
            if (!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
            return Map.of("intentCode","GENERAL_QUERY");
        });
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            var future = pool.submit(() -> send(id,"reset2","确认提交"));
            assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
            service.reset(new WorkbenchDTO.Reset(true)); release.countDown();
            assertThatThrownBy(() -> future.get(10,TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(CommerceException.class);
            assertThat(service.list()).isEmpty(); assertThat(tickets.count()).isZero();
            assertThat(sessions.existsById(id)).isFalse();
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    @Test void httpContractsReturnPersistedSnapshotAndKeepOriginalListCompatible() throws Exception {
        String opened = mvc.perform(post("/api/chat/session/open").contentType(MediaType.APPLICATION_JSON).content("""
            {"shopId":"demo-shop-tb","customerId":"demo-customer-tb-1","forceNew":false}
            """)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.session.receptionMode").value("AI"))
            .andExpect(jsonPath("$.data.messages[0].role").value("system"))
            .andReturn().getResponse().getContentAsString();
        String id = new ObjectMapper().readTree(opened).at("/data/session/sessionId").asText();
        mvc.perform(post("/api/chat/message/send").contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(Map.of("sessionId",id,"clientMessageId","http1","content","查询 TB202610100001 物流"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.selectedOrderId").value("demo-order-tb-01"))
            .andExpect(jsonPath("$.data.messages[2].content").value(org.hamcrest.Matchers.containsString("DEMO-TB-0001")));
        mvc.perform(post("/api/chat/session/list").contentType(MediaType.APPLICATION_JSON).content("{\"workbench\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].sessionId").value(id));
        verifyNoInteractions(originalChat);
        when(originalChat.listSessions(any())).thenReturn(List.of());
        mvc.perform(post("/api/chat/session/list").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        verify(originalChat).listSessions(any());
    }

    @Test void httpRejectsMissingMessageIdUnknownReceptionAndUnconfirmedReset() throws Exception {
        String id = open("tb").session().sessionId();
        mvc.perform(post("/api/chat/message/send").contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(Map.of("sessionId",id,"content","确认提交"))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.errorCode").value("INVALID_ARGUMENT"));
        mvc.perform(post("/api/chat/session/reception/update").contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(Map.of("sessionId",id,"receptionMode","WAITING_HUMAN"))))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/demo/reset").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/chat/session/open").contentType(MediaType.APPLICATION_JSON).content("""
            {"shopId":"demo-shop-dy","customerId":"demo-customer-tb-1"}
            """)).andExpect(status().isNotFound());
        mvc.perform(post("/api/chat/message/human/send").contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(Map.of("sessionId",id,"clientMessageId","human1","content","你好"))))
            .andExpect(status().isConflict());
        assertThat(fetch(id).messages()).hasSize(1); verifyNoInteractions(originalChat);
    }

    private String last(WorkbenchVO.Snapshot s) { return s.messages().get(s.messages().size()-1).content(); }
}
