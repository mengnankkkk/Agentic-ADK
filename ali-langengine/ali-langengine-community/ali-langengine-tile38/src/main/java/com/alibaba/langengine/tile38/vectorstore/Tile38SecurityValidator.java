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
package com.alibaba.langengine.tile38.vectorstore;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;


@Slf4j
public class Tile38SecurityValidator {

    private static final Pattern COLLECTION_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    private static final Pattern DOCUMENT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,128}$");
    private static final int MAX_CONTENT_LENGTH = 1024 * 1024; // 1MB
    private static final int MAX_FIELD_COUNT = 100;

    private final boolean enableValidation;

    public Tile38SecurityValidator(boolean enableValidation) {
        this.enableValidation = enableValidation;
    }

    public void validateCollectionName(String collectionName) {
        if (!enableValidation) return;

        if (StringUtils.isEmpty(collectionName)) {
            throw new Tile38Exception("INVALID_COLLECTION", "Collection name cannot be empty");
        }

        if (!COLLECTION_NAME_PATTERN.matcher(collectionName).matches()) {
            throw new Tile38Exception("INVALID_COLLECTION", 
                "Collection name must contain only alphanumeric characters, hyphens, and underscores");
        }
    }

    public void validateDocumentId(String documentId) {
        if (!enableValidation) return;

        if (StringUtils.isEmpty(documentId)) {
            throw new Tile38Exception("INVALID_DOCUMENT_ID", "Document ID cannot be empty");
        }

        if (!DOCUMENT_ID_PATTERN.matcher(documentId).matches()) {
            throw new Tile38Exception("INVALID_DOCUMENT_ID", 
                "Document ID must contain only alphanumeric characters, hyphens, and underscores");
        }
    }

    public void validateContent(String content) {
        if (!enableValidation) return;

        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new Tile38Exception("CONTENT_TOO_LARGE", 
                "Content size exceeds maximum limit of " + MAX_CONTENT_LENGTH + " bytes");
        }

        if (content != null && containsSqlInjection(content)) {
            throw new Tile38Exception("SECURITY_VIOLATION", "Content contains potential SQL injection");
        }
    }

    public void validateFieldCount(int fieldCount) {
        if (!enableValidation) return;

        if (fieldCount > MAX_FIELD_COUNT) {
            throw new Tile38Exception("TOO_MANY_FIELDS", 
                "Field count exceeds maximum limit of " + MAX_FIELD_COUNT);
        }
    }

    public void validateCoordinates(double lat, double lon) {
        if (!enableValidation) return;

        if (lat < -90 || lat > 90) {
            throw new Tile38Exception("INVALID_COORDINATES", "Latitude must be between -90 and 90");
        }

        if (lon < -180 || lon > 180) {
            throw new Tile38Exception("INVALID_COORDINATES", "Longitude must be between -180 and 180");
        }
    }

    public void validateResultSize(int size, int maxSize) {
        if (!enableValidation) return;

        if (size > maxSize) {
            throw new Tile38Exception("RESULT_TOO_LARGE", 
                "Result size " + size + " exceeds maximum limit of " + maxSize);
        }
    }

    private boolean containsSqlInjection(String input) {
        if (input == null) return false;
        
        String lowerInput = input.toLowerCase();
        String[] sqlKeywords = {"drop", "delete", "truncate", "alter", "create", "insert", "update", 
                               "union", "select", "script", "exec", "execute"};
        
        for (String keyword : sqlKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

}