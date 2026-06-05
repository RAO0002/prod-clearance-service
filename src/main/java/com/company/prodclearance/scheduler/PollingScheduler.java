package com.company.prodclearance.scheduler;

import com.company.prodclearance.model.GroupProcessStatus;
import com.company.prodclearance.repository.GroupProcessStatusRepository;
import com.company.prodclearance.service.ClearanceProcessingService;
import com.company.prodclearance.service.KafkaPublisherService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled service that polls MongoDB for records to process.
 * Runs every 30 seconds (configurable via POLL_INTERVAL_SECONDS).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PollingScheduler {

    private final GroupProcessStatusRepository repository;
    private final ClearanceProcessingService clearanceProcessingService;
    private final KafkaPublisherService kafkaPublisherService;
    private final MeterRegistry meterRegistry;

    @Value("${app.poll.interval-seconds:30}")
    private long pollIntervalSeconds;

    @Value("${app.poll.batch-size:100}")
    private int batchSize;

    /**
     * Poll MongoDB for records and process them.
     * Runs on schedule: every 30 seconds (configurable).
     */
    @Scheduled(fixedDelayString = "${app.poll.interval-seconds:30}000", initialDelayString = "5000")
    public void pollAndProcess() {
        try {
            log.info("Starting polling cycle - batchSize={}", batchSize);

            // Fetch records with 'R' status
            List<GroupProcessStatus> recordsToProcess = repository.findAllReceivedRecords();

            if (recordsToProcess.isEmpty()) {
                log.debug("No records to process");
                return;
            }

            log.info("Found {} records to process", recordsToProcess.size());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // Process each record
            for (GroupProcessStatus record : recordsToProcess) {
                try {
                    // Execute business logic
                    boolean success = clearanceProcessingService.executeClearanceLogic(record);

                    if (success) {
                        // Publish to Kafka
                        kafkaPublisherService.publishToKafka(record);
                        successCount.incrementAndGet();
                        recordCounter("success", 1);
                    } else {
                        failureCount.incrementAndGet();
                        recordCounter("failure", 1);
                    }

                } catch (Exception e) {
                    log.error("Error processing record: productId={}", record.getProductId(), e);
                    failureCount.incrementAndGet();
                    recordCounter("failure", 1);
                }
            }

            log.info("Polling cycle completed - success={}, failure={}", 
                successCount.get(), failureCount.get());

        } catch (Exception e) {
            log.error("Error in polling cycle", e);
            recordCounter("error", 1);
        }
    }

    /**
     * Record metrics using Micrometer.
     */
    private void recordCounter(String status, int value) {
        Counter.builder("clearance.processing")
            .tag("status", status)
            .register(meterRegistry)
            .increment(value);
    }

}