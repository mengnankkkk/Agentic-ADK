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
package com.alibaba.langengine.sqlitevss.vectorstore;


public class SqliteVssException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public SqliteVssException(String message) {
        super(message);
        this.errorCode = "SQLITE_VSS_ERROR";
    }

    public SqliteVssException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SQLITE_VSS_ERROR";
    }

    public SqliteVssException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SqliteVssException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Database connection error
     */
    public static SqliteVssException connectionError(String message, Throwable cause) {
        return new SqliteVssException("SQLITE_VSS_CONNECTION_ERROR", message, cause);
    }

    /**
     * SQL execution error
     */
    public static SqliteVssException sqlError(String message, Throwable cause) {
        return new SqliteVssException("SQLITE_VSS_SQL_ERROR", message, cause);
    }

    /**
     * Vector search error
     */
    public static SqliteVssException searchError(String message, Throwable cause) {
        return new SqliteVssException("SQLITE_VSS_SEARCH_ERROR", message, cause);
    }

    /**
     * Document operation error
     */
    public static SqliteVssException documentError(String message, Throwable cause) {
        return new SqliteVssException("SQLITE_VSS_DOCUMENT_ERROR", message, cause);
    }

    /**
     * Configuration error
     */
    public static SqliteVssException configError(String message) {
        return new SqliteVssException("SQLITE_VSS_CONFIG_ERROR", message);
    }

    /**
     * Configuration error with cause
     */
    public static SqliteVssException configError(String message, Throwable cause) {
        return new SqliteVssException("SQLITE_VSS_CONFIG_ERROR", message, cause);
    }
}
