package com.anjing.module.chat.workbench;

import com.anjing.module.chat.LlmService;
import com.anjing.module.chat.ChatController;
import com.anjing.module.chat.ChatService;
import com.anjing.module.chat.entity.*;
import com.anjing.module.chat.repository.*;
import com.anjing.module.commerce.*;
import com.anjing.module.commerce.CommerceDTO.*;
import com.anjing.module.commerce.CommerceTypes.*;
import com.anjing.module.commerce.entity.Merchant;
import com.anjing.module.commerce.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
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
        WorkbenchKnowledgeService.class,SessionTicketGuard.class})
    static class Config {
        @Bean ObjectMapper mapper() { return new ObjectMapper().registerModule(new JavaTimeModule()); }
        @Bean LlmService llm() { return mock(LlmService.class); }
        @Bean static MethodValidationPostProcessor validation() { return new MethodValidationPostProcessor(); }
    }
    @Autowired WorkbenchService service;
    @Autowired CommerceService commerce;
    @Autowired CommerceDemoService demo;
    @Autowired LlmService llm;
    @Autowired ChatSessionRepository sessions;
    @Autowired ChatMessageRepository messages;
    @Autowired ChatExecutionRepository executions;
    @Autowired ScopedKnowledgeRepository knowledge;
    @Autowired ServiceTicketRepository tickets;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired DataSource dataSource;
    private MockMvc mvc;
    private ChatService originalChat;

    @BeforeEach void setup() {
        reset(llm);
        when(llm.analyzeUserInput(anyString())).thenReturn(Map.of("intentCode","GENERAL_QUERY"));
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

    @Test void identityBoundSessionRestoresAndRejectsCrossShopCustomer() {
        var s = open("tb");
        assertThat(open("tb").session().sessionId()).isEqualTo(s.session().sessionId());
        assertThat(s.messages()).hasSize(1);
        assertThat(s.orders()).hasSize(4).allSatisfy(o -> assertThat(o.get("shopId")).isEqualTo("demo-shop-tb"));
        assertThat(service.list()).hasSize(1);
        assertThatThrownBy(() -> service.open(new WorkbenchDTO.Open("demo-shop-dy","demo-customer-tb-1",false)))
            .isInstanceOf(CommerceException.class);
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
        commerce.updateTicket(new UpdateTicket("demo-merchant","demo-shop-dy",ticketId,
            TicketStatus.PENDING_REVIEW,TicketStatus.PROCESSING,"商家核实中","operator"));
        assertThat(last(send(id,"t3","刚才的工单进度怎么样"))).contains(number,"处理中","商家核实中");
        var newSession = service.open(new WorkbenchDTO.Open("demo-shop-dy","demo-customer-dy-1",true));
        assertThat(newSession.tickets()).hasSize(1);
        assertThat(last(send(newSession.session().sessionId(),"t4","工单进度"))).contains(number);
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
        when(llm.analyzeUserInput("确认提交")).thenAnswer(invocation -> {
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
        when(llm.analyzeUserInput(anyString())).thenReturn(null);
        String id = open("tb").session().sessionId();
        assertThat(last(send(id,"f1","查询 TB202610100001 物流"))).contains("规则降级","DEMO-TB-0001");
        var write = send(id,"f2","我想退货 TB202610100001，原因是不喜欢");
        assertThat(write.session().receptionMode()).isEqualTo("WAITING_HUMAN");
        assertThat(tickets.count()).isZero();
        assertThat(last(write)).contains("等待人工");
    }

    @Test void scopedKnowledgeIsPersistedAndCannotCrossShops() {
        var tb = send(open("tb").session().sessionId(),"k1","退货期限几天");
        var dy = send(open("dy").session().sessionId(),"k2","退货期限几天");
        assertThat(last(tb)).contains("15天").doesNotContain("7天");
        assertThat(last(dy)).contains("7天").doesNotContain("15天");
        assertThat(fetch(tb.session().sessionId()).knowledge()).extracting(WorkbenchVO.Knowledge::knowledgeId)
            .contains("demo-knowledge-tb-return").doesNotContain("demo-knowledge-dy-return");
        assertThat(knowledge.count()).isEqualTo(10);
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
        assertThat(knowledge.count()).isEqualTo(10);
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
        when(llm.analyzeUserInput("确认提交")).thenAnswer(invocation -> {
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
