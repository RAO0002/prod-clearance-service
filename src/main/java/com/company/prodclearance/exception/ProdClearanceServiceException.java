package com.company.prodclearance.exception;

/**
 * Base exception for Product Clearance Service
 */
public class ProdClearanceServiceException extends RuntimeException {
    
    public ProdClearanceServiceException(String message) {
        super(message);
    }
    
    public ProdClearanceServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
