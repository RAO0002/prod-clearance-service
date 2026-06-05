package com.company.prodclearance.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Metrics and monitoring configuration for production observability
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsConfig implements WebMvcConfigurer {

    /**
     * Register custom application metrics
     */
    @Bean
    public MeterRegistry meterRegistry() {
        log.info("Initializing MeterRegistry for metrics collection");
        return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    }

    /**
     * Custom tags for Prometheus metrics
     */
    @Bean
    public WebMvcTagsContributor customTagsContributor() {
        return new WebMvcTagsContributor() {
            @Override
            public Iterable<Tag> getTags(javax.servlet.http.HttpServletRequest request, 
                                         javax.servlet.http.HttpServletResponse response, 
                                         Object handler, Exception ex) {
                return Tags.empty();
            }

            @Override
            public Iterable<Tag> getLongRequestTags(javax.servlet.http.HttpServletRequest request, 
                                                    Object handler) {
                return Tags.empty();
            }
        };
    }
}
