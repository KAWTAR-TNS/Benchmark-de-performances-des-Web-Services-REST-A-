package com.benchmark.jersey;

import com.benchmark.jersey.config.EntityManagerProducer;
import com.benchmark.jersey.resource.CategoryResource;
import com.benchmark.jersey.resource.ItemResource;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class JerseyApplication {

    private static final Logger LOGGER = Logger.getLogger(JerseyApplication.class.getName());
    
    private static final String BASE_URI = String.format("http://%s:%s/",
            System.getenv().getOrDefault("SERVER_HOST", "0.0.0.0"),
            System.getenv().getOrDefault("SERVER_PORT", "8081"));

    public static HttpServer startServer() {
        final ResourceConfig rc = new ResourceConfig()
                .packages("com.benchmark.jersey.resource")
                .register(JacksonFeature.class)
                .register(CategoryResource.class)
                .register(ItemResource.class)
                .property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true)
                .property(ServerProperties.WADL_FEATURE_DISABLE, true);

        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) throws IOException {
        LOGGER.info("Starting Jersey REST API (Variant A)...");
        LOGGER.info("Base URI: " + BASE_URI);
        LOGGER.info("Query Mode: " + System.getenv().getOrDefault("QUERY_MODE", "optimized"));
        
        final HttpServer server = startServer();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Jersey REST API...");
            server.shutdownNow();
            EntityManagerProducer.close();
        }));

        LOGGER.info("Jersey REST API started successfully!");
        LOGGER.info("Press Ctrl+C to stop...");
        
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
