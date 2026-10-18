package com.anjing.module.commerce.repository;

import com.anjing.module.commerce.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShopRepository extends JpaRepository<Shop, String> {
    List<Shop> findByMerchantIdOrderById(String merchantId);
}
