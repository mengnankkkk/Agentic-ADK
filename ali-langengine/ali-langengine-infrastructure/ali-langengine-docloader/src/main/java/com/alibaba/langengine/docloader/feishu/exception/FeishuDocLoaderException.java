package com.alibaba.langengine.docloader.feishu.exception;


public class FeishuDocLoaderException extends RuntimeException {

    private final String errorCode;

    public FeishuDocLoaderException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
    }

    public FeishuDocLoaderException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
    }

    public FeishuDocLoaderException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FeishuDocLoaderException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}