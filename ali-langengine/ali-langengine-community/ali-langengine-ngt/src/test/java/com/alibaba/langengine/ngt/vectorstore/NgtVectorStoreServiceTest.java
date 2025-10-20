package com.alibaba.langengine.ngt.vectorstore;

import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.ngt.vectorstore.model.NgtSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NgtVectorStoreServiceTest {

    @Mock
    private NgtVectorStoreClient client;

    private NgtVectorStoreService service;
    private NgtVectorStoreParam param;

    @BeforeEach
    void setUp() {
        param = NgtVectorStoreParam.builder()
                .indexPath("./target/ngt-test")
                .dimension(4)
                .nativeLibraryName("ngt")
                .searchK(5)
                .searchEpsilon(0.2f)
                .searchRadius(-1f)
                .build();
        service = new NgtVectorStoreService("test-index", param, client);
    }

    @Test
    void initShouldInitializeClient() {
        service.init(null);
        verify(client).initialize("test-index", param, 4);
    }

    @Test
    void addDocumentsShouldInsertVectors() {
        service.init(null);
        Document document = new Document();
        document.setEmbedding(List.of(0.1, 0.2, 0.3, 0.4));

        when(client.insert(any(float[].class))).thenReturn(42);

        service.addDocuments(List.of(document));

        verify(client, times(1)).insert(any(float[].class));
        assertTrue(service.getDocumentStore().containsKey(42));
        assertEquals("42", document.getUniqueId());
    }

    @Test
    void addDocumentsShouldThrowWhenNotInitialized() {
        Document document = new Document();
        document.setEmbedding(List.of(0.1, 0.2, 0.3, 0.4));

        assertThrows(NgtVectorStoreException.class, () -> service.addDocuments(List.of(document)));
        verifyNoInteractions(client);
        assertTrue(service.getDocumentStore().isEmpty());
    }

    @Test
    void addDocumentsShouldSkipEmptyEmbeddings() {
        service.init(null);
        Document emptyEmbedding = new Document();
        emptyEmbedding.setEmbedding(List.of());
        Document nullEmbedding = new Document();

        service.addDocuments(List.of(emptyEmbedding, nullEmbedding));

        verify(client, never()).insert(any(float[].class));
        assertTrue(service.getDocumentStore().isEmpty());
    }

    @Test
    void similaritySearchShouldReturnStoredDocuments() {
        service.init(null);
        Document stored = new Document();
        stored.setEmbedding(List.of(0.1, 0.2, 0.3, 0.4));

        when(client.insert(any(float[].class))).thenReturn(1);
        service.addDocuments(List.of(stored));

        when(client.search(any(float[].class), anyInt(), anyFloat(), anyFloat()))
                .thenReturn(List.of(new NgtSearchResult(1, 0.15f)));

        List<Document> results = service.similaritySearch(List.of(0.1, 0.2, 0.3, 0.4), 3, null);

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getUniqueId());
        assertEquals(0.15d, results.get(0).getScore(), 1e-6);
    }

    @Test
    void similaritySearchShouldRespectMaxDistanceFilter() {
        service.init(null);
        Document stored = new Document();
        stored.setEmbedding(List.of(0.1, 0.2, 0.3, 0.4));

        when(client.insert(any(float[].class))).thenReturn(10);
        service.addDocuments(List.of(stored));

        when(client.search(any(float[].class), anyInt(), anyFloat(), anyFloat()))
                .thenReturn(List.of(new NgtSearchResult(10, 0.6f)));

        List<Document> results = service.similaritySearch(List.of(0.1, 0.2, 0.3, 0.4), 3, 0.5d);

        assertTrue(results.isEmpty());
    }

    @Test
    void similaritySearchShouldUseMaxConfiguredTopK() {
        service.init(null);
        Document stored = new Document();
        stored.setEmbedding(List.of(0.1, 0.2, 0.3, 0.4));

        when(client.insert(any(float[].class))).thenReturn(3);
        service.addDocuments(List.of(stored));

        ArgumentCaptor<Integer> topKCaptor = ArgumentCaptor.forClass(Integer.class);
        when(client.search(any(float[].class), topKCaptor.capture(), anyFloat(), anyFloat()))
                .thenReturn(List.of());

        service.similaritySearch(List.of(0.1, 0.2, 0.3, 0.4), 2, null);

        assertEquals(5, topKCaptor.getValue());
    }

    @Test
    void deleteDocumentsShouldInvokeClient() {
        service.init(null);
        Document document = new Document();
        document.setEmbedding(List.of(0.1, 0.2, 0.3, 0.4));
        when(client.insert(any(float[].class))).thenReturn(7);
        service.addDocuments(List.of(document));

        service.deleteDocuments(List.of("7"));

        verify(client).remove(7);
        assertFalse(service.getDocumentStore().containsKey(7));
    }

    @Test
    void deleteDocumentsShouldIgnoreInvalidIds() {
        service.init(null);

        service.deleteDocuments(Arrays.asList("abc", "", null));

        verify(client, never()).remove(anyInt());
    }

    @Test
    void closeShouldReleaseResources() {
        service.init(null);
        Document document = new Document();
        document.setEmbedding(List.of(0.1, 0.2, 0.3, 0.4));
        when(client.insert(any(float[].class))).thenReturn(11);
        service.addDocuments(List.of(document));
        assertFalse(service.getDocumentStore().isEmpty());

        service.close();

        verify(client).close();
        assertTrue(service.getDocumentStore().isEmpty());
        assertThrows(NgtVectorStoreException.class, () -> service.addDocuments(List.of(document)));
    }
}
