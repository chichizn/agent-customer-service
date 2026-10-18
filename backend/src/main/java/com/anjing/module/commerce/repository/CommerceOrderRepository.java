package com.anjing.module.commerce.repository;

import com.anjing.module.commerce.entity.CommerceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommerceOrderRepository extends JpaRepository<CommerceOrder, String> {
    List<CommerceOrder> findByMerchantIdAndShopIdAndCustomerIdOrderByOrderedAtDescIdAsc(String merchantId, String shopId, String customerId);
    List<CommerceOrder> findByMerchantIdAndDemoDataTrue(String merchantId);
}
