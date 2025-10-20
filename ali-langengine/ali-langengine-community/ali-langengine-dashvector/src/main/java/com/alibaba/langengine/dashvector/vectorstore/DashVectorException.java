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
package com.alibaba.langengine.dashvector.vectorstore;


public class DashVectorException extends RuntimeException {

    public enum ErrorCode {
        CONNECTION_FAILED("DASHVECTOR_001", "Failed to connect to DashVector"),
        AUTHENTICATION_FAILED("DASHVECTOR_002", "Authentication failed"),
        COLLECTION_NOT_FOUND("DASHVECTOR_003", "Collection not found"),
        INVALID_PARAMETERS("DASHVECTOR_004", "Invalid parameters"),
        OPERATION_TIMEOUT("DASHVECTOR_005", "Operation timeout"),
        CONNECTION_LIMIT_EXCEEDED("DASHVECTOR_006", "Connection limit exceeded"),
        UNKNOWN_ERROR("DASHVECTOR_999", "Unknown error");
        
        private final String code;
        private final String description;
        
        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    private final String errorCode;
    private final long timestamp;

    public DashVectorException(String message) {
        super(message);
        this.errorCode = null;
        this.timestamp = System.currentTimeMillis();
    }

    public DashVectorException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.timestamp = System.currentTimeMillis();
    }

    public DashVectorException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    public DashVectorException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }
    
    public DashVectorException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode.getCode();
        this.timestamp = System.currentTimeMillis();
    }
    
    public DashVectorException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode.getCode();
        this.timestamp = System.currentTimeMillis();
    }

    public String getErrorCode() {
        return errorCode;
    }
    
    public long getTimestamp() {
        return timestamp;
    }

}