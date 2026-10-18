package com.anjing.module.commerce.repository;

import com.anjing.module.commerce.entity.TicketSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

public interface TicketSubmissionRepository extends JpaRepository<TicketSubmission, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TicketSubmission s where s.id = :id")
    Optional<TicketSubmission> lockById(@Param("id") String id);
    boolean existsByTicketIdAndSessionIdAndMerchantIdAndShopIdAndCustomerId(String ticketId, String sessionId, String merchantId, String shopId, String customerId);
    List<TicketSubmission> findByMerchantIdAndDemoDataTrue(String merchantId);
    List<TicketSubmission> findByMerchantIdAndDemoDataFalse(String merchantId);
}
