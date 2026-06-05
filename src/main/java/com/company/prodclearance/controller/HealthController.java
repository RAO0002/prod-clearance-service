package com.company.prodclearance.controller;

import com.company.prodclearance.repository.GroupProcessStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for health checks and statistics.
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final GroupProcessStatusRepository repository;

    /**
     * Kubernetes/ECS Liveness probe endpoint.
     * Returns 200 if application is alive.
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> live() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis() + "");
        return ResponseEntity.ok(response);
    }

    /**
     * Kubernetes/ECS Readiness probe endpoint.
     * Returns 200 if application is ready to handle traffic.
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        try {
            // Test MongoDB connection
            repository.count();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "UP");
            response.put("mongodb", "connected");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Readiness check failed", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            return ResponseEntity.serviceUnavailable().body(response);
        }
    }

    /**
     * Get processing statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("received_records", repository.countByStatus("R"));
            stats.put("processing_records", repository.countByStatus("P"));
            stats.put("completed_records", repository.countByStatus("C"));
            stats.put("error_records", repository.countByStatus("E"));
            stats.put("total_records", repository.count());
            stats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching statistics", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

}