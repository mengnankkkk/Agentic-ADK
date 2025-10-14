package com.alibaba.langengine.docloader.dingtalk.exception;


public class DingTalkCircuitBreakerException extends DingTalkException {

    private final String circuitBreakerState;

    public DingTalkCircuitBreakerException(String message, String state) {
        super(message, "CIRCUIT_BREAKER_OPEN", 503);
        this.circuitBreakerState = state;
    }

    public String getCircuitBreakerState() {
        return circuitBreakerState;
    }

    @Override
    public String toString() {
        return super.toString() + " [state=" + circuitBreakerState + "]";
    }
}
