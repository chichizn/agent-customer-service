package com.anjing.module.commerce.repository;

import com.anjing.module.commerce.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    List<Customer> findByMerchantIdAndShopIdOrderById(String merchantId, String shopId);
}
