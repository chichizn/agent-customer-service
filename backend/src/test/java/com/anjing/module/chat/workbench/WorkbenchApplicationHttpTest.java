package com.anjing.module.chat.workbench;

import com.anjing.Application;
import com.anjing.module.commerce.CommerceDemoService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes=Application.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:workbench-http;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.type=org.springframework.jdbc.datasource.DriverManagerDataSource",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "app.features.redis.enabled=false",
    "app.features.distributed-lock.enabled=false",
    "app.features.distributed-lock.provider=local",
    "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2",
    "llm.enabled=false",
    "logging.level.root=WARN"
})
class WorkbenchApplicationHttpTest {
    @LocalServerPort int port;
    @Autowired TestRestTemplate http;
    @Autowired CommerceDemoService demo;
    @Autowired WorkbenchService workbench;
    @MockitoBean org.redisson.api.RedissonClient redissonClient;

    @BeforeEach void resetIsolatedDatabase() {
        demo.load();
        workbench.reset(new WorkbenchDTO.Reset(true));
    }

    @Test void bootedApplicationRoutesIdentityAndEmptyCustomerConversationOverHttp() {
        JsonNode identities = post("/api/commerce/demo/identities", Map.of());
        assertThat(identities.at("/data/shops/1/customers").toString()).contains("demo-customer-tb-3");

        JsonNode opened = post("/api/chat/session/open", Map.of(
            "shopId","demo-shop-tb","customerId","demo-customer-tb-3","forceNew",true));
        String sessionId = opened.at("/data/session/sessionId").asText();
        assertThat(sessionId).isNotBlank();
        assertThat(opened.at("/data/orders").size()).isZero();

        JsonNode sent = post("/api/chat/message/send", Map.of(
            "sessionId",sessionId,"clientMessageId","http-empty-orders-1","content","查一下我的订单物流"));
        assertThat(sent.at("/data/orders").size()).isZero();
        assertThat(sent.at("/data/logistics").size()).isZero();
        assertThat(sent.at("/data/messages/2/content").asText()).contains("未找到匹配订单");
        assertThat(sent.at("/data/executions").toString()).contains("get_order","SUCCESS");

        JsonNode listed = post("/api/chat/session/list", Map.of("workbench",true));
        assertThat(listed.at("/data/0/sessionId").asText()).isEqualTo(sessionId);
    }

    private JsonNode post(String path, Object body) {
        ResponseEntity<JsonNode> response = http.postForEntity("http://localhost:"+port+path, body, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asInt()).isIn(0,200);
        return response.getBody();
    }
}
