/**
 * Copyright (C) 2024 AIDC-AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.langengine.vectra.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VectraException extends RuntimeException {

    private final String errorCode;
    private final LocalDateTime timestamp;
    private final String operation;
    private final ErrorSeverity severity;
    private final String requestId;
    private final String context;
    
    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public VectraException(String message) {
        super(message);
        this.errorCode = "VECTRA_ERROR";
        this.timestamp = LocalDateTime.now();
        this.operation = "UNKNOWN";
        this.severity = ErrorSeverity.MEDIUM;
        this.requestId = generateRequestId();
        this.context = null;
    }

    public VectraException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "VECTRA_ERROR";
        this.timestamp = LocalDateTime.now();
        this.operation = "UNKNOWN";
        this.severity = ErrorSeverity.MEDIUM;
        this.requestId = generateRequestId();
        this.context = null;
    }

    public VectraException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.operation = extractOperationFromCode(errorCode);
        this.severity = determineSeverity(errorCode);
        this.requestId = generateRequestId();
        this.context = null;
    }

    public VectraException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.operation = extractOperationFromCode(errorCode);
        this.severity = determineSeverity(errorCode);
        this.requestId = generateRequestId();
        this.context = null;
    }
    
    public VectraException(String errorCode, String message, Throwable cause, String context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.operation = extractOperationFromCode(errorCode);
        this.severity = determineSeverity(errorCode);
        this.requestId = generateRequestId();
        this.context = context;
    }
    
    private String generateRequestId() {
        return "REQ_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }
    
    private String extractOperationFromCode(String errorCode) {
        if (errorCode.contains("CLIENT")) return "CLIENT_OPERATION";
        if (errorCode.contains("COLLECTION")) return "COLLECTION_OPERATION";
        if (errorCode.contains("VECTOR")) return "VECTOR_OPERATION";
        if (errorCode.contains("SEARCH")) return "SEARCH_OPERATION";
        return "UNKNOWN";
    }
    
    private ErrorSeverity determineSeverity(String errorCode) {
        if (errorCode.contains("CRITICAL") || errorCode.contains("FAILED")) return ErrorSeverity.CRITICAL;
        if (errorCode.contains("ERROR")) return ErrorSeverity.HIGH;
        if (errorCode.contains("WARNING")) return ErrorSeverity.MEDIUM;
        return ErrorSeverity.LOW;
    }

    public String getErrorCode() {
        return errorCode;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public ErrorSeverity getSeverity() {
        return severity;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public String getContext() {
        return context;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VectraException[")
          .append("code=").append(errorCode)
          .append(", requestId=").append(requestId)
          .append(", operation=").append(operation)
          .append(", severity=").append(severity)
          .append(", time=").append(timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .append(", message=").append(getMessage());
        
        if (context != null) {
            sb.append(", context=").append(context);
        }
        
        sb.append("]");
        return sb.toString();
    }
}