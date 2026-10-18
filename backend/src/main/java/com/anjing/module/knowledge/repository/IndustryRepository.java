package com.anjing.module.knowledge.repository;

import com.anjing.module.knowledge.entity.Industry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndustryRepository extends JpaRepository<Industry, Long> {
    long countByVectorized(Boolean vectorized);
}
