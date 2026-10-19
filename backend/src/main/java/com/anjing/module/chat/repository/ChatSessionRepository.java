package com.anjing.module.chat.repository;

import com.anjing.module.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChatSession s where s.sessionId = :id")
    Optional<ChatSession> lockById(@Param("id") String id);

    List<ChatSession> findByMerchantIdOrderByUpdatedAtDesc(String merchantId);

    List<ChatSession> findByUserIdOrderByCreatedAtDesc(String userId);

    List<ChatSession> findByStatusOrderByCreatedAtDesc(String status);

    List<ChatSession> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);

    List<ChatSession> findAllByOrderByCreatedAtDesc();
}
