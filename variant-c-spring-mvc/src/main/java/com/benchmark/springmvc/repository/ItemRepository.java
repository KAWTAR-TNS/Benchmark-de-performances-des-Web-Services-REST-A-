package com.benchmark.springmvc.repository;

import com.benchmark.springmvc.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    
    // Baseline mode (N+1 queries)
    Page<Item> findAll(Pageable pageable);
    
    // Optimized mode (JOIN FETCH)
    @Query("SELECT i FROM Item i JOIN FETCH i.category")
    Page<Item> findAllOptimized(Pageable pageable);
    
    // Baseline mode with filter
    Page<Item> findByCategoryId(Long categoryId, Pageable pageable);
    
    // Optimized mode with filter
    @Query("SELECT i FROM Item i JOIN FETCH i.category WHERE i.category.id = :categoryId")
    Page<Item> findByCategoryIdOptimized(@Param("categoryId") Long categoryId, Pageable pageable);
}
