package com.benchmark.springdatarest.repository;

import com.benchmark.springdatarest.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(path = "items", collectionResourceRel = "items")
public interface ItemRepository extends JpaRepository<Item, Long> {
    
    @RestResource(path = "by-category", rel = "by-category")
    @Query("SELECT i FROM Item i WHERE i.category.id = :categoryId")
    Page<Item> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
}
