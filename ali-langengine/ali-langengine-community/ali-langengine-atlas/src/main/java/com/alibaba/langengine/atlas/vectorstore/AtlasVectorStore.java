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
import com.alibaba.langengine.core.vectorstore.VectorStore;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class AtlasVectorStore extends VectorStore {

    private Embeddings embedding;
    private final AtlasService atlasService;
    private final AtlasVectorStoreParam param;

    public AtlasVectorStore(String connectionString, String databaseName, String collectionName, String indexName, AtlasVectorStoreParam param) {
        this(new AtlasService(connectionString, databaseName, collectionName, indexName, param), param);
    }

    /**
     * Constructor for testing purposes.
     */
    AtlasVectorStore(AtlasService atlasService, AtlasVectorStoreParam param) {
        this.atlasService = atlasService;
        this.param = param;
    }

    @Override
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        try {
            List<Document> documentsWithEmbeddings = documents;
            if (embedding != null) {
                documentsWithEmbeddings = embedding.embedDocument(documents);
            }
            atlasService.addDocuments(documentsWithEmbeddings);
        } catch (Exception e) {
            throw new AtlasVectorStoreException("ADD_DOCUMENTS_FAILED", "Failed to add documents to Atlas.", e);
        }
    }

    @Override
    public List<Document> similaritySearch(String query, int k, Double maxDistanceValue, Integer type) {
        if (embedding == null) {
            log.warn("No embedding model provided for similarity search.");
            return Lists.newArrayList();
        }

        try {
            List<Double> queryEmbedding = embedding.embedQuery(query);
            if (CollectionUtils.isEmpty(queryEmbedding)) {
                log.warn("Generated empty embedding for query: {}", query);
                return Lists.newArrayList();
            }
            List<Float> floatEmbedding = queryEmbedding.stream().map(Double::floatValue).collect(Collectors.toList());
            return atlasService.similaritySearch(floatEmbedding, k);
        } catch (Exception e) {
            throw new AtlasVectorStoreException("SIMILARITY_SEARCH_FAILED", "Failed to perform similarity search in Atlas.", e);
        }
    }

    public void deleteDocuments(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        try {
            atlasService.deleteDocuments(ids);
        } catch (Exception e) {
            throw new AtlasVectorStoreException("DELETE_DOCUMENTS_FAILED", "Failed to delete documents from Atlas.", e);
        }
    }

    public void close() {
        if (atlasService != null) {
            atlasService.close();
        }
    }
}