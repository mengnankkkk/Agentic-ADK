package com.alibaba.langengine.docloader.dingtalk.exception;


public class DingTalkException extends RuntimeException {

    private final String errorCode;
    private final Integer httpStatusCode;

    public DingTalkException(String message) {
        super(message);
        this.errorCode = null;
        this.httpStatusCode = null;
    }

    public DingTalkException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.httpStatusCode = null;
    }

    public DingTalkException(String message, String errorCode, Integer httpStatusCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    public DingTalkException(String message, String errorCode, Integer httpStatusCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (errorCode != null) {
            sb.append(" [errorCode=").append(errorCode).append("]");
        }
        if (httpStatusCode != null) {
            sb.append(" [httpStatus=").append(httpStatusCode).append("]");
        }
        return sb.toString();
    }
}
