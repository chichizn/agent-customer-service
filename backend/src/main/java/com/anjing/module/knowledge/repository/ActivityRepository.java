package com.anjing.module.knowledge.repository;

import com.anjing.module.knowledge.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    long countByVectorized(Boolean vectorized);
}
