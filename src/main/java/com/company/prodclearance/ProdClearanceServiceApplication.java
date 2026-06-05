package com.company.prodclearance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot Application for Production Clearance Service.
 * 
 * This application:
 * - Polls MongoDB for clearance records in 'R' (Received) status
 * - Processes records every 30 seconds (configurable)
 * - Updates status to 'C' (Completed) atomically
 * - Publishes processed records to Kafka with Avro schema
 * - Provides health checks and Prometheus metrics
 * - Runs on AWS ECS Fargate
 */
@SpringBootApplication
@EnableScheduling
@EnableRetry
public class ProdClearanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProdClearanceServiceApplication.class, args);
    }

}