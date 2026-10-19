package com.anjing.module.chat;

import com.anjing.module.chat.entity.ChatSession;
import com.anjing.module.chat.repository.ChatMessageRepository;
import com.anjing.module.chat.repository.ChatSessionRepository;
import com.anjing.module.knowledge.repository.ActivityRepository;
import com.anjing.module.knowledge.repository.FaqRepository;
import com.anjing.module.knowledge.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@ContextConfiguration(classes = ChatContextPersistenceTest.Config.class)
class ChatContextPersistenceTest {

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = ChatSession.class)
    @EnableJpaRepositories(basePackageClasses = ChatSessionRepository.class)
    static class Config {
    }

    @org.springframework.beans.factory.annotation.Autowired
    ChatSessionRepository sessions;

    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(
                sessions,
                mock(ChatMessageRepository.class),
                mock(LlmService.class),
                mock(ProductRepository.class),
                mock(ActivityRepository.class),
                mock(FaqRepository.class),
                new ObjectMapper());
    }

    @Test
    void contextUpdateSurvivesReloadAndAppearsInSessionDetail() {
        ChatSession session = new ChatSession();
        session.setSessionId("legacy-context-session");
        session.setUserId("user-1");
        sessions.saveAndFlush(session);

        ChatDTO.UpdateContextDTO update = new ChatDTO.UpdateContextDTO();
        update.setSessionId(session.getSessionId());
        update.setSelectedProducts(List.of("product-1", "product-2"));
        update.setSelectedActivities(List.of("activity-7"));
        update.setUserProfile(Map.of("level", "GOLD", "nickname", "小王"));
        service.updateContext(update);
        sessions.flush();

        ChatVO.ContextVO context = service.getContext(session.getSessionId());
        assertThat(context.getSelectedProducts()).containsExactly("product-1", "product-2");
        assertThat(context.getSelectedActivities()).containsExactly("activity-7");
        assertThat(context.getUserProfile())
                .containsEntry("level", "GOLD")
                .containsEntry("nickname", "小王");

        ChatVO.SessionDetailVO detail = service.getSession(session.getSessionId());
        assertThat(detail.getContext().getSelectedProducts()).containsExactly("product-1", "product-2");
        assertThat(detail.getContext().getSelectedActivities()).containsExactly("activity-7");
        assertThat(detail.getContext().getUserProfile()).containsEntry("nickname", "小王");
    }

    @Test
    void nullFieldsRemainUnchangedAndEmptyListsClearSelections() {
        ChatSession session = new ChatSession();
        session.setSessionId("partial-context-session");
        sessions.saveAndFlush(session);

        ChatDTO.UpdateContextDTO initial = new ChatDTO.UpdateContextDTO();
        initial.setSessionId(session.getSessionId());
        initial.setSelectedProducts(List.of("product-1"));
        initial.setSelectedActivities(List.of("activity-1"));
        initial.setUserProfile(Map.of("level", "SILVER"));
        service.updateContext(initial);

        ChatDTO.UpdateContextDTO partial = new ChatDTO.UpdateContextDTO();
        partial.setSessionId(session.getSessionId());
        partial.setSelectedProducts(List.of());
        service.updateContext(partial);

        ChatVO.ContextVO context = service.getContext(session.getSessionId());
        assertThat(context.getSelectedProducts()).isEmpty();
        assertThat(context.getSelectedActivities()).containsExactly("activity-1");
        assertThat(context.getUserProfile()).containsEntry("level", "SILVER");
    }
}
