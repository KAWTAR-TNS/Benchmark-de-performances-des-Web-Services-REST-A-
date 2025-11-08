package com.benchmark.jersey.resource;

import com.benchmark.jersey.config.EntityManagerProducer;
import com.benchmark.jersey.dto.PageResponse;
import com.benchmark.jersey.entity.Category;
import com.benchmark.jersey.entity.Item;
import com.benchmark.jersey.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {

    private boolean useOptimized = "optimized".equals(System.getenv().getOrDefault("QUERY_MODE", "optimized"));

    @GET
    public Response getCategories(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        EntityManager em = EntityManagerProducer.createEntityManager();
        try {
            CategoryRepository repository = new CategoryRepository(em);
            PageResponse<Category> result = repository.findAll(page, size);
            return Response.ok(result).build();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{id}")
    public Response getCategory(@PathParam("id") Long id) {
        EntityManager em = EntityManagerProducer.createEntityManager();
        try {
            CategoryRepository repository = new CategoryRepository(em);
            return repository.findById(id)
                    .map(category -> Response.ok(category).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/{id}/items")
    public Response getCategoryItems(
            @PathParam("id") Long id,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        EntityManager em = EntityManagerProducer.createEntityManager();
        try {
            // Count query
            Long total = em.createQuery(
                    "SELECT COUNT(i) FROM Item i WHERE i.category.id = :categoryId", Long.class)
                    .setParameter("categoryId", id)
                    .getSingleResult();

            // Data query with optional JOIN FETCH
            String jpql = useOptimized
                    ? "SELECT i FROM Item i JOIN FETCH i.category WHERE i.category.id = :categoryId ORDER BY i.id"
                    : "SELECT i FROM Item i WHERE i.category.id = :categoryId ORDER BY i.id";

            TypedQuery<Item> query = em.createQuery(jpql, Item.class);
            query.setParameter("categoryId", id);
            query.setFirstResult(page * size);
            query.setMaxResults(size);

            List<Item> items = query.getResultList();
            PageResponse<Item> result = new PageResponse<>(items, page, size, total);
            
            return Response.ok(result).build();
        } finally {
            em.close();
        }
    }

    @POST
    public Response createCategory(@Valid Category category) {
        EntityManager em = EntityManagerProducer.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        
        try {
            tx.begin();
            CategoryRepository repository = new CategoryRepository(em);
            Category saved = repository.save(category);
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
    public Response updateCategory(@PathParam("id") Long id, @Valid Category category) {
        EntityManager em = EntityManagerProducer.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        
        try {
            tx.begin();
            CategoryRepository repository = new CategoryRepository(em);
            
            if (repository.findById(id).isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            
            category.setId(id);
            Category updated = repository.save(category);
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
    public Response deleteCategory(@PathParam("id") Long id) {
        EntityManager em = EntityManagerProducer.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        
        try {
            tx.begin();
            CategoryRepository repository = new CategoryRepository(em);
            
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
