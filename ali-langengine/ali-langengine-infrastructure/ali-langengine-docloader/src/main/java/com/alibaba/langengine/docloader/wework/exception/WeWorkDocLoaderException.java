package com.alibaba.langengine.docloader.wework.exception;


public class WeWorkDocLoaderException extends RuntimeException {

    private final String errorCode;
    private final String operation;

    public WeWorkDocLoaderException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.operation = null;
    }

    public WeWorkDocLoaderException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
        this.operation = null;
    }

    public WeWorkDocLoaderException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.operation = null;
    }

    public WeWorkDocLoaderException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.operation = null;
    }

    public WeWorkDocLoaderException(String errorCode, String operation, String message) {
        super(message);
        this.errorCode = errorCode;
        this.operation = operation;
    }

    public WeWorkDocLoaderException(String errorCode, String operation, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.operation = operation;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getOperation() {
        return operation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        if (errorCode != null && !errorCode.equals("UNKNOWN")) {
            sb.append(" [").append(errorCode).append("]");
        }
        if (operation != null) {
            sb.append(" (").append(operation).append(")");
        }
        sb.append(": ").append(getMessage());
        return sb.toString();
    }

    // 常用错误代码常量
    public static final String ERROR_INVALID_CONFIG = "INVALID_CONFIG";
    public static final String ERROR_AUTH_FAILED = "AUTH_FAILED";
    public static final String ERROR_NETWORK_TIMEOUT = "NETWORK_TIMEOUT";
    public static final String ERROR_API_LIMIT = "API_LIMIT";
    public static final String ERROR_DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND";
    public static final String ERROR_PARSE_FAILED = "PARSE_FAILED";
    public static final String ERROR_THREAD_POOL = "THREAD_POOL";
    public static final String ERROR_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
}
