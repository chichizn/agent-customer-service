package com.anjing.module.commerce.repository;

import com.anjing.module.commerce.entity.ServiceTicket;
import com.anjing.module.commerce.CommerceTypes.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;

public interface ServiceTicketRepository extends JpaRepository<ServiceTicket, String> {
    List<ServiceTicket> findByMerchantIdAndShopIdAndCustomerIdOrderByCreatedAtDescIdAsc(String merchantId, String shopId, String customerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ServiceTicket> findByMerchantIdAndShopIdAndCustomerIdAndOrderIdAndTicketTypeAndStatusInOrderByCreatedAtAsc(
        String merchantId, String shopId, String customerId, String orderId, TicketType ticketType, Collection<TicketStatus> statuses);
    List<ServiceTicket> findByMerchantIdAndDemoDataTrue(String merchantId);
    List<ServiceTicket> findByMerchantIdAndDemoDataFalse(String merchantId);
}
