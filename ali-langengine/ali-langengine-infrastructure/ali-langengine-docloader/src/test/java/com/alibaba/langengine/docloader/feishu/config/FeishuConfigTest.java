package com.alibaba.langengine.docloader.feishu.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeishuConfigTest {

    @Test
    void testConstants() {
        assertEquals(50, FeishuConfig.DEFAULT_BATCH_SIZE);
        assertEquals(100, FeishuConfig.MAX_BATCH_SIZE);
        assertEquals(30L, FeishuConfig.DEFAULT_TIMEOUT);
        assertEquals(4, FeishuConfig.DEFAULT_CORE_POOL_SIZE);
        assertEquals(8, FeishuConfig.DEFAULT_MAX_POOL_SIZE);
        assertEquals(60L, FeishuConfig.DEFAULT_KEEP_ALIVE_TIME);
        assertEquals(300L, FeishuConfig.TOKEN_REFRESH_ADVANCE_TIME);
        assertEquals("https://feishu.cn/", FeishuConfig.DEFAULT_DOMAIN);
        assertEquals("https://open.feishu.cn/open-apis/", FeishuConfig.API_BASE_URL);
        assertTrue(FeishuConfig.TOKEN_URL.contains("tenant_access_token"));
    }

    @Test
    void testBuilder() {
        FeishuConfig config = FeishuConfig.builder().build();
        assertNotNull(config);
    }
}