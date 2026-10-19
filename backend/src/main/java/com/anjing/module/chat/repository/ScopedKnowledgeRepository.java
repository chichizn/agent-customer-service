package com.anjing.module.chat.repository;
import com.anjing.module.chat.entity.ScopedKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ScopedKnowledgeRepository extends JpaRepository<ScopedKnowledge,String> {
    List<ScopedKnowledge> findByMerchantIdAndStatus(String merchantId,String status);
    List<ScopedKnowledge> findByMerchantIdAndDemoDataTrue(String merchantId);
}
