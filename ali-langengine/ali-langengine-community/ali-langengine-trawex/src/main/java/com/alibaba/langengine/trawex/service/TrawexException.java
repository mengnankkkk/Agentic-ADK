package com.alibaba.langengine.trawex.service;

/**
 * Trawex API 异常类
 * 
 * @author AIDC-AI
 */
public class TrawexException extends RuntimeException {
    
    private final int statusCode;
    private final String errorCode;
    
    public TrawexException(String message) {
        super(message);
        this.statusCode = -1;
        this.errorCode = null;
    }
    
    public TrawexException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.errorCode = null;
    }
    
    public TrawexException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public TrawexException(int statusCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String toString() {
        if (statusCode > 0) {
            return String.format("TrawexException[statusCode=%d, errorCode=%s, message=%s]",
                statusCode, errorCode, getMessage());
        }
        return String.format("TrawexException[message=%s]", getMessage());
    }
}
