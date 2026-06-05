package com.company.prodclearance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-specific configuration properties
 */
@Data
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {
    
    private Processing processing = new Processing();
    private Kafka kafka = new Kafka();
    private Mongodb mongodb = new Mongodb();
    private Monitoring monitoring = new Monitoring();
    
    @Data
    public static class Processing {
        private int pollIntervalSeconds = 30;
        private int batchSize = 100;
        private int maxRetryAttempts = 3;
        private long retryBackoffDelayMs = 1000;
        private long retryMaxDelayMs = 10000;
        private int threadPoolSize = 5;
    }
    
    @Data
    public static class Kafka {
        private String topicName = "clr-delta-prod";
        private String schemaSubject = "Clr-Delta-Prod-value";
        private int partitionCount = 3;
        private int replicationFactor = 1;
    }
    
    @Data
    public static class Mongodb {
        private String collectionName = "group_process_status";
        private long queryTimeoutMs = 30000;
    }
    
    @Data
    public static class Monitoring {
        private boolean enabled = true;
        private int metricsPushIntervalSeconds = 60;
    }
}
