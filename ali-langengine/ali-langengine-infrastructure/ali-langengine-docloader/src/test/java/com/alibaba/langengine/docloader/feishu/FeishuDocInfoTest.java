package com.alibaba.langengine.docloader.feishu;

import com.alibaba.langengine.docloader.feishu.service.FeishuDocInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeishuDocInfoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSerialization() throws Exception {
        FeishuDocInfo docInfo = new FeishuDocInfo();
        docInfo.setDocumentId("test_id");
        docInfo.setTitle("Test Title");
        docInfo.setContent("Test Content");

        String json = objectMapper.writeValueAsString(docInfo);
        assertNotNull(json);
        assertTrue(json.contains("test_id"));
        assertTrue(json.contains("Test Title"));
    }

    @Test
    void testDeserialization() throws Exception {
        String json = "{\"document_id\":\"test_id\",\"title\":\"Test Title\",\"content\":\"Test Content\"}";
        
        FeishuDocInfo docInfo = objectMapper.readValue(json, FeishuDocInfo.class);
        
        assertEquals("test_id", docInfo.getDocumentId());
        assertEquals("Test Title", docInfo.getTitle());
        assertEquals("Test Content", docInfo.getContent());
    }

    @Test
    void testDefaultConstructor() {
        FeishuDocInfo docInfo = new FeishuDocInfo();
        assertNull(docInfo.getDocumentId());
        assertNull(docInfo.getTitle());
        assertNull(docInfo.getContent());
    }

    @Test
    void testSettersAndGetters() {
        FeishuDocInfo docInfo = new FeishuDocInfo();
        docInfo.setDocumentId("id123");
        docInfo.setTitle("My Document");
        docInfo.setContent("Document content");
        docInfo.setOwnerId("owner123");

        assertEquals("id123", docInfo.getDocumentId());
        assertEquals("My Document", docInfo.getTitle());
        assertEquals("Document content", docInfo.getContent());
        assertEquals("owner123", docInfo.getOwnerId());
    }
}