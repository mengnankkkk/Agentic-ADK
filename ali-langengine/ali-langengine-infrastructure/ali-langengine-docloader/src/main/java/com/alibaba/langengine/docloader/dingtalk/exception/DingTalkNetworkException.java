package com.alibaba.langengine.docloader.dingtalk.exception;


public class DingTalkNetworkException extends DingTalkException {

    public DingTalkNetworkException(String message) {
        super(message, "NETWORK_ERROR", null);
    }

    public DingTalkNetworkException(String message, Throwable cause) {
        super(message, "NETWORK_ERROR", null, cause);
    }

    public DingTalkNetworkException(String message, Integer httpStatusCode, Throwable cause) {
        super(message, "NETWORK_ERROR", httpStatusCode, cause);
    }
}
