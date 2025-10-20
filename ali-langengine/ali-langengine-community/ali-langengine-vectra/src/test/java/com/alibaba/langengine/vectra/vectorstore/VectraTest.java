package com.alibaba.langengine.vectra.vectorstore;

import com.alibaba.langengine.core.indexes.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class VectraTest {

    @Test
    void testBasicFunctionality() {
        VectraParam param = new VectraParam();
        assertNotNull(param);
        assertEquals("page_content", param.getFieldNamePageContent());
    }

    @Test
    void testDocumentCreation() {
        Document doc = new Document();
        doc.setPageContent("test content");
        doc.setEmbedding(Arrays.asList(1.0, 2.0, 3.0));
        
        assertEquals("test content", doc.getPageContent());
        assertEquals(3, doc.getEmbedding().size());
    }
}