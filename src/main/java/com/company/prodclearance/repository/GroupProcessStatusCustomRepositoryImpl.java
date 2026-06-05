package com.company.prodclearance.repository;

import com.company.prodclearance.entity.GroupProcessStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Custom implementation of GroupProcessStatusCustomRepository using MongoTemplate.
 * Provides atomic operations to prevent race conditions in distributed processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupProcessStatusCustomRepositoryImpl implements GroupProcessStatusCustomRepository {

    private final MongoTemplate mongoTemplate;
    private static final String COLLECTION_NAME = "group_process_status";

    /**
     * Atomically find and update a record from status 'R' to 'P'.
     * This operation is atomic at the database level, preventing duplicate processing.
     */
    @Override
    public Optional<GroupProcessStatus> findAndUpdateStatusRToP() {
        log.debug("Attempting to find and update record from status R to P");
        
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(GroupProcessStatus.STATUS_READY)
                .and("type").is(GroupProcessStatus.TYPE_PRODUCTION));
        
        Update update = new Update()
                .set("status", GroupProcessStatus.STATUS_PROCESSING)
                .set("updatedDt", Instant.now())
                .inc("attemptCount", 1);
        
        try {
            GroupProcessStatus updated = mongoTemplate.findAndModify(
                    query,
                    update,
                    GroupProcessStatus.class,
                    COLLECTION_NAME
            );
            
            if (updated != null) {
                log.debug("Successfully updated record {} from R to P", updated.getId());
                return Optional.of(updated);
            }
            
            log.debug("No records found with status R for atomic update");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error during atomic status update from R to P", e);
            throw new RuntimeException("Atomic update operation failed", e);
        }
    }

    /**
     * Atomically update multiple records to new status.
     */
    @Override
    public long atomicUpdateStatus(List<String> ids, String newStatus) {
        log.debug("Updating {} records to status {}", ids.size(), newStatus);
        
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(ids));
        
        Update update = new Update()
                .set("status", newStatus)
                .set("updatedDt", Instant.now());
        
        try {
            var result = mongoTemplate.updateMulti(query, update, GroupProcessStatus.class, COLLECTION_NAME);
            log.info("Updated {} records to status {}", result.getModifiedCount(), newStatus);
            return result.getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating multiple records to status {}", newStatus, e);
            throw new RuntimeException("Batch update operation failed", e);
        }
    }

    /**
     * Atomically update a record with error information.
     */
    @Override
    public long updateWithError(String id, String errorMessage) {
        log.warn("Updating record {} with error: {}", id, errorMessage);
        
        Query query = new Query(Criteria.where("_id").is(id));
        
        Update update = new Update()
                .set("status", GroupProcessStatus.STATUS_ERROR)
                .set("errorMessage", errorMessage)
                .set("updatedDt", Instant.now());
        
        try {
            var result = mongoTemplate.updateFirst(query, update, GroupProcessStatus.class, COLLECTION_NAME);
            log.info("Updated record {} with error status", id);
            return result.getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating record {} with error status", id, e);
            throw new RuntimeException("Error update operation failed", e);
        }
    }

    /**
     * Atomically update status along with Kafka publication info.
     */
    @Override
    public Optional<GroupProcessStatus> atomicUpdateStatusWithKafkaInfo(String id, String newStatus, 
                                                                         String kafkaMessageId, String processingResult) {
        log.debug("Updating record {} to status {} with Kafka info", id, newStatus);
        
        Query query = new Query(Criteria.where("_id").is(id));
        
        Update update = new Update()
                .set("status", newStatus)
                .set("updatedDt", Instant.now())
                .set("kafkaMessageId", kafkaMessageId)
                .set("processingResult", processingResult);
        
        try {
            GroupProcessStatus updated = mongoTemplate.findAndModify(
                    query,
                    update,
                    GroupProcessStatus.class,
                    COLLECTION_NAME
            );
            
            if (updated != null) {
                log.debug("Successfully updated record {} with Kafka info", id);
                return Optional.of(updated);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error updating record {} with Kafka info", id, e);
            throw new RuntimeException("Kafka info update operation failed", e);
        }
    }
}
