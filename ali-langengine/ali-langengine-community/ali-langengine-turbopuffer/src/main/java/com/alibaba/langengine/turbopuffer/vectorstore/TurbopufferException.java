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
package com.alibaba.langengine.turbopuffer.vectorstore;


public class TurbopufferException extends RuntimeException {

    private final String errorCode;

    public TurbopufferException(String message) {
        super(message);
        this.errorCode = "TURBOPUFFER_ERROR";
    }

    public TurbopufferException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TurbopufferException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TURBOPUFFER_ERROR";
    }

    public TurbopufferException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Turbopuffer client connection error
     */
    public static class TurbopufferConnectionException extends TurbopufferException {
        public TurbopufferConnectionException(String message) {
            super("TURBOPUFFER_CONNECTION_ERROR", message);
        }

        public TurbopufferConnectionException(String message, Throwable cause) {
            super("TURBOPUFFER_CONNECTION_ERROR", message, cause);
        }
    }

    /**
     * Turbopuffer API error
     */
    public static class TurbopufferApiException extends TurbopufferException {
        public TurbopufferApiException(String message) {
            super("TURBOPUFFER_API_ERROR", message);
        }

        public TurbopufferApiException(String message, Throwable cause) {
            super("TURBOPUFFER_API_ERROR", message, cause);
        }
    }

    /**
     * Turbopuffer namespace error
     */
    public static class TurbopufferNamespaceException extends TurbopufferException {
        public TurbopufferNamespaceException(String message) {
            super("TURBOPUFFER_NAMESPACE_ERROR", message);
        }

        public TurbopufferNamespaceException(String message, Throwable cause) {
            super("TURBOPUFFER_NAMESPACE_ERROR", message, cause);
        }
    }

}
