package com.anjing.module.knowledge.repository;

import com.anjing.module.knowledge.entity.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, Long> {
    long countByVectorized(Boolean vectorized);
}
