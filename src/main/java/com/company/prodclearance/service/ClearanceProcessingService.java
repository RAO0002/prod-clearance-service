package com.company.prodclearance.service;

import com.company.prodclearance.model.GroupProcessStatus;
import com.company.prodclearance.repository.GroupProcessStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for processing clearance records.
 * Contains business logic for executing clearance operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClearanceProcessingService {

    private final GroupProcessStatusRepository repository;

    @Value("${app.retry.max-attempts:3}")
    private int maxRetries;

    /**
     * Execute clearance logic for a single record.
     * This is the main business logic that processes each record.
     * 
     * @param record the GroupProcessStatus record to process
     * @return true if processing succeeded, false otherwise
     */
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    @Transactional
    public boolean executeClearanceLogic(GroupProcessStatus record) {
        try {
            log.info("Processing clearance record: productId={}, status={}", 
                record.getProductId(), record.getStatus());

            // TODO: Implement your actual business logic here
            // Example steps:
            // 1. Validate record data
            if (record.getProductId() == null || record.getProductId().isEmpty()) {
                throw new IllegalArgumentException("Product ID cannot be null or empty");
            }

            // 2. Perform business operations (database calls, API calls, etc.)
            simulateProcessing();

            // 3. Update status to completed
            record.setStatus("C");
            record.setUpdatedDt(LocalDateTime.now());
            record.setRetryCount(0);

            repository.save(record);

            log.info("Successfully processed clearance record: productId={}", record.getProductId());
            return true;

        } catch (Exception e) {
            log.error("Error processing clearance record: productId={}, error={}", 
                record.getProductId(), e.getMessage(), e);
            
            record.setErrorMessage(e.getMessage());
            record.setRetryCount(record.getRetryCount() + 1);
            
            if (record.getRetryCount() >= maxRetries) {
                record.setStatus("E"); // Mark as error
                repository.save(record);
                return false;
            }
            
            throw e; // Trigger retry
        }
    }

    /**
     * Simulate processing delay (replace with actual business logic).
     */
    private void simulateProcessing() throws InterruptedException {
        // Simulate some processing time (100-500ms)
        Thread.sleep(100 + (int)(Math.random() * 400));
    }

}