package com.anjing.module.commerce;

import com.anjing.module.commerce.CommerceDTO.*;
import com.anjing.module.commerce.CommerceTypes.*;
import com.anjing.module.commerce.CommerceVO.*;
import com.anjing.module.commerce.entity.*;
import com.anjing.module.commerce.repository.*;
import com.anjing.module.demo.DemoBusinessTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@DataJpaTest(showSql = false, properties = {
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.show_sql=false",
    "spring.datasource.hikari.maximum-pool-size=12"
})
@ContextConfiguration(classes = CommercePersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CommercePersistenceTest {
    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = Merchant.class)
    @EnableJpaRepositories(basePackageClasses = MerchantRepository.class)
    @Import({CommerceService.class, CommerceDemoService.class, DemoBusinessTime.class})
    static class Config {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().registerModule(new JavaTimeModule()); }
        @Bean static MethodValidationPostProcessor methodValidation() { return new MethodValidationPostProcessor(); }
    }

    private static final Scope TB1 = new Scope("demo-merchant", "demo-shop-tb", "demo-customer-tb-1");
    private static final Scope TB2 = new Scope("demo-merchant", "demo-shop-tb", "demo-customer-tb-2");
    private static final Scope DY1 = new Scope("demo-merchant", "demo-shop-dy", "demo-customer-dy-1");
    @Autowired CommerceService service;
    @Autowired CommerceDemoService demo;
    @Autowired MerchantRepository merchants;
    @Autowired ShopRepository shops;
    @Autowired CustomerRepository customers;
    @Autowired CommerceOrderRepository orders;
    @Autowired LogisticsRepository logistics;
    @Autowired ServiceTicketRepository tickets;
    @Autowired TicketSubmissionRepository submissions;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired javax.sql.DataSource dataSource;

    @BeforeEach void setup() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            submissions.deleteAll(); tickets.deleteAll(); logistics.deleteAll(); orders.deleteAll();
            customers.deleteAll(); shops.deleteAll(); merchants.deleteAll();
        });
        demo.load();
    }

    @Test void demoContainsTwoPlatformsTwelveOrdersAndThreeProducts() {
        assertThat(demo.identities().shops()).hasSize(2);
        assertThat(demo.identities().shops()).filteredOn(s -> s.shopId().equals("demo-shop-tb"))
            .singleElement().satisfies(s -> assertThat(s.customers()).hasSize(3));
        assertThat(demo.identities().shops()).filteredOn(s -> s.shopId().equals("demo-shop-dy"))
            .singleElement().satisfies(s -> assertThat(s.customers()).hasSize(2));
        assertThat(orders.count()).isEqualTo(12);
        assertThat(orders.findAll()).extracting(CommerceOrder::getStatus).containsAll(List.of(OrderStatus.values()));
        assertThat(service.getOrder(new OrderQuery(TB1, null, null, null)).orders()).hasSize(4);
        assertThat(service.getOrder(new OrderQuery(DY1, null, null, null)).orders()).hasSize(3);
        assertThat(service.getOrder(new OrderQuery(new Scope("demo-merchant", "demo-shop-tb", "demo-customer-tb-3"),
            null, null, null)).orders()).isEmpty();
    }

    @Test void queriesReturnExplicitAmbiguityAndUseIntersectedFilters() {
        OrderMatches matches = service.getOrder(new OrderQuery(TB1, null, null, "杯"));
        assertThat(matches.matchStatus()).isEqualTo(MatchStatus.NEED_SELECTION);
        assertThat(matches.orders()).extracting(Order::orderId).containsExactly("demo-order-tb-02", "demo-order-tb-01");
        OrderMatches one = service.getOrder(new OrderQuery(TB1, null, "TB202610100001", "白色"));
        assertThat(one.matchStatus()).isEqualTo(MatchStatus.MATCHED);
        assertThat(one.orders().get(0).totalQuantity()).isEqualTo(1);
        assertThat(one.orders().get(0).actualPaid()).isEqualByComparingTo("59.00");
        assertThat(one.orders().get(0).platform()).isEqualTo(Platform.TAOBAO);
        assertThat(service.getOrder(new OrderQuery(TB1, null, "TB202610100001", "收纳盒")).matchStatus()).isEqualTo(MatchStatus.NOT_FOUND);
        assertThat(service.getOrder(new OrderQuery(TB1, null, null, "不存在商品")).orders()).isEmpty();
    }

    @Test void ownershipIsEnforcedForOrdersLogisticsAndCreation() {
        for (String otherOrder : List.of("demo-order-dy-01", "demo-order-tb-05", "missing")) {
            assertThat(service.getOrder(new OrderQuery(TB1, otherOrder, null, null)).matchStatus()).isEqualTo(MatchStatus.NOT_FOUND);
            assertCode(() -> service.getLogistics(new LogisticsQuery(TB1, otherOrder)), "NOT_FOUND");
            assertCode(() -> service.createTicket(create(TB1, otherOrder, "message-" + otherOrder, true)), "NOT_FOUND");
        }
        assertCode(() -> service.getOrder(new OrderQuery(new Scope("wrong", TB1.shopId(), TB1.customerId()), null, null, null)), "NOT_FOUND");
        assertCode(() -> service.getOrder(new OrderQuery(new Scope(TB1.merchantId(), TB1.shopId(), DY1.customerId()), null, null, null)), "NOT_FOUND");
        assertThat(tickets.count()).isZero();
    }

    @Test void logisticsReturnsRealChronologicalNodesAndNoLogisticsReasons() {
        LogisticsResult result = service.getLogistics(new LogisticsQuery(TB1, "demo-order-tb-01"));
        assertThat(result.available()).isTrue();
        assertThat(result.trackingNumber()).isEqualTo("DEMO-TB-0001");
        assertThat(result.nodes()).hasSize(2).extracting(LogisticsNode::occurredAt).isSorted();
        LogisticsResult pending = service.getLogistics(new LogisticsQuery(TB1, "demo-order-tb-02"));
        assertThat(pending.available()).isFalse(); assertThat(pending.unavailableReason()).contains("尚未发货");
        assertThat(pending.nodes()).isEmpty(); assertThat(pending.carrier()).isNull();
        assertThat(service.getLogistics(new LogisticsQuery(TB1, "demo-order-tb-04")).unavailableReason()).contains("已取消");
        assertThat(service.getLogistics(new LogisticsQuery(TB2, "demo-order-tb-05")).status()).contains("延迟");
        logistics.delete(logistics.findByOrderId("demo-order-tb-01").orElseThrow());
        assertThat(service.getLogistics(new LogisticsQuery(TB1, "demo-order-tb-01")).unavailableReason()).contains("暂未提供");
    }

    @Test void noConfirmationMissingReasonOrMissingAgentGuardCannotWrite() {
        assertThatThrownBy(() -> service.createTicket(create(DY1, "demo-order-dy-01", "m1", false)))
            .isInstanceOfAny(jakarta.validation.ConstraintViolationException.class, CommerceException.class);
        assertThatThrownBy(() -> service.createTicket(new CreateTicket(DY1, "s1", "m1", "demo-order-dy-01", TicketType.RETURN_REQUEST, "  ", true)))
            .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertCode(() -> service.createAgentTicket(create(DY1, "demo-order-dy-01", "m1", true)), "AGENT_GUARD_UNAVAILABLE");
        assertThat(tickets.count()).isZero(); assertThat(submissions.count()).isZero();
    }

    @Test void ticketIsPersistentQueryableAndNeverClaimsPlatformRefund() {
        TicketCreated created = service.createTicket(create(DY1, "demo-order-dy-01", "m1", true));
        assertThat(created.created()).isTrue(); assertThat(created.ticket().ticketNumber()).startsWith("ST");
        assertThat(created.message()).contains("待商家审核", "未执行平台");
        Ticket ticket = service.getServiceTicket(new TicketQuery(DY1, created.ticket().ticketNumber(), "demo-order-dy-01", "session-1")).get(0);
        assertThat(ticket.ticketId()).isEqualTo(created.ticket().ticketId());
        assertThat(ticket.status()).isEqualTo(TicketStatus.PENDING_REVIEW);
        assertThat(ticket.platformAfterSaleStatus()).isEqualTo("NOT_SUBMITTED_TO_PLATFORM");
        assertThat(ticket.history()).hasSize(1);
        assertThat(service.getServiceTicket(new TicketQuery(TB1, ticket.ticketNumber(), null, null))).isEmpty();
        assertThat(service.getServiceTicket(new TicketQuery(new Scope(DY1.merchantId(), DY1.shopId(), "demo-customer-dy-2"), ticket.ticketNumber(), null, null))).isEmpty();
        assertThat(service.getServiceTicket(new TicketQuery(DY1, ticket.ticketNumber(), "demo-order-dy-02", null))).isEmpty();
        assertCode(() -> service.updateTicket(new UpdateTicket(TB1.merchantId(), TB1.shopId(), ticket.ticketId(), ticket.status(), TicketStatus.PROCESSING, "处理", "operator")), "NOT_FOUND");
    }

    @Test void retryAndOpenDedupRemainIdempotentAfterCompletionAcrossSessions() {
        TicketCreated first = service.createTicket(create(DY1, "demo-order-dy-01", "m1", true));
        assertThat(service.createTicket(create(DY1, "demo-order-dy-01", "m1", true)).reuseReason()).isEqualTo("MESSAGE_RETRY");
        CreateTicket duplicate = new CreateTicket(DY1, "session-2", "m2", "demo-order-dy-01", TicketType.RETURN_REQUEST, "补充原因", true);
        TicketCreated second = service.createTicket(duplicate);
        assertThat(second.reuseReason()).isEqualTo("OPEN_SAME_TYPE");
        assertThat(second.ticket().ticketId()).isEqualTo(first.ticket().ticketId());
        assertThat(service.getServiceTicket(new TicketQuery(DY1, null, null, "session-2"))).hasSize(1);
        Ticket processing = update(first.ticket(), TicketStatus.PROCESSING);
        update(processing, TicketStatus.COMPLETED);
        assertThat(service.createTicket(duplicate).ticket().status()).isEqualTo(TicketStatus.COMPLETED);
        assertThat(tickets.count()).isEqualTo(1);
        assertThat(submissions.count()).isEqualTo(2);
        assertThat(service.createTicket(create(DY1, "demo-order-dy-01", "m3", true)).created()).isTrue();
        assertThat(tickets.count()).isEqualTo(2);
    }

    @Test void sameMessageChangedPayloadIsRejectedAndDifferentTypesAreIndependent() {
        service.createTicket(create(DY1, "demo-order-dy-01", "m1", true));
        assertCode(() -> service.createTicket(new CreateTicket(DY1, "session-1", "m1", "demo-order-dy-01", TicketType.RETURN_REQUEST, "变更原因", true)), "IDEMPOTENCY_CONFLICT");
        TicketCreated otherType = service.createTicket(new CreateTicket(DY1, "session-1", "m2", "demo-order-dy-01", TicketType.EXCEPTION_COMPENSATION, "包裹破损申请补偿", true));
        assertThat(otherType.created()).isTrue(); assertThat(tickets.count()).isEqualTo(2);
    }

    @Test void transitionsKeepHistoryRequireExpectedStatusAndDoNotReopen() {
        Ticket initial = service.createTicket(create(DY1, "demo-order-dy-01", "m1", true)).ticket();
        assertCode(() -> update(initial, TicketStatus.COMPLETED), "INVALID_TRANSITION");
        Ticket processing = update(initial, TicketStatus.PROCESSING);
        assertCode(() -> update(initial, TicketStatus.REJECTED), "STALE_STATUS");
        Ticket completed = update(processing, TicketStatus.COMPLETED);
        assertThat(completed.history()).hasSize(3).extracting(HandlingRecord::toStatus)
            .containsExactly(TicketStatus.PENDING_REVIEW, TicketStatus.PROCESSING, TicketStatus.COMPLETED);
        assertThat(completed.history().get(2).note()).isEqualTo("商家已核实并记录处理结果");
        assertCode(() -> update(completed, TicketStatus.PROCESSING), "INVALID_TRANSITION");
        Ticket other = service.createTicket(create(TB1, "demo-order-tb-01", "m2", true)).ticket();
        Ticket rejected = update(other, TicketStatus.REJECTED);
        assertCode(() -> update(rejected, TicketStatus.PROCESSING), "INVALID_TRANSITION");
    }

    @Test void simultaneousSameMessageAndDistinctMessagesCreateOnlyOneOpenTicket() {
        List<TicketCreated> results = concurrently(8, i -> service.createTicket(create(DY1, "demo-order-dy-01", i < 4 ? "same-message" : "m-" + i, true)));
        assertThat(results).extracting(r -> r.ticket().ticketId()).containsOnly(results.get(0).ticket().ticketId());
        assertThat(results.stream().filter(TicketCreated::created).count()).isEqualTo(1);
        assertThat(tickets.count()).isEqualTo(1); assertThat(submissions.count()).isEqualTo(5);
    }

    @Test void concurrentStatusUpdatesAllowOnlyOneWinner() {
        Ticket ticket = service.createTicket(create(DY1, "demo-order-dy-01", "m1", true)).ticket();
        List<String> outcomes = concurrently(6, i -> {
            try { update(ticket, TicketStatus.PROCESSING); return "updated"; }
            catch (CommerceException ex) { return ex.getErrorCode(); }
        });
        assertThat(outcomes.stream().filter("updated"::equals).count()).isEqualTo(1);
        assertThat(outcomes.stream().filter("STALE_STATUS"::equals).count()).isEqualTo(5);
        assertThat(service.getServiceTicket(new TicketQuery(DY1, ticket.ticketNumber(), null, null)).get(0).history()).hasSize(2);
    }

    @Test void demoLoadAndRestoreAreIdempotentAndPreserveNonDemoData() {
        service.createTicket(create(DY1, "demo-order-dy-01", "m1", true));
        demo.load(); assertThat(tickets.count()).isEqualTo(1); assertThat(orders.count()).isEqualTo(12);
        Merchant other = new Merchant(); other.setId("other-merchant"); other.setName("非演示商家"); merchants.saveAndFlush(other);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            CommerceOrder copy = orders.findById("demo-order-tb-01").orElseThrow();
            CommerceOrder nonDemo = new CommerceOrder(); nonDemo.setId("real-order"); nonDemo.setMerchantId("demo-merchant");
            nonDemo.setShopId(copy.getShopId()); nonDemo.setCustomerId(copy.getCustomerId()); nonDemo.setOrderNumber("REAL-001");
            nonDemo.setActualPaid(copy.getActualPaid()); nonDemo.setOrderedAt(copy.getOrderedAt()); nonDemo.setStatus(copy.getStatus());
            orders.save(nonDemo);
            copy.setStatus(OrderStatus.CANCELLED);
        });
        Ticket nonDemoTicket = service.createTicket(create(TB1, "real-order", "real-message", true)).ticket();
        assertCode(() -> demo.restore(false), "CONFIRMATION_REQUIRED");
        demo.restore(true); demo.restore(true);
        assertThat(orders.count()).isEqualTo(13); assertThat(tickets.count()).isEqualTo(1); assertThat(submissions.count()).isEqualTo(1);
        assertThat(merchants.existsById(other.getId())).isTrue();
        assertThat(orders.findById("demo-order-tb-01").orElseThrow().getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(service.getServiceTicket(new TicketQuery(TB1, nonDemoTicket.ticketNumber(), null, null))).hasSize(1);
        assertThat(logistics.count()).isEqualTo(7);
    }

    @Test void restoreRejectsUnmarkedIdCollisionAndMixedReferencesWithoutDeletingTickets() {
        Ticket ticket = service.createTicket(create(DY1, "demo-order-dy-01", "m1", true)).ticket();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ServiceTicket t = tickets.findById(ticket.ticketId()).orElseThrow(); t.setDemoData(false);
        });
        assertCode(() -> demo.restore(true), "DEMO_DATA_CONFLICT");
        assertThat(tickets.count()).isEqualTo(1); assertThat(orders.count()).isEqualTo(12);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            tickets.findById(ticket.ticketId()).orElseThrow().setDemoData(true);
            shops.findById("demo-shop-tb").orElseThrow().setDemoData(false);
        });
        assertCode(() -> demo.load(), "DEMO_DATA_CONFLICT");
        assertCode(() -> demo.restore(true), "DEMO_DATA_CONFLICT");
        assertThat(tickets.count()).isEqualTo(1); assertThat(orders.count()).isEqualTo(12);
    }

    @Test void actualDatabaseFailureRollsBackTicketHistoryAndSubmissionTogether() {
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        jdbc.execute("alter table commerce_ticket_submission add constraint fail_submission check (ticket_id = 'must-fail')");
        try {
            assertThatThrownBy(() -> service.createTicket(create(DY1, "demo-order-dy-01", "m1", true)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(tickets.count()).isZero(); assertThat(submissions.count()).isZero();
            assertThat(jdbc.queryForObject("select count(*) from commerce_ticket_history", Long.class)).isZero();
        } finally { jdbc.execute("alter table commerce_ticket_submission drop constraint fail_submission"); }
        assertThat(service.createTicket(create(DY1, "demo-order-dy-01", "m1", true)).created()).isTrue();
    }

    private Ticket update(Ticket ticket, TicketStatus next) {
        return service.updateTicket(new UpdateTicket(ticket.merchantId(), ticket.shopId(), ticket.ticketId(), ticket.status(), next,
            "商家已核实并记录处理结果", "demo-operator"));
    }
    private CreateTicket create(Scope scope, String orderId, String message, boolean confirmed) {
        return new CreateTicket(scope, "session-1", message, orderId, TicketType.RETURN_REQUEST, "杯盖破损，申请退货", confirmed);
    }
    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(CommerceException.class).extracting("errorCode").isEqualTo(code);
    }
    private <T> List<T> concurrently(int count, java.util.function.IntFunction<T> action) {
        return assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            ExecutorService executor = Executors.newFixedThreadPool(count);
            CountDownLatch ready = new CountDownLatch(count), start = new CountDownLatch(1);
            try {
                List<Future<T>> futures = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    int index = i;
                    futures.add(executor.submit(() -> { ready.countDown(); start.await(); return action.apply(index); }));
                }
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
                List<T> results = new ArrayList<>();
                for (Future<T> future : futures) results.add(future.get(20, TimeUnit.SECONDS));
                return results;
            } finally { start.countDown(); executor.shutdownNow(); }
        });
    }
}
