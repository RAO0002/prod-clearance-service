package com.company.prodclearance.repository;

import com.company.prodclearance.entity.GroupProcessStatus;
import java.util.Optional;

/**
 * Custom repository interface for atomic MongoDB operations.
 * Provides operations that require custom implementation with MongoTemplate.
 */
public interface GroupProcessStatusCustomRepository {

    /**
     * Atomically update status from 'R' (Ready) to 'P' (Processing).
     * Uses MongoDB's findAndModify to prevent duplicate processing across multiple tasks.
     * 
     * @return Optional containing the updated record if found, empty if no 'R' status record exists
     */
    Optional<GroupProcessStatus> findAndUpdateStatusRToP();

    /**
     * Atomically update multiple records status from 'P' (Processing) to 'C' (Completed).
     * Updates the updatedDt timestamp to current time.
     * 
     * @param ids List of record IDs to update
     * @param newStatus Target status ('C' for completed or 'E' for error)
     * @return Number of documents updated
     */
    long atomicUpdateStatus(java.util.List<String> ids, String newStatus);

    /**
     * Atomically update a single record with error information.
     * Sets status to 'E' and stores error message.
     * 
     * @param id Record ID
     * @param errorMessage Error message to store
     * @return Number of documents updated (0 or 1)
     */
    long updateWithError(String id, String errorMessage);

    /**
     * Atomic update of status and additional fields in single operation.
     * 
     * @param id Record ID
     * @param newStatus New status value
     * @param kafkaMessageId Kafka message ID from publishing
     * @param processingResult Processing result
     * @return The updated record
     */
    Optional<GroupProcessStatus> atomicUpdateStatusWithKafkaInfo(String id, String newStatus, String kafkaMessageId, String processingResult);
}
