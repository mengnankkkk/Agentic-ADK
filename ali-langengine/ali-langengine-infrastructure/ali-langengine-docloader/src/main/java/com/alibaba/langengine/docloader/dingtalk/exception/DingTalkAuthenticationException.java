package com.alibaba.langengine.docloader.dingtalk.exception;


public class DingTalkAuthenticationException extends DingTalkException {

    public DingTalkAuthenticationException(String message) {
        super(message, "AUTH_ERROR", 401);
    }

    public DingTalkAuthenticationException(String message, Throwable cause) {
        super(message, "AUTH_ERROR", 401, cause);
    }

    public DingTalkAuthenticationException(String message, String errorCode) {
        super(message, errorCode, 401);
    }
}
