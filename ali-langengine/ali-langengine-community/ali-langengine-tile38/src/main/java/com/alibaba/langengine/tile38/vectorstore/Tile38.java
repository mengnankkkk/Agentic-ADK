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
package com.alibaba.langengine.tile38.vectorstore;

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.core.vectorstore.VectorStore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;

import static com.alibaba.langengine.tile38.Tile38Configuration.*;


@Slf4j
@Data
@EqualsAndHashCode(callSuper=false)
public class Tile38 extends VectorStore {

    /**
     * 向量库的embedding
     */
    private Embeddings embedding;

    /**
     * 标识一个唯一的仓库，可以看做是某个业务，向量内容的集合标识；某个知识库内容，这个知识库所有的内容都应该是相同的collectionName
     */
    private String collectionName;

    /**
     * 内部使用的client，不希望对外暴露
     */
    private Tile38Client _client;

    /**
     * 内部使用的service，不希望对外暴露
     */
    private Tile38Service _service;

    public Tile38(Embeddings embedding, String collectionName) {
        this.collectionName = collectionName == null ? UUID.randomUUID().toString() : collectionName;
        this.embedding = embedding;
        
        Tile38Param param = Tile38Param.builder()
                .host(TILE38_HOST)
                .port(TILE38_PORT)
                .password(TILE38_PASSWORD)
                .timeout(TILE38_TIMEOUT)
                .collectionName(this.collectionName)
                .build();
                
        this._client = new Tile38Client(param);
        this._service = new Tile38Service(_client, this.collectionName, param.getBatchSize(), param.getMaxResultSize());
    }

    public Tile38(Tile38Param param, Embeddings embedding) {
        this.collectionName = param.getCollectionName() == null ? 
            UUID.randomUUID().toString() : param.getCollectionName();
        this.embedding = embedding;
        this._client = new Tile38Client(param);
        this._service = new Tile38Service(_client, this.collectionName, param.getBatchSize(), param.getMaxResultSize());
    }

    /**
     * 添加文本向量，如果没有向量，系统会自动的使用embedding生成向量
     *
     * @param documents
     */
    @Override
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        try {
            // Generate embeddings for documents that don't have them
            for (Document document : documents) {
                if (StringUtils.isEmpty(document.getUniqueId())) {
                    document.setUniqueId(UUID.randomUUID().toString());
                }
                
                if (StringUtils.isEmpty(document.getPageContent())) {
                    continue;
                }

                // Generate embedding if not present
                if (CollectionUtils.isEmpty(document.getEmbedding()) && embedding != null) {
                    List<Document> embeddedDocs = embedding.embedTexts(List.of(document.getPageContent()));
                    if (CollectionUtils.isNotEmpty(embeddedDocs)) {
                        document.setEmbedding(embeddedDocs.get(0).getEmbedding());
                    }
                }
            }

            _service.addDocuments(documents);
        } catch (Exception e) {
            log.error("Failed to add documents to Tile38", e);
            throw new Tile38Exception("ADD_DOCUMENTS_ERROR", "Failed to add documents", e);
        }
    }

    /**
     * Tile38向量库查询
     *
     * @param query
     * @param k
     * @param maxDistanceValue
     * @param type
     * @return
     */
    @Override
    public List<Document> similaritySearch(String query, int k, Double maxDistanceValue, Integer type) {
        try {
            // Generate embedding for query if embedding service is available
            if (embedding != null) {
                List<Document> queryDocs = embedding.embedTexts(List.of(query));
                if (CollectionUtils.isNotEmpty(queryDocs)) {
                    // Use the embedding for more sophisticated search
                    // For now, we'll use the basic similarity search
                }
            }

            return _service.similaritySearch(query, k, maxDistanceValue);
        } catch (Exception e) {
            log.error("Failed to perform similarity search in Tile38", e);
            throw new Tile38Exception("SIMILARITY_SEARCH_ERROR", "Failed to perform similarity search", e);
        }
    }

    /**
     * Search nearby documents based on coordinates
     *
     * @param lat latitude
     * @param lon longitude
     * @param k number of results
     * @return list of documents
     */
    public List<Document> nearbySearch(double lat, double lon, int k) {
        try {
            return _service.nearbySearch(lat, lon, k);
        } catch (Exception e) {
            log.error("Failed to perform nearby search in Tile38", e);
            throw new Tile38Exception("NEARBY_SEARCH_ERROR", "Failed to perform nearby search", e);
        }
    }

    /**
     * Delete document by id
     *
     * @param id document id
     */
    public void deleteDocument(String id) {
        try {
            _service.deleteDocument(id);
        } catch (Exception e) {
            log.error("Failed to delete document from Tile38", e);
            throw new Tile38Exception("DELETE_DOCUMENT_ERROR", "Failed to delete document", e);
        }
    }

    /**
     * Drop the entire collection
     */
    public void dropCollection() {
        try {
            _service.dropCollection();
        } catch (Exception e) {
            log.error("Failed to drop collection from Tile38", e);
            throw new Tile38Exception("DROP_COLLECTION_ERROR", "Failed to drop collection", e);
        }
    }

    /**
     * Batch add documents using parallel processing
     */
    public void batchAddDocuments(List<Document> documents) {
        try {
            _service.batchAddDocuments(documents);
        } catch (Exception e) {
            log.error("Failed to batch add documents to Tile38", e);
            throw new Tile38Exception("BATCH_ADD_ERROR", "Failed to batch add documents", e);
        }
    }

    /**
     * Batch delete documents
     */
    public void batchDeleteDocuments(List<String> documentIds) {
        try {
            _service.batchDeleteDocuments(documentIds);
        } catch (Exception e) {
            log.error("Failed to batch delete documents from Tile38", e);
            throw new Tile38Exception("BATCH_DELETE_ERROR", "Failed to batch delete documents", e);
        }
    }

    /**
     * Close the Tile38 client connection
     */
    public void close() {
        if (_service != null) {
            _service.shutdown();
        }
        if (_client != null) {
            _client.close();
        }
    }

}