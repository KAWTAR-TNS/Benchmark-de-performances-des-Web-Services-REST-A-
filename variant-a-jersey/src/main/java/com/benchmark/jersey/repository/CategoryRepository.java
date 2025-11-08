package com.benchmark.jersey.repository;

import com.benchmark.jersey.dto.PageResponse;
import com.benchmark.jersey.entity.Category;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

public class CategoryRepository {

    private final EntityManager entityManager;

    public CategoryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public PageResponse<Category> findAll(int page, int size) {
        // Count query
        Long total = entityManager.createQuery(
                "SELECT COUNT(c) FROM Category c", Long.class)
                .getSingleResult();

        // Data query
        TypedQuery<Category> query = entityManager.createQuery(
                "SELECT c FROM Category c ORDER BY c.id", Category.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<Category> categories = query.getResultList();
        return new PageResponse<>(categories, page, size, total);
    }

    public Optional<Category> findById(Long id) {
        Category category = entityManager.find(Category.class, id);
        return Optional.ofNullable(category);
    }

    public Category save(Category category) {
        if (category.getId() == null) {
            entityManager.persist(category);
            return category;
        } else {
            return entityManager.merge(category);
        }
    }

    public void delete(Long id) {
        Category category = entityManager.find(Category.class, id);
        if (category != null) {
            entityManager.remove(category);
        }
    }
}
