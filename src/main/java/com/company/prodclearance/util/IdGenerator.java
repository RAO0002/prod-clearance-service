package com.company.prodclearance.util;

import lombok.extern.slf4j.Slf4j;
import java.util.UUID;

/**
 * Utility class for generating unique identifiers and correlation IDs
 */
@Slf4j
public class IdGenerator {
    
    private IdGenerator() {
        // Utility class
    }
    
    /**
     * Generate a unique message ID for Kafka publishing
     */
    public static String generateMessageId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generate a correlation ID for tracing across services
     */
    public static String generateCorrelationId() {
        return "CORR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
    }
    
    /**
     * Generate a processing batch ID
     */
    public static String generateBatchId() {
        return "BATCH-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
    }
}
