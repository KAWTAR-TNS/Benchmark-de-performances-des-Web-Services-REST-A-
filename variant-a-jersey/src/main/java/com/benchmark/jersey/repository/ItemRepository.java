package com.benchmark.jersey.repository;

import com.benchmark.jersey.dto.PageResponse;
import com.benchmark.jersey.entity.Item;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

public class ItemRepository {

    private final EntityManager entityManager;
    private final boolean useOptimized;

    public ItemRepository(EntityManager entityManager, boolean useOptimized) {
        this.entityManager = entityManager;
        this.useOptimized = useOptimized;
    }

    public PageResponse<Item> findAll(int page, int size) {
        // Count query
        Long total = entityManager.createQuery(
                "SELECT COUNT(i) FROM Item i", Long.class)
                .getSingleResult();

        // Data query with optional JOIN FETCH
        String jpql = useOptimized 
                ? "SELECT i FROM Item i JOIN FETCH i.category ORDER BY i.id"
                : "SELECT i FROM Item i ORDER BY i.id";
        
        TypedQuery<Item> query = entityManager.createQuery(jpql, Item.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<Item> items = query.getResultList();
        return new PageResponse<>(items, page, size, total);
    }

    public PageResponse<Item> findByCategoryId(Long categoryId, int page, int size) {
        // Count query
        Long total = entityManager.createQuery(
                "SELECT COUNT(i) FROM Item i WHERE i.category.id = :categoryId", Long.class)
                .setParameter("categoryId", categoryId)
                .getSingleResult();

        // Data query with optional JOIN FETCH
        String jpql = useOptimized
                ? "SELECT i FROM Item i JOIN FETCH i.category WHERE i.category.id = :categoryId ORDER BY i.id"
                : "SELECT i FROM Item i WHERE i.category.id = :categoryId ORDER BY i.id";

        TypedQuery<Item> query = entityManager.createQuery(jpql, Item.class);
        query.setParameter("categoryId", categoryId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<Item> items = query.getResultList();
        return new PageResponse<>(items, page, size, total);
    }

    public Optional<Item> findById(Long id) {
        Item item = entityManager.find(Item.class, id);
        return Optional.ofNullable(item);
    }

    public Item save(Item item) {
        if (item.getId() == null) {
            entityManager.persist(item);
            return item;
        } else {
            return entityManager.merge(item);
        }
    }

    public void delete(Long id) {
        Item item = entityManager.find(Item.class, id);
        if (item != null) {
            entityManager.remove(item);
        }
    }
}
