package com.example.userproduct;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for User Product Service.
 *
 * Why: Entry point for the microservice extracted from the delivery-service monolith.
 *      Manages Users, Products, Categories, Addresses, and Auth/Security.
 * What: Bootstraps the Spring Boot application context.
 * Test: Run the application and verify /api/health returns {"status": "UP"}.
 */
@Slf4j
@SpringBootApplication
public class UserProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserProductApplication.class, args);
        log.info("User Product Service started successfully!");
    }
}
