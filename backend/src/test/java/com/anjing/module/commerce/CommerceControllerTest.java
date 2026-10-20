package com.anjing.module.commerce;

import com.anjing.module.commerce.CommerceDTO.*;
import com.anjing.module.commerce.CommerceTypes.*;
import com.anjing.module.commerce.CommerceVO.*;
import com.anjing.module.chat.workbench.MerchantTicketService;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommerceControllerTest {
    private CommerceService service;
    private CommerceDemoService demo;
    private MockMvc mvc;
    private static final String CREATE = """
        {"scope":{"merchantId":"demo-merchant","shopId":"demo-shop-dy","customerId":"demo-customer-dy-1"},
        "sessionId":"session-1","messageId":"message-1","orderId":"demo-order-dy-01",
        "ticketType":"RETURN_REQUEST","reason":"杯盖破损","customerConfirmed":true}
        """;

    @BeforeEach void setup() {
        service = mock(CommerceService.class); demo = mock(CommerceDemoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CommerceController(service, demo, mock(MerchantTicketService.class)))
            .setControllerAdvice(new CommerceExceptionHandler()).build();
    }

    @Test void queryUsesCommonResponseEnvelopeAndStringEnums() throws Exception {
        when(service.getOrder(any())).thenReturn(new OrderMatches(MatchStatus.NOT_FOUND, List.of(), "当前店铺和顾客下未找到匹配订单"));
        mvc.perform(post("/api/commerce/orders/query").contentType(MediaType.APPLICATION_JSON).content("""
            {"scope":{"merchantId":"demo-merchant","shopId":"demo-shop-dy","customerId":"demo-customer-dy-1"},"productClue":"杯"}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.matchStatus").value("NOT_FOUND")).andExpect(jsonPath("$.data.orders").isEmpty());
    }

    @Test void invalidConfirmationMissingIdentityOrUnknownEnumCannotCallService() throws Exception {
        for (String payload : List.of(CREATE.replace("true", "false"), CREATE.replace("demo-customer-dy-1", ""),
            CREATE.replace("RETURN_REQUEST", "REFUND"), CREATE.replace("\"customerConfirmed\":true", "\"unused\":true"), CREATE.replace("杯盖破损", " "))) {
            mvc.perform(post("/api/commerce/tickets/create").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.errorCode").value("INVALID_ARGUMENT"));
        }
        verifyNoInteractions(service);
    }

    @Test void ownershipConflictAndPersistenceErrorsNeverProduceSuccess() throws Exception {
        doThrow(new CommerceException("NOT_FOUND", "当前范围内未找到资源")).when(service).createTicket(any());
        mvc.perform(post("/api/commerce/tickets/create").contentType(MediaType.APPLICATION_JSON).content(CREATE))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404)).andExpect(jsonPath("$.data.ticket").doesNotExist());
        doThrow(new CommerceException("IDEMPOTENCY_CONFLICT", "提交内容发生变化")).when(service).createTicket(any());
        mvc.perform(post("/api/commerce/tickets/create").contentType(MediaType.APPLICATION_JSON).content(CREATE))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.data.errorCode").value("IDEMPOTENCY_CONFLICT"));
        doThrow(new DataAccessResourceFailureException("database offline")).when(service).createTicket(any());
        mvc.perform(post("/api/commerce/tickets/create").contentType(MediaType.APPLICATION_JSON).content(CREATE))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503))
            .andExpect(jsonPath("$.data.errorCode").value("PERSISTENCE_FAILED")).andExpect(jsonPath("$.data.ticket").doesNotExist());
    }

    @Test void restoreRequiresExplicitConfirmation() throws Exception {
        mvc.perform(post("/api/commerce/demo/restore").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(demo);
    }
}
