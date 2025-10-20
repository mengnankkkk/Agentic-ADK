package com.alibaba.langengine.ngt.vectorstore;

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NgtTest {

    @Mock
    private Embeddings embeddings;

    @Mock
    private NgtVectorStoreService service;

    private Ngt ngt;
    private NgtVectorStoreParam param;

    @BeforeEach
    void setUp() {
        param = NgtVectorStoreParam.builder().dimension(4).build();
        ngt = new Ngt("demo-index", param, service);
        ngt.setEmbedding(embeddings);
    }

    @Test
    void initShouldDelegateToService() {
        ngt.init();
        verify(service).init(embeddings);
    }

    @Test
    void addDocumentsShouldEmbedAndDelegate() {
        List<Document> documents = List.of(new Document());
        List<Document> embedded = List.of(new Document());
        when(embeddings.embedDocument(any())).thenReturn(embedded);

        ngt.addDocuments(documents);

        verify(embeddings).embedDocument(documents);
        verify(service).addDocuments(embedded);
    }

    @Test
    void addDocumentsShouldReturnWhenInputEmpty() {
        ngt.addDocuments(List.of());
        ngt.addDocuments(null);

        verifyNoInteractions(embeddings);
        verifyNoInteractions(service);
    }

    @Test
    void addDocumentsShouldWrapExceptions() {
        List<Document> documents = List.of(new Document());
        when(embeddings.embedDocument(any())).thenThrow(new IllegalStateException("boom"));

        NgtVectorStoreException exception = assertThrows(NgtVectorStoreException.class, () -> ngt.addDocuments(documents));
        assertEquals(NgtVectorStoreException.ErrorCodes.INSERT_FAILED, exception.getErrorCode());
        verify(embeddings).embedDocument(documents);
        verify(service, never()).addDocuments(anyList());
    }

    @Test
    void similaritySearchShouldEmbedQuery() {
        when(embeddings.embedQuery(eq("hello"), anyInt())).thenReturn(List.of("[0.1,0.2,0.3,0.4]"));
        Document document = new Document();
        when(service.similaritySearch(anyList(), anyInt(), isNull())).thenReturn(List.of(document));

        List<Document> result = ngt.similaritySearch("hello", 5, null, null);

        verify(embeddings).embedQuery("hello", 5);
        verify(service).similaritySearch(anyList(), eq(5), isNull());
        assertEquals(1, result.size());
        assertSame(document, result.get(0));
    }

    @Test
    void similaritySearchShouldReturnEmptyWhenEmbeddingEmpty() {
        when(embeddings.embedQuery(eq("hello"), anyInt())).thenReturn(List.of());

        List<Document> result = ngt.similaritySearch("hello", 3, null, null);

        assertTrue(result.isEmpty());
        verify(service, never()).similaritySearch(anyList(), anyInt(), any());
    }

    @Test
    void similaritySearchShouldParseCommaSeparatedEmbedding() {
        when(embeddings.embedQuery(eq("hello"), anyInt())).thenReturn(List.of("0.1, 0.2,0.3"));
        when(service.similaritySearch(anyList(), anyInt(), any())).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Double>> captor = ArgumentCaptor.forClass(List.class);

        ngt.similaritySearch("hello", 4, 0.5, null);

        verify(service).similaritySearch(captor.capture(), eq(4), eq(0.5));
        assertEquals(List.of(0.1d, 0.2d, 0.3d), captor.getValue());
    }

    @Test
    void similaritySearchShouldWrapExceptions() {
        when(embeddings.embedQuery(anyString(), anyInt())).thenThrow(new IllegalArgumentException("fail"));

        NgtVectorStoreException exception = assertThrows(NgtVectorStoreException.class,
                () -> ngt.similaritySearch("hello", 3, null, null));
        assertEquals(NgtVectorStoreException.ErrorCodes.SEARCH_FAILED, exception.getErrorCode());
    }

    @Test
    void similaritySearchByVectorShouldDelegate() {
        List<Double> vector = List.of(0.1, 0.2, 0.3, 0.4);
        ngt.similaritySearchByVector(vector, 3, 0.5);
        verify(service).similaritySearch(vector, 3, 0.5);
    }

    @Test
    void similaritySearchByVectorShouldWrapExceptions() {
        List<Double> vector = List.of(0.1, 0.2, 0.3);
        when(service.similaritySearch(vector, 3, null)).thenThrow(new IllegalStateException("error"));

        NgtVectorStoreException exception = assertThrows(NgtVectorStoreException.class,
                () -> ngt.similaritySearchByVector(vector, 3, null));
        assertEquals(NgtVectorStoreException.ErrorCodes.SEARCH_FAILED, exception.getErrorCode());
    }

    @Test
    void deleteShouldDelegate() {
        List<String> ids = List.of("1", "2");
        ngt.delete(ids);
        verify(service).deleteDocuments(ids);
    }

    @Test
    void deleteShouldWrapExceptions() {
        List<String> ids = List.of("1");
        doThrow(new IllegalStateException("fail")).when(service).deleteDocuments(ids);

        NgtVectorStoreException exception = assertThrows(NgtVectorStoreException.class, () -> ngt.delete(ids));
        assertEquals(NgtVectorStoreException.ErrorCodes.DELETE_FAILED, exception.getErrorCode());
    }

    @Test
    void closeShouldDelegate() {
        ngt.close();
        verify(service).close();
    }

    @Test
    void closeShouldNotThrowWhenServiceFails() {
        doThrow(new IllegalStateException("fail")).when(service).close();

        assertDoesNotThrow(() -> ngt.close());
        verify(service).close();
    }
}
