package com.anjing.module.knowledge.repository;

import com.anjing.module.knowledge.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
    long countByVectorized(Boolean vectorized);
}
