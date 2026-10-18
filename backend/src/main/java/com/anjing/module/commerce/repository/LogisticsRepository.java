package com.anjing.module.commerce.repository;

import com.anjing.module.commerce.entity.Logistics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LogisticsRepository extends JpaRepository<Logistics, String> {
    Optional<Logistics> findByOrderId(String orderId);
    List<Logistics> findByMerchantIdAndDemoDataTrue(String merchantId);
    List<Logistics> findByMerchantIdAndDemoDataFalse(String merchantId);
}
