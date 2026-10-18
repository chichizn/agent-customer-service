package com.anjing.module.knowledge.repository;

import com.anjing.module.knowledge.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    long countByVectorized(Boolean vectorized);
}
