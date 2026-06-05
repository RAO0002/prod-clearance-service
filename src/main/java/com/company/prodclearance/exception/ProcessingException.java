package com.company.prodclearance.exception;

/**
 * Exception thrown when processing of clearance record fails
 */
public class ProcessingException extends ProdClearanceServiceException {
    
    private final String recordId;
    private final String productId;
    
    public ProcessingException(String message, String recordId, String productId) {
        super(message);
        this.recordId = recordId;
        this.productId = productId;
    }
    
    public ProcessingException(String message, Throwable cause, String recordId, String productId) {
        super(message, cause);
        this.recordId = recordId;
        this.productId = productId;
    }
    
    public String getRecordId() {
        return recordId;
    }
    
    public String getProductId() {
        return productId;
    }
}
