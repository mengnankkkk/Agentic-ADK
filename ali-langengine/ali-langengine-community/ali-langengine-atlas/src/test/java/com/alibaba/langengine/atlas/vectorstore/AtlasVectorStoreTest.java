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
package com.alibaba.langengine.atlas.vectorstore;

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AtlasVectorStoreTest {

    @Mock
    private AtlasService atlasService;

    @Mock
    private Embeddings embeddings;

    private AtlasVectorStore atlasVectorStore;

    @BeforeEach
    void setUp() {
        AtlasVectorStoreParam param = AtlasVectorStoreParam.builder().build();
        atlasVectorStore = new AtlasVectorStore(atlasService, param);
        atlasVectorStore.setEmbedding(embeddings);
    }

    @Test
    void testAddDocumentsWithEmbedding() {
        Document doc = new Document();
        doc.setPageContent("test");
        List<Document> docs = Collections.singletonList(doc);

        Document docWithEmbedding = new Document();
        docWithEmbedding.setPageContent("test");
        docWithEmbedding.setEmbedding(Arrays.asList(1.0, 2.0));
        List<Document> embeddedDocs = Collections.singletonList(docWithEmbedding);

        when(embeddings.embedDocument(docs)).thenReturn(embeddedDocs);

        atlasVectorStore.addDocuments(docs);

        verify(atlasService).addDocuments(embeddedDocs);
    }

    @Test
    void testAddDocumentsWithoutEmbeddingService() {
        atlasVectorStore.setEmbedding(null);

        Document doc = new Document();
        doc.setPageContent("test");
        doc.setEmbedding(Arrays.asList(1.0, 2.0));
        List<Document> docs = Collections.singletonList(doc);

        atlasVectorStore.addDocuments(docs);

        verify(embeddings, never()).embedDocument(any());
        verify(atlasService).addDocuments(docs);
    }

    @Test
    void testSimilaritySearch() {
        String query = "test query";
        List<Double> queryEmbedding = Arrays.asList(1.0, 2.0, 3.0);
        List<Float> floatEmbedding = Arrays.asList(1.0f, 2.0f, 3.0f);

        when(embeddings.embedQuery(query)).thenReturn(queryEmbedding);

        atlasVectorStore.similaritySearch(query, 1, null, null);

        verify(atlasService).similaritySearch(floatEmbedding, 1);
    }

    @Test
    void testDeleteDocuments() {
        List<String> ids = Collections.singletonList("some-id");
        atlasVectorStore.deleteDocuments(ids);
        verify(atlasService).deleteDocuments(ids);
    }

    @Test
    void testClose() {
        atlasVectorStore.close();
        verify(atlasService).close();
    }
}