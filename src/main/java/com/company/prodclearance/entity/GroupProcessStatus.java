package com.company.prodclearance.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

/**
 * MongoDB entity representing group process status records requiring clearance processing.
 * Tracks the lifecycle of clearance requests from initial submission through completion or error.
 */
@Document(collection = "group_process_status")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class GroupProcessStatus {

    /**
     * Unique identifier for the process record
     */
    @Id
    private String id;

    /**
     * Track number - optional field for tracking purposes
     */
    @Field("trackNo")
    private String trackNo;

    /**
     * Type of record - PROD for production records
     */
    @Field("type")
    private String type;

    /**
     * Product identifier - unique SKU or product code
     */
    @Field("productId")
    private String productId;

    /**
     * Track extension - optional field for additional tracking
     */
    @Field("trackExt")
    private String trackExt;

    /**
     * Processing status:
     * R = Ready for processing
     * P = Processing (in progress)
     * C = Completed successfully
     * E = Error occurred
     */
    @Field("status")
    private String status;

    /**
     * Last update timestamp in UTC
     */
    @Field("updatedDt")
    private Instant updatedDt;

    /**
     * AWS request ID for traceability and correlation
     */
    @Field("awsRequestId")
    private String awsRequestId;

    /**
     * Error message - populated when status is 'E'
     */
    @Field("errorMessage")
    private String errorMessage;

    /**
     * ECS task ID that is processing this record
     */
    @Field("processingTaskId")
    private String processingTaskId;

    /**
     * Number of processing attempts
     */
    @Field("attemptCount")
    private Integer attemptCount;

    /**
     * Kafka message ID for correlation
     */
    @Field("kafkaMessageId")
    private String kafkaMessageId;

    /**
     * Business logic processing result
     */
    @Field("processingResult")
    private String processingResult;

    // Status constants
    public static final String STATUS_READY = "R";
    public static final String STATUS_PROCESSING = "P";
    public static final String STATUS_COMPLETED = "C";
    public static final String STATUS_ERROR = "E";

    // Type constants
    public static final String TYPE_PRODUCTION = "PROD";
}
