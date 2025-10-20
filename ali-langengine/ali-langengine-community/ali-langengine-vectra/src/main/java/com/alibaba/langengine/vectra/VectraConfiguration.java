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
package com.alibaba.langengine.vectra;

import com.alibaba.langengine.core.util.WorkPropertiesUtils;
import com.alibaba.langengine.vectra.exception.VectraException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class VectraConfiguration {

    // Connection settings
    public static final String VECTRA_SERVER_URL = WorkPropertiesUtils.get("vectra_server_url", "http://localhost:30000");
    public static final String VECTRA_API_KEY = getSecureApiKey();
    public static final String VECTRA_DATABASE_NAME = WorkPropertiesUtils.get("vectra_database_name", "default_database");
    
    // Timeout settings
    public static final int VECTRA_CONNECT_TIMEOUT = getIntProperty("vectra_connect_timeout", 30, 5, 300);
    public static final int VECTRA_READ_TIMEOUT = getIntProperty("vectra_read_timeout", 60, 10, 600);
    public static final int VECTRA_WRITE_TIMEOUT = getIntProperty("vectra_write_timeout", 60, 10, 600);
    
    // Connection pool settings
    public static final int VECTRA_POOL_SIZE = getIntProperty("vectra_pool_size", 5, 1, 20);
    public static final int VECTRA_MAX_IDLE_TIME = getIntProperty("vectra_max_idle_time", 300, 60, 3600);
    
    // Security settings
    public static final boolean VECTRA_SSL_ENABLED = Boolean.parseBoolean(WorkPropertiesUtils.get("vectra_ssl_enabled", "true"));
    public static final boolean VECTRA_API_KEY_ENCRYPTED = Boolean.parseBoolean(WorkPropertiesUtils.get("vectra_api_key_encrypted", "false"));
    public static final String VECTRA_ENCRYPTION_KEY = WorkPropertiesUtils.get("vectra_encryption_key", "default_key_12345");
    
    // Rate limiting
    public static final int VECTRA_MAX_REQUESTS_PER_SECOND = getIntProperty("vectra_max_requests_per_second", 100, 1, 1000);
    public static final int VECTRA_BATCH_SIZE = getIntProperty("vectra_batch_size", 100, 1, 1000);
    
    // Access control
    private static final ConcurrentMap<String, Long> ACCESS_TOKENS = new ConcurrentHashMap<>();
    private static final long TOKEN_EXPIRY_TIME = 3600000; // 1 hour
    
    static {
        validateConfiguration();
        log.info("VectraConfiguration initialized with secure settings");
    }
    
    /**
     * Get secure API key with decryption if needed
     */
    private static String getSecureApiKey() {
        String apiKey = WorkPropertiesUtils.get("vectra_api_key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Vectra API key not configured");
            return null;
        }
        
        if (VECTRA_API_KEY_ENCRYPTED) {
            try {
                return decryptApiKey(apiKey);
            } catch (Exception e) {
                log.error("Failed to decrypt API key", e);
                throw new VectraException("CONFIG_ERROR", "Failed to decrypt API key", e);
            }
        }
        
        return apiKey;
    }
    
    /**
     * Decrypt API key using AES
     */
    private static String decryptApiKey(String encryptedKey) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(VECTRA_ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedKey));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Get integer property with validation
     */
    private static int getIntProperty(String key, int defaultValue, int minValue, int maxValue) {
        try {
            int value = Integer.parseInt(WorkPropertiesUtils.get(key, String.valueOf(defaultValue)));
            if (value < minValue || value > maxValue) {
                log.warn("Property {} value {} out of range [{}, {}], using default: {}", 
                    key, value, minValue, maxValue, defaultValue);
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value for property {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Validate configuration
     */
    private static void validateConfiguration() {
        if (VECTRA_SERVER_URL == null || VECTRA_SERVER_URL.trim().isEmpty()) {
            throw new VectraException("CONFIG_ERROR", "Vectra server URL is required");
        }
        
        if (!VECTRA_SERVER_URL.startsWith("http://") && !VECTRA_SERVER_URL.startsWith("https://")) {
            throw new VectraException("CONFIG_ERROR", "Invalid server URL format: " + VECTRA_SERVER_URL);
        }
    }
    
    /**
     * Generate access token for client authentication
     */
    public static String generateAccessToken(String clientId) {
        String token = Base64.getEncoder().encodeToString(
            (clientId + ":" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        ACCESS_TOKENS.put(token, System.currentTimeMillis());
        return token;
    }
    
    /**
     * Validate access token
     */
    public static boolean validateAccessToken(String token) {
        Long timestamp = ACCESS_TOKENS.get(token);
        if (timestamp == null) {
            return false;
        }
        
        if (System.currentTimeMillis() - timestamp > TOKEN_EXPIRY_TIME) {
            ACCESS_TOKENS.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get configuration summary for logging
     */
    public static String getConfigSummary() {
        return String.format("VectraConfig[url=%s, db=%s, poolSize=%d, sslEnabled=%s]",
            maskUrl(VECTRA_SERVER_URL), VECTRA_DATABASE_NAME, VECTRA_POOL_SIZE, VECTRA_SSL_ENABLED);
    }
    
    private static String maskUrl(String url) {
        if (url == null || url.length() < 10) return "***";
        return url.substring(0, 8) + "***" + url.substring(url.length() - 4);
    }
}