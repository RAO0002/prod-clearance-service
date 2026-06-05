package com.company.prodclearance.repository;

import com.company.prodclearance.model.GroupProcessStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataMongoTest
class GroupProcessStatusRepositoryIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private GroupProcessStatusRepository repository;

    @Test
    void testFindAllReceivedRecords() {
        // Arrange
        GroupProcessStatus record1 = GroupProcessStatus.builder()
            .type("PROD")
            .productId("PROD-001")
            .status("R")
            .updatedDt(LocalDateTime.now())
            .build();

        GroupProcessStatus record2 = GroupProcessStatus.builder()
            .type("PROD")
            .productId("PROD-002")
            .status("C")
            .updatedDt(LocalDateTime.now())
            .build();

        repository.save(record1);
        repository.save(record2);

        // Act
        List<GroupProcessStatus> results = repository.findAllReceivedRecords();

        // Assert
        assertEquals(1, results.size());
        assertEquals("PROD-001", results.get(0).getProductId());
        assertEquals("R", results.get(0).getStatus());
    }

    @Test
    void testCountByStatus() {
        // Arrange
        GroupProcessStatus record = GroupProcessStatus.builder()
            .type("PROD")
            .productId("PROD-001")
            .status("R")
            .updatedDt(LocalDateTime.now())
            .build();

        repository.save(record);

        // Act
        long count = repository.countByStatus("R");

        // Assert
        assertEquals(1, count);
    }

}