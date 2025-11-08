package com.benchmark.jersey.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public class EntityManagerProducer {

    private static EntityManagerFactory emf;

    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            // Read environment variables for database configuration
            Map<String, String> properties = new HashMap<>();
            properties.put("jakarta.persistence.jdbc.url", 
                String.format("jdbc:postgresql://%s:%s/%s",
                    System.getenv().getOrDefault("DB_HOST", "localhost"),
                    System.getenv().getOrDefault("DB_PORT", "5432"),
                    System.getenv().getOrDefault("DB_NAME", "benchmark")));
            properties.put("jakarta.persistence.jdbc.user", 
                System.getenv().getOrDefault("DB_USER", "benchmark"));
            properties.put("jakarta.persistence.jdbc.password", 
                System.getenv().getOrDefault("DB_PASSWORD", "benchmark"));

            emf = Persistence.createEntityManagerFactory("benchmark-pu", properties);
        }
        return emf;
    }

    public static EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
