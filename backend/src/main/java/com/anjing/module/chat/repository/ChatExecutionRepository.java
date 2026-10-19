package com.anjing.module.chat.repository;
import com.anjing.module.chat.entity.ChatExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ChatExecutionRepository extends JpaRepository<ChatExecution,String> {
    List<ChatExecution> findBySessionIdOrderByCreatedAtAscIdAsc(String sessionId);
    void deleteBySessionId(String sessionId);
}
