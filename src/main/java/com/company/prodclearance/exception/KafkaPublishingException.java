package com.company.prodclearance.exception;

/**
 * Exception thrown when Kafka publishing fails
 */
public class KafkaPublishingException extends ProdClearanceServiceException {
    
    private final String recordId;
    
    public KafkaPublishingException(String message, String recordId) {
        super(message);
        this.recordId = recordId;
    }
    
    public KafkaPublishingException(String message, Throwable cause, String recordId) {
        super(message, cause);
        this.recordId = recordId;
    }
    
    public String getRecordId() {
        return recordId;
    }
}
