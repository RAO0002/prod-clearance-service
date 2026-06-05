package com.company.prodclearance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka configuration properties
 */
@Data
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {
    private String bootstrapServers;
    private String schemaRegistryUrl;
    private String topicName;
    private String schemaSubject;
    private int partitionCount;
    private int replicationFactor;
}
