package com.alibaba.langengine.docloader.feishu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeishuServiceTest {

    private FeishuService service;

    @BeforeEach
    void setUp() {
        service = new FeishuService();
    }

    @Test
    void testGetDocumentListValidParams() {
        FeishuResult<List<FeishuDocInfo>> result = service.getDocumentList("valid_token", 0, 10);
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals("success", result.getMsg());
    }

    @Test
    void testGetDocumentListInvalidToken() {
        assertThrows(IllegalArgumentException.class, 
            () -> service.getDocumentList(null, 0, 10));
        assertThrows(IllegalArgumentException.class, 
            () -> service.getDocumentList("", 0, 10));
    }

    @Test
    void testGetDocumentListInvalidOffset() {
        assertThrows(IllegalArgumentException.class, 
            () -> service.getDocumentList("token", -1, 10));
    }

    @Test
    void testGetDocumentListInvalidLimit() {
        assertThrows(IllegalArgumentException.class, 
            () -> service.getDocumentList("token", 0, 0));
        assertThrows(IllegalArgumentException.class, 
            () -> service.getDocumentList("token", 0, 101));
    }

    @Test
    void testGetDocumentDetailValidParams() {
        FeishuResult<FeishuDocInfo> result = service.getDocumentDetail("valid_token", "doc_id");
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals("success", result.getMsg());
    }

    @Test
    void testGetDocumentDetailInvalidParams() {
        assertThrows(IllegalArgumentException.class, 
            () -> service.getDocumentDetail(null, "doc_id"));
        assertThrows(IllegalArgumentException.class, 
            () -> service.getDocumentDetail("token", null));
        assertThrows(IllegalArgumentException.class, 
            () -> service.getDocumentDetail("token", ""));
    }
}