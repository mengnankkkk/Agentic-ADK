package com.alibaba.langengine.ngt.vectorstore;

import lombok.Getter;


public class NgtVectorStoreException extends RuntimeException {

    @Getter
    private final String errorCode;

    public NgtVectorStoreException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public NgtVectorStoreException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static final class ErrorCodes {
        public static final String NATIVE_LIBRARY_LOAD_FAILED = "NATIVE_LIBRARY_LOAD_FAILED";
        public static final String INVALID_CONFIGURATION = "INVALID_CONFIGURATION";
        public static final String INDEX_INITIALIZATION_FAILED = "INDEX_INITIALIZATION_FAILED";
        public static final String INSERT_FAILED = "INSERT_FAILED";
        public static final String SEARCH_FAILED = "SEARCH_FAILED";
        public static final String DELETE_FAILED = "DELETE_FAILED";
        public static final String DIMENSION_MISMATCH = "DIMENSION_MISMATCH";
        public static final String INDEX_NOT_INITIALIZED = "INDEX_NOT_INITIALIZED";
        public static final String RESOURCE_RELEASE_FAILED = "RESOURCE_RELEASE_FAILED";
    }
}
