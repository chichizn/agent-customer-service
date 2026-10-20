package com.anjing.module.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/** Real loopback HTTP transport: no provider credentials or database required. */
class LlmServiceTest {
    private HttpServer server;
    private ExecutorService executor;
    private LlmService service;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger calls = new AtomicInteger();
    private volatile String response = "{}";
    private volatile int status = 200;
    private volatile int delayMs;
    private volatile Map<?,?> request;
    private volatile String authorization;

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            request = mapper.readValue(exchange.getRequestBody(), Map.class);
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            try {
                Thread.sleep(delayMs);
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        service = new LlmService();
        ReflectionTestUtils.setField(service, "apiUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions");
        ReflectionTestUtils.setField(service, "apiKey", "local-test-secret");
        ReflectionTestUtils.setField(service, "model", "local-test-model");
        ReflectionTestUtils.setField(service, "enabled", true);
        service.configureTimeouts();
    }

    @AfterEach void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void answer(String content) throws Exception {
        response = mapper.writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
    }

    @Test void sendsCompatibleRequestAndParsesFencedIntent() throws Exception {
        answer("```json\n{\"intentCode\":\"RETURN_EXCHANGE\",\"confidence\":0.92}\n```");
        assertThat(service.analyzeUserInput("杯子漏水想退货")).containsEntry("intentCode", "RETURN_EXCHANGE");
        assertThat(authorization).isEqualTo("Bearer local-test-secret");
        assertThat(request.get("model")).isEqualTo("local-test-model");
        assertThat((List<?>) request.get("messages")).hasSize(2);
    }

    @Test void replyPreservesHistoryAndKnowledge() throws Exception {
        answer("请提供订单号。");
        assertThat(service.generateReply("想退货", Map.of("knowledge", "退货需审核"),
                List.of(Map.of("role", "user", "content", "杯子漏水")))).isEqualTo("请提供订单号。");
        var messages = (List<?>) request.get("messages");
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).toString()).contains("退货需审核");
        assertThat(messages.get(1).toString()).contains("杯子漏水");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{}", "[]", "{\"intentCode\":\"DELETE_ORDER\"}", "{\"intentCode\":null}"})
    void rejectsMalformedOrUnknownIntent(String content) throws Exception {
        answer(content);
        assertThat(service.analyzeUserInput("想退货")).isNull();
    }

    @Test void malformedEnvelopeReturnsUnavailable() {
        response = "{\"choices\":[{\"message\":{\"content\":{\"intentCode\":\"RETURN_EXCHANGE\"}}}]}";
        assertThat(service.analyzeUserInput("想退货")).isNull();
        assertThat(service.generateReply("想退货", Map.of(), null)).isNull();
    }

    @Test void gatewayFailureDoesNotLogErrorBodyOrCredentials() {
        status = 500;
        response = "local-test-secret customer-private-text";
        Logger logger = (Logger) LoggerFactory.getLogger(LlmService.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            assertThat(service.analyzeUserInput("想退货")).isNull();
            assertThat(service.generateReply("想退货", Map.of(), null)).isNull();
            assertThat(appender.list).isNotEmpty().allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain("local-test-secret", "customer-private-text");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

    @Test void configuredReadTimeoutReturnsUnavailableAndLongerTimeoutAllowsResponse() throws Exception {
        answer("{\"intentCode\":\"GENERAL_QUERY\"}");
        delayMs = 700;
        ReflectionTestUtils.setField(service, "readTimeoutMs", 100);
        service.configureTimeouts();
        long start = System.nanoTime();
        assertThat(service.analyzeUserInput("你好")).isNull();
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(3000);
        ReflectionTestUtils.setField(service, "readTimeoutMs", 2000);
        service.configureTimeouts();
        assertThat(service.analyzeUserInput("你好")).containsEntry("intentCode", "GENERAL_QUERY");
    }

    @Test void disabledOrMissingKeyNeverCallsGateway() {
        ReflectionTestUtils.setField(service, "enabled", false);
        assertThat(service.analyzeUserInput("你好")).isNull();
        assertThat(service.generateReply("你好", Map.of(), null)).isNull();
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "");
        assertThat(service.analyzeUserInput("你好")).isNull();
        assertThat(calls).hasValue(0);
    }

    @Test void timeoutMustRemainBoundedAndPositive() {
        ReflectionTestUtils.setField(service, "readTimeoutMs", 0);
        assertThatIllegalArgumentException().isThrownBy(service::configureTimeouts);
        ReflectionTestUtils.setField(service, "readTimeoutMs", 20001);
        assertThatIllegalArgumentException().isThrownBy(service::configureTimeouts);
        ReflectionTestUtils.setField(service, "readTimeoutMs", 20000);
        ReflectionTestUtils.setField(service, "connectTimeoutMs", 5001);
        assertThatIllegalArgumentException().isThrownBy(service::configureTimeouts);
    }
}
