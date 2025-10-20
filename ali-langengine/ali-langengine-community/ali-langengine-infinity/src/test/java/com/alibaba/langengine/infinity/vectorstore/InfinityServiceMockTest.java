package com.alibaba.langengine.infinity.vectorstore;

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class InfinityServiceMockTest {

    private static final String TABLE = "test_table";

    @Mock
    private InfinityClient infinityClient;

    @Mock
    private Embeddings embeddings;

    private InfinityService infinityService;

    @BeforeEach
    void setUp() {
        infinityService = new InfinityService(TABLE, infinityClient, new InfinityParam());
    }

    @Test
    void initCreatesDatabaseAndTableWhenMissing() {
        lenient().doReturn(false).when(infinityClient).checkDatabaseExists();
        lenient().doReturn(false).when(infinityClient).checkTableExists(TABLE);
        lenient().doNothing().when(infinityClient).createDatabase();
        lenient().doNothing().when(infinityClient).createTable(eq(TABLE), any());
        lenient().doNothing().when(infinityClient).createIndex(eq(TABLE), any(), any());
        lenient().doReturn(mockEmbeddingStrings()).when(embeddings).embedQuery(any(), eq(1));

        infinityService.init(embeddings);

        verify(infinityClient).createDatabase();
        verify(infinityClient).createTable(eq(TABLE), any());
        verify(infinityClient).createIndex(eq(TABLE), any(), any());
    }

    @Test
    void initSkipsWhenAlreadyPresent() {
        doReturn(true).when(infinityClient).checkDatabaseExists();
        doReturn(true).when(infinityClient).checkTableExists(TABLE);

        infinityService.init(embeddings);

        verify(infinityClient, never()).createDatabase();
        verify(infinityClient, never()).createTable(any(), any());
    }

    @Test
    void initUsesEmbeddingToDetermineDimension() {
        InfinityParam param = new InfinityParam();
        param.getInitParam().setFieldEmbeddingsDimension(0);
        infinityService = new InfinityService(TABLE, infinityClient, param);

        doReturn(false).when(infinityClient).checkDatabaseExists();
        doReturn(false).when(infinityClient).checkTableExists(TABLE);
        doReturn(mockEmbeddingStrings()).when(embeddings).embedQuery(any(), eq(1));

        InfinityParam.InitParam initParam = param.getInitParam();

        infinityService.init(embeddings);

        assertTrue(initParam.getFieldEmbeddingsDimension() > 0);
    }

    @Test
    void initThrowsInfinityExceptionWhenClientFails() {
        doThrow(new RuntimeException("network"))
            .when(infinityClient).checkDatabaseExists();

        assertThrows(InfinityException.class, () -> infinityService.init(embeddings));
    }

    @Test
    void addDocumentsConvertsDocumentsAndCallsInsert() {
        List<Document> documents = createDocuments();
        doNothing().when(infinityClient).insert(eq(TABLE), any());

        infinityService.addDocuments(documents);

        ArgumentCaptor<InfinityInsertRequest> captor = ArgumentCaptor.forClass(InfinityInsertRequest.class);
        verify(infinityClient).insert(eq(TABLE), captor.capture());
        assertEquals(2, captor.getValue().getData().size());
    }

    @Test
    void addDocumentsReturnsWhenEmpty() {
        infinityService.addDocuments(Collections.emptyList());
        verify(infinityClient, never()).insert(any(), any());
    }

    @Test
    void similaritySearchThrowsWhenClientErrors() {
        doThrow(new InfinityException("fail")).when(infinityClient).search(eq(TABLE), any());

        List<Double> queryVector = Collections.singletonList(0.1d);
        assertThrows(InfinityException.class, () -> infinityService.similaritySearch(queryVector, 1, null));
    }

    @Test
    void similaritySearchReturnsDocuments() {
        doReturn(createSearchResponse()).when(infinityClient).search(eq(TABLE), any());

        List<Double> queryVector = Collections.singletonList(0.1d);
        List<Document> results = infinityService.similaritySearch(queryVector, 1, 0.5d);

        assertEquals(1, results.size());
        assertEquals("doc1", results.get(0).getUniqueId());
    }

    @Test
    void similaritySearchReturnsEmptyWhenVectorEmpty() {
        List<Document> result = infinityService.similaritySearch(Collections.emptyList(), 1, null);
        assertTrue(result.isEmpty());
        verify(infinityClient, never()).search(any(), any());
    }

    @Test
    void deleteDocumentsCallsClient() {
        doNothing().when(infinityClient).delete(eq(TABLE), any());

        infinityService.deleteDocuments(Arrays.asList("id1", "id2"));

        verify(infinityClient).delete(eq(TABLE), any());
    }

    @Test
    void deleteDocumentsSkipsWhenEmpty() {
        infinityService.deleteDocuments(Collections.emptyList());
        verify(infinityClient, never()).delete(any(), any());
    }

    @Test
    void closeDelegatesToClient() {
        doNothing().when(infinityClient).close();

        assertDoesNotThrow(infinityService::close);
        verify(infinityClient).close();
    }

    private List<String> mockEmbeddingStrings() {
        return Collections.singletonList("[0.1,0.2,0.3]");
    }

    private List<Document> createDocuments() {
        List<Document> documents = new ArrayList<>();
        Document doc = new Document();
        doc.setUniqueId("1");
        doc.setPageContent("content");
        doc.setMetadata(new HashMap<>());
        doc.setEmbedding(Arrays.asList(0.1d, 0.2d));
        documents.add(doc);

        Document doc2 = new Document();
        doc2.setUniqueId("2");
        doc2.setPageContent("content2");
        doc2.setMetadata(new HashMap<>());
        doc2.setEmbedding(Arrays.asList(0.2d, 0.3d));
        documents.add(doc2);
        return documents;
    }

    private InfinitySearchResponse createSearchResponse() {
        InfinitySearchResponse response = new InfinitySearchResponse();
        response.setErrorCode(0);
        InfinitySearchResponse.SearchResult result = new InfinitySearchResponse.SearchResult();
        Map<String, Object> row = new HashMap<>();
        row.put("unique_id", "doc1");
        row.put("page_content", "content");
        row.put("metadata", "{\"key\":\"value\"}");
        result.setRow(row);
        response.setOutput(Collections.singletonList(result));
        return response;
    }
}
