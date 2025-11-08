package com.benchmark.jersey.resource;

import com.benchmark.jersey.config.EntityManagerProducer;
import com.benchmark.jersey.dto.PageResponse;
import com.benchmark.jersey.entity.Category;
import com.benchmark.jersey.entity.Item;
import com.benchmark.jersey.repository.ItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ItemResource {

    private boolean useOptimized = "optimized".equals(System.getenv().getOrDefault("QUERY_MODE", "optimized"));

    @GET
    public Response getItems(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("categoryId") Long categoryId) {
        
        EntityManager em = EntityManagerProducer.createEntityManager();
        try {
            ItemRepository repository = new ItemRepository(em, useOptimized);
            
            PageResponse<Item> result;
            if (categoryId != null) {
                result = repository.findByCategoryId(categoryId, page, size);
            } else {
                result = repository.findAll(page, size);
            }
            
            return Response.ok(result).build();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{id}")
    public Response getItem(@PathParam("id") Long id) {
        EntityManager em = EntityManagerProducer.createEntityManager();
        try {
            ItemRepository repository = new ItemRepository(em, useOptimized);
            return repository.findById(id)
                    .map(item -> Response.ok(item).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } finally {
            em.close();
        }
    }

    @POST
    public Response createItem(@Valid Item item) {
        EntityManager em = EntityManagerProducer.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        
        try {
            tx.begin();
            
            // Ensure category exists and is attached
            if (item.getCategory() != null && item.getCategory().getId() != null) {
                Category category = em.find(Category.class, item.getCategory().getId());
                if (category == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Category not found"))
                            .build();
                }
                item.setCategory(category);
            }
            
            ItemRepository repository = new ItemRepository(em, useOptimized);
            Item saved = repository.save(item);
            tx.commit();
            
            return Response.status(Response.Status.CREATED).entity(saved).build();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateItem(@PathParam("id") Long id, @Valid Item item) {
        EntityManager em = EntityManagerProducer.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        
        try {
            tx.begin();
            ItemRepository repository = new ItemRepository(em, useOptimized);
            
            if (repository.findById(id).isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            
            // Ensure category exists and is attached
            if (item.getCategory() != null && item.getCategory().getId() != null) {
                Category category = em.find(Category.class, item.getCategory().getId());
                if (category == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Category not found"))
                            .build();
                }
                item.setCategory(category);
            }
            
            item.setId(id);
            Item updated = repository.save(item);
            tx.commit();
            
            return Response.ok(updated).build();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } finally {
            em.close();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteItem(@PathParam("id") Long id) {
        EntityManager em = EntityManagerProducer.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        
        try {
            tx.begin();
            ItemRepository repository = new ItemRepository(em, useOptimized);
            
            if (repository.findById(id).isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            
            repository.delete(id);
            tx.commit();
            
            return Response.noContent().build();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } finally {
            em.close();
        }
    }

    private static class Map {
        public static java.util.Map<String, String> of(String key, String value) {
            return java.util.Collections.singletonMap(key, value);
        }
    }
}
