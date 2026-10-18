package com.anjing.module.commerce.repository;

import com.anjing.module.commerce.entity.Merchant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Merchant m where m.id = :id")
    Optional<Merchant> lockById(@Param("id") String id);
}
