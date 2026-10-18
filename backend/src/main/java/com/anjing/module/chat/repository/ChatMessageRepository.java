package com.anjing.module.chat.repository;

import com.anjing.module.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);

    long countBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
