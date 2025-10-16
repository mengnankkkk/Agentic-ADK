package com.alibaba.langengine.docloader.wework;

import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.wework.exception.WeWorkDocLoaderException;
import com.alibaba.langengine.docloader.wework.service.WeWorkDocInfo;
import com.alibaba.langengine.docloader.wework.service.WeWorkResult;
import com.alibaba.langengine.docloader.wework.service.WeWorkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


class WeWorkDocLoaderTest {

    @Mock
    private WeWorkService mockService;

    private WeWorkDocLoader loader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        loader = new WeWorkDocLoader("test-token", 30L);
        loader.setService(mockService);
        loader.setNamespace("test-namespace");
    }

    @Test
    void testLoadSingleDocument() {
        // Arrange
        String documentId = "test-doc-id";
        loader.setDocumentId(documentId);

        WeWorkDocInfo docInfo = createSampleDocInfo();
        WeWorkResult<WeWorkDocInfo> result = new WeWorkResult<>();
        result.setData(docInfo);

        when(mockService.getDocumentDetail(anyString(), eq(documentId))).thenReturn(result);

        // Act
        List<Document> documents = loader.load();

        // Assert
        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        assertEquals("Test Document", doc.getMetadata().get("title"));
        assertEquals("Test content", doc.getPageContent());
        
        verify(mockService).getDocumentDetail("test-namespace", documentId);
    }

    @Test
    void testLoadBatchDocuments() {
        // Arrange
        WeWorkDocInfo docInfo1 = createSampleDocInfo();
        docInfo1.setId("doc1");
        docInfo1.setTitle("Document 1");

        WeWorkDocInfo docInfo2 = createSampleDocInfo();
        docInfo2.setId("doc2");
        docInfo2.setTitle("Document 2");

        // Mock document list response
        WeWorkResult<List<WeWorkDocInfo>> listResult = new WeWorkResult<>();
        listResult.setData(Arrays.asList(docInfo1, docInfo2));

        when(mockService.getDocumentList(eq("test-namespace"), eq(0), anyInt()))
            .thenReturn(listResult);

        // Mock empty response for next page
        WeWorkResult<List<WeWorkDocInfo>> emptyResult = new WeWorkResult<>();
        emptyResult.setData(Collections.emptyList());

        when(mockService.getDocumentList(eq("test-namespace"), eq(50), anyInt()))
            .thenReturn(emptyResult);

        // Mock document detail responses
        WeWorkResult<WeWorkDocInfo> detailResult1 = new WeWorkResult<>();
        detailResult1.setData(docInfo1);
        
        WeWorkResult<WeWorkDocInfo> detailResult2 = new WeWorkResult<>();
        detailResult2.setData(docInfo2);

        when(mockService.getDocumentDetail("test-namespace", "doc1")).thenReturn(detailResult1);
        when(mockService.getDocumentDetail("test-namespace", "doc2")).thenReturn(detailResult2);

        // Act
        List<Document> documents = loader.load();

        // Assert
        assertEquals(2, documents.size());
        verify(mockService).getDocumentList("test-namespace", 0, 50);
        verify(mockService).getDocumentDetail("test-namespace", "doc1");
        verify(mockService).getDocumentDetail("test-namespace", "doc2");
    }

    @Test
    void testLoadEmptyResult() {
        // Arrange
        when(mockService.getDocumentDetail(anyString(), anyString())).thenReturn(new WeWorkResult<>());
        loader.setDocumentId("empty_doc");

        // Act
        List<Document> documents = loader.load();

        // Assert
        assertTrue(documents.isEmpty());
    }

    @Test
    void testBuilderPattern() {
        // Act
        WeWorkDocLoader builtLoader = new WeWorkDocLoader.Builder()
            .apiToken("token")
            .namespace("namespace")
            .documentId("doc-id")
            .batchSize(10)
            .returnHtml(true)
            .timeout(20L)
            .build();

        // Assert
        assertEquals("token", builtLoader.getApiToken());
        assertEquals("namespace", builtLoader.getNamespace());
        assertEquals("doc-id", builtLoader.getDocumentId());
        assertEquals(10, builtLoader.getBatchSize());
        assertTrue(builtLoader.isReturnHtml());
    }

    @Test
    void testBuilderValidation() {
        // Test missing API token
        assertThrows(WeWorkDocLoaderException.class, () -> {
            new WeWorkDocLoader.Builder()
                .namespace("namespace")
                .build();
        });

        // Test missing namespace
        assertThrows(WeWorkDocLoaderException.class, () -> {
            new WeWorkDocLoader.Builder()
                .apiToken("token")
                .build();
        });
    }

    @Test
    void testConfigurationValidation() {
        // Test missing API token
        loader.setApiToken(null);
        assertThrows(WeWorkDocLoaderException.class, () -> loader.load());

        // Test missing namespace
        loader.setApiToken("token");
        loader.setNamespace(null);
        assertThrows(WeWorkDocLoaderException.class, () -> loader.load());

        // Test invalid batch size
        loader.setNamespace("namespace");
        loader.setBatchSize(0);
        assertThrows(WeWorkDocLoaderException.class, () -> loader.load());

        loader.setBatchSize(101);
        assertThrows(WeWorkDocLoaderException.class, () -> loader.load());
    }

    @Test
    void testContentCleaning() {
        // Arrange
        WeWorkDocInfo docInfo = createSampleDocInfo();
        docInfo.setBody("<p>HTML <strong>content</strong> with <em>tags</em></p>");

        WeWorkResult<WeWorkDocInfo> result = new WeWorkResult<>();
        result.setData(docInfo);

        when(mockService.getDocumentDetail(anyString(), anyString())).thenReturn(result);
        loader.setDocumentId("test-doc");

        // Act
        List<Document> documents = loader.load();

        // Assert
        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        assertEquals("HTML content with tags", doc.getPageContent());
    }

    @Test
    void testReturnHtmlMode() {
        // Arrange
        WeWorkDocInfo docInfo = createSampleDocInfo();
        docInfo.setBodyHtml("<p>HTML <strong>content</strong></p>");

        WeWorkResult<WeWorkDocInfo> result = new WeWorkResult<>();
        result.setData(docInfo);

        when(mockService.getDocumentDetail(anyString(), anyString())).thenReturn(result);
        
        loader.setDocumentId("test-doc");
        loader.setReturnHtml(true);

        // Act
        List<Document> documents = loader.load();

        // Assert
        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        assertEquals("<p>HTML <strong>content</strong></p>", doc.getPageContent());
    }

    @Test
    void testMetadataPopulation() {
        // Arrange
        WeWorkDocInfo docInfo = createSampleDocInfo();
        docInfo.setCreator("John Doe");
        docInfo.setCreatedAt("1640995200000");
        docInfo.setTags(Arrays.asList("tag1", "tag2"));

        WeWorkResult<WeWorkDocInfo> result = new WeWorkResult<>();
        result.setData(docInfo);

        when(mockService.getDocumentDetail(anyString(), anyString())).thenReturn(result);
        loader.setDocumentId("test-doc");

        // Act
        List<Document> documents = loader.load();

        // Assert
        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        
        assertEquals("Test Document", doc.getMetadata().get("title"));
        assertEquals("John Doe", doc.getMetadata().get("author"));
        assertEquals("1640995200000", doc.getMetadata().get("createdAt"));
        assertEquals(Arrays.asList("tag1", "tag2"), doc.getMetadata().get("tags"));
        assertEquals("wework", doc.getMetadata().get("source"));
    }

    @Test
    void testEmptyContentHandling() {
        // Arrange
        WeWorkDocInfo docInfo = new WeWorkDocInfo();
        docInfo.setId("empty-doc");
        docInfo.setTitle("Empty Document");
        // No body or bodyHtml content

        WeWorkResult<WeWorkDocInfo> result = new WeWorkResult<>();
        result.setData(docInfo);

        when(mockService.getDocumentDetail(anyString(), anyString())).thenReturn(result);
        loader.setDocumentId("empty-doc");

        // Act
        List<Document> documents = loader.load();

        // Assert
        assertTrue(documents.isEmpty());
    }

    @Test
    void testServiceShutdown() {
        // Act
        loader.shutdown();

        // Assert - No exceptions should be thrown
        // Verify that service shutdown is called
        verify(mockService).shutdown();
    }

    private WeWorkDocInfo createSampleDocInfo() {
        WeWorkDocInfo docInfo = new WeWorkDocInfo();
        docInfo.setId("test-doc-id");
        docInfo.setTitle("Test Document");
        docInfo.setBody("Test content");
        docInfo.setBodyHtml("<p>Test content</p>");
        docInfo.setCreator("Test Author");
        docInfo.setCreatedAt("1640995200000");
        docInfo.setUpdatedAt("1640995200000");
        return docInfo;
    }

    void integrationTest() {
        WeWorkDocLoader realLoader = new WeWorkDocLoader.Builder()
            .apiToken("your-real-api-token")
            .namespace("your-real-namespace")
            .batchSize(5)
            .timeout(30L)
            .build();

        try {
            List<Document> documents = realLoader.load();
            assertNotNull(documents);
        } finally {
            realLoader.shutdown();
        }
    }
}
