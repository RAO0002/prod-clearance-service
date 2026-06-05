package com.company.prodclearance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB document representing a group clearance process status.
 * 
 * Status values:
 * - "R" (Received): Initial status, waiting for processing
 * - "P" (Processing): Currently being processed
 * - "C" (Completed): Successfully processed
 * - "E" (Error): Failed processing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "group_process_status")
public class GroupProcessStatus {

    @Id
    private String id;

    @Field("type")
    private String type; // PROD, STAGING, etc.

    @Field("product_id")
    private String productId;

    @Field("status")
    private String status; // R, P, C, E

    @Field("updated_dt")
    private LocalDateTime updatedDt;

    @Field("aws_request_id")
    private String awsRequestId;

    @Field("error_message")
    private String errorMessage;

    @Field("retry_count")
    @Builder.Default
    private int retryCount = 0;

}