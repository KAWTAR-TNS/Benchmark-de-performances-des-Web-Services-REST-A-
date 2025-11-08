package com.benchmark.springdatarest.repository;

import com.benchmark.springdatarest.entity.Category;
import com.benchmark.springdatarest.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(path = "categories", collectionResourceRel = "categories")
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    @RestResource(path = "items", rel = "items")
    @Query("SELECT i FROM Item i WHERE i.category.id = :id")
    Page<Item> findItemsByCategoryId(@Param("id") Long id, Pageable pageable);
}
