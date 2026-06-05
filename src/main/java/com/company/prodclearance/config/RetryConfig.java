package com.company.prodclearance.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Spring Retry configuration with exponential backoff
 */
@Slf4j
@Configuration
@EnableRetry
@EnableScheduling
@RequiredArgsConstructor
@EnableConfigurationProperties(ApplicationProperties.class)
public class RetryConfig {

    private final ApplicationProperties applicationProperties;

    /**
     * Thread pool executor for async processing
     */
    @Bean("processingExecutor")
    public Executor processingExecutor() {
        log.info("Creating thread pool executor with size: {}", 
                applicationProperties.getProcessing().getThreadPoolSize());
        return Executors.newFixedThreadPool(
                applicationProperties.getProcessing().getThreadPoolSize()
        );
    }
}
