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

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AtlasServiceTest {

    @Mock
    private AtlasClient atlasClient;

    private AtlasService atlasService;
    private AtlasVectorStoreParam param;

    @BeforeEach
    void setUp() {
        param = AtlasVectorStoreParam.builder().build();
        atlasService = new AtlasService(atlasClient, param);
    }

    @Test
    void testAddDocuments() {
        com.alibaba.langengine.core.indexes.Document langEngineDoc = new com.alibaba.langengine.core.indexes.Document();
        String objectIdHex = new ObjectId().toHexString();
        langEngineDoc.setUniqueId(objectIdHex);
        langEngineDoc.setPageContent("test content");
        langEngineDoc.setEmbedding(Arrays.asList(1.0, 2.0, 3.0));
        langEngineDoc.setMetadata(Collections.singletonMap("key", "value"));

        List<com.alibaba.langengine.core.indexes.Document> docs = Collections.singletonList(langEngineDoc);

        atlasService.addDocuments(docs);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(atlasClient).insertDocuments(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("test content", captor.getValue().get(0).getString(param.getTextPath()));
        assertEquals(new ObjectId(objectIdHex), captor.getValue().get(0).getObjectId(param.getUniqueIdPath()));
    }

    @Test
    void testSimilaritySearch() {
        Document mongoDoc = new Document();
        mongoDoc.put(param.getUniqueIdPath(), new ObjectId());
        mongoDoc.put(param.getTextPath(), "search result");
        mongoDoc.put("score", 0.95);
        mongoDoc.put(param.getMetadataPath(), new Document("key", "value"));

        when(atlasClient.similaritySearch(any(), anyInt())).thenReturn(Collections.singletonList(mongoDoc));

        List<com.alibaba.langengine.core.indexes.Document> results = atlasService.similaritySearch(Arrays.asList(1.0f, 2.0f), 1);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("search result", results.get(0).getPageContent());
        assertEquals(0.95, results.get(0).getScore());
        assertNotNull(results.get(0).getMetadata());
        assertEquals("value", results.get(0).getMetadata().get("key"));
    }

    @Test
    void testDeleteDocuments() {
        List<String> ids = Collections.singletonList(new ObjectId().toHexString());
        atlasService.deleteDocuments(ids);
        verify(atlasClient).deleteDocuments(ids);
    }

    @Test
    void testClose() {
        atlasService.close();
        verify(atlasClient).close();
    }
}