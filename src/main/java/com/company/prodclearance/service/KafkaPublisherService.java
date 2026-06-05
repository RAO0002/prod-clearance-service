package com.company.prodclearance.service;

import com.company.prodclearance.model.GroupProcessStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Service for publishing processed records to Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic:prod.clearance.output}")
    private String kafkaTopic;

    /**
     * Publish processed record to Kafka with Avro serialization.
     * 
     * @param record the processed GroupProcessStatus record
     */
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public void publishToKafka(GroupProcessStatus record) {
        try {
            // Create Kafka message with headers
            Message<GroupProcessStatus> message = MessageBuilder
                .withPayload(record)
                .setHeader(KafkaHeaders.TOPIC, kafkaTopic)
                .setHeader("product_id", record.getProductId())
                .setHeader("status", record.getStatus())
                .setHeader("aws_request_id", record.getAwsRequestId())
                .build();

            // Send to Kafka
            kafkaTemplate.send(message);

            log.info("Published record to Kafka: topic={}, productId={}, status={}", 
                kafkaTopic, record.getProductId(), record.getStatus());

        } catch (Exception e) {
            log.error("Failed to publish record to Kafka: productId={}, error={}", 
                record.getProductId(), e.getMessage(), e);
            throw new RuntimeException("Kafka publishing failed", e);
        }
    }

}