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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class Tile38SecurityTest {

    private Tile38SecurityValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new Tile38SecurityValidator(true);
    }

    @Test
    public void testValidCollectionName() {
        assertDoesNotThrow(() -> validator.validateCollectionName("valid_collection"));
        assertDoesNotThrow(() -> validator.validateCollectionName("test-123"));
    }

    @Test
    public void testInvalidCollectionName() {
        assertThrows(Tile38Exception.class, () -> validator.validateCollectionName(""));
        assertThrows(Tile38Exception.class, () -> validator.validateCollectionName("invalid@name"));
        assertThrows(Tile38Exception.class, () -> validator.validateCollectionName("a".repeat(65)));
    }

    @Test
    public void testValidDocumentId() {
        assertDoesNotThrow(() -> validator.validateDocumentId("doc123"));
        assertDoesNotThrow(() -> validator.validateDocumentId("test_doc-456"));
    }

    @Test
    public void testInvalidDocumentId() {
        assertThrows(Tile38Exception.class, () -> validator.validateDocumentId(""));
        assertThrows(Tile38Exception.class, () -> validator.validateDocumentId("doc@123"));
    }

    @Test
    public void testValidCoordinates() {
        assertDoesNotThrow(() -> validator.validateCoordinates(40.7128, -74.0060));
        assertDoesNotThrow(() -> validator.validateCoordinates(0, 0));
    }

    @Test
    public void testInvalidCoordinates() {
        assertThrows(Tile38Exception.class, () -> validator.validateCoordinates(91, 0));
        assertThrows(Tile38Exception.class, () -> validator.validateCoordinates(0, 181));
    }

    @Test
    public void testContentValidation() {
        assertDoesNotThrow(() -> validator.validateContent("Valid content"));
        assertThrows(Tile38Exception.class, () -> validator.validateContent("DROP TABLE users"));
    }

    @Test
    public void testFieldCountValidation() {
        assertDoesNotThrow(() -> validator.validateFieldCount(50));
        assertThrows(Tile38Exception.class, () -> validator.validateFieldCount(101));
    }

    @Test
    public void testDisabledValidation() {
        Tile38SecurityValidator disabledValidator = new Tile38SecurityValidator(false);
        assertDoesNotThrow(() -> disabledValidator.validateCollectionName("invalid@name"));
        assertDoesNotThrow(() -> disabledValidator.validateCoordinates(91, 181));
    }

}