package com.alibaba.langengine.docloader.feishu.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeishuAuthenticationInterceptorTest {

    @Test
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, 
            () -> new FeishuAuthenticationInterceptor(null, "secret"));
        assertThrows(IllegalArgumentException.class, 
            () -> new FeishuAuthenticationInterceptor("", "secret"));
        assertThrows(IllegalArgumentException.class, 
            () -> new FeishuAuthenticationInterceptor("id", null));
        assertThrows(IllegalArgumentException.class, 
            () -> new FeishuAuthenticationInterceptor("id", ""));
    }

    @Test
    void testValidConstructor() {
        assertDoesNotThrow(() -> new FeishuAuthenticationInterceptor("valid_id", "valid_secret"));
    }
}