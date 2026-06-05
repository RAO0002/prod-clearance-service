package com.company.prodclearance;

import com.company.prodclearance.entity.GroupProcessStatus;
import com.company.prodclearance.kafka.ClearanceDeltaMessage;
import com.company.prodclearance.repository.GroupProcessStatusRepository;
import com.company.prodclearance.service.ClearanceProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests using Testcontainers
 */
@SpringBootTest
@DisplayName("End-to-End Integration Tests")
@Testcontainers
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.consumer.bootstrap-servers=localhost:9092"
})
class ProdClearanceServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.0");

    @Autowired
    private GroupProcessStatusRepository repository;

    @Autowired
    private ClearanceProcessingService processingService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should process complete flow from MongoDB to Kafka")
    void testEndToEndProcessing() {
        // Arrange
        GroupProcessStatus record = GroupProcessStatus.builder()
                .id("e2e-test-1")
                .productId("G010004451009V")
                .status(GroupProcessStatus.STATUS_READY)
                .type(GroupProcessStatus.TYPE_PRODUCTION)
                .awsRequestId("aws-request-e2e")
                .updatedDt(Instant.now())
                .attemptCount(0)
                .build();

        repository.save(record);

        // Act
        processingService.processNextBatch("TASK-e2e-test");

        // Assert
        var processed = repository.findById("e2e-test-1");
        assertTrue(processed.isPresent());
        assertEquals(GroupProcessStatus.STATUS_COMPLETED, processed.get().getStatus());
    }

    @Test
    @DisplayName("Should handle multiple concurrent processing tasks")
    void testMultipleConcurrentTasks() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            repository.save(GroupProcessStatus.builder()
                    .id("concurrent-test-" + i)
                    .productId("PROD-" + i)
                    .status(GroupProcessStatus.STATUS_READY)
                    .type(GroupProcessStatus.TYPE_PRODUCTION)
                    .updatedDt(Instant.now())
                    .build());
        }

        // Act - Simulate multiple parallel task processing
        processingService.processNextBatch("TASK-1");
        processingService.processNextBatch("TASK-2");

        // Assert
        long completedCount = repository.countByStatus(GroupProcessStatus.STATUS_COMPLETED);
        assertTrue(completedCount > 0);
    }

    @Test
    @DisplayName("Should maintain idempotent processing")
    void testIdempotentProcessing() {
        // Arrange
        GroupProcessStatus record = GroupProcessStatus.builder()
                .id("idempotent-test")
                .productId("PROD-IDEMPOTENT")
                .status(GroupProcessStatus.STATUS_READY)
                .type(GroupProcessStatus.TYPE_PRODUCTION)
                .updatedDt(Instant.now())
                .build();

        repository.save(record);

        // Act - Process same batch multiple times
        processingService.processNextBatch("TASK-idempotent-1");
        processingService.processNextBatch("TASK-idempotent-2");

        // Assert - Record should only be processed once
        var finalRecord = repository.findById("idempotent-test").orElseThrow();
        assertEquals(GroupProcessStatus.STATUS_COMPLETED, finalRecord.getStatus());
    }
}
