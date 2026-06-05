package com.company.prodclearance.config;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for production-grade publishing.
 * Configures Avro serialization with Confluent Schema Registry integration.
 */
@Slf4j
@Configuration
@EnableKafka
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    /**
     * Configure Kafka producer factory with Avro serializer
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        log.info("Initializing Kafka ProducerFactory with Avro serialization");
        
        Map<String, Object> configProps = new HashMap<>();
        
        // Broker configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                kafkaProperties.getBootstrapServers());
        
        // Serialization
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        
        // Schema Registry
        configProps.put("schema.registry.url", kafkaProperties.getSchemaRegistryUrl());
        
        // Producer reliability
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        
        // Performance tuning
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);
        
        // Timeout configuration
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        
        // Idempotence for exactly-once semantics
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Compression
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate for sending messages to Kafka topics
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        log.info("Creating KafkaTemplate bean");
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        template.setDefaultTopic(kafkaProperties.getTopicName());
        return template;
    }

    /**
     * Helper method to create a message with headers
     */
    public static Message<Object> createMessage(Object payload, String recordId, String messageId) {
        return MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, "clr-delta-prod")
                .setHeader("record-id", recordId)
                .setHeader("message-id", messageId)
                .setHeader("timestamp", System.currentTimeMillis())
                .build();
    }
}
