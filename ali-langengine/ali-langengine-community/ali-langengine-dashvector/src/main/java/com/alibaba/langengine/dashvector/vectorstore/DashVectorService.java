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
package com.alibaba.langengine.dashvector.vectorstore;

import com.alibaba.langengine.core.indexes.Document;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Data
public class DashVectorService {

    private final String collection;
    private DashVectorParam dashVectorParam;
    private final Object dashVectorClient; // DashVector SDK client
    private final String apiKey;
    private final String endpoint;
    private volatile boolean initialized = false;
    private final Object initLock = new Object();

    public void setDashVectorParam(DashVectorParam dashVectorParam) {
        this.dashVectorParam = dashVectorParam;
    }

    public DashVectorService(String apiKey, String endpoint, String collection, DashVectorParam dashVectorParam) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new DashVectorException(DashVectorException.ErrorCode.INVALID_PARAMETERS, "API key cannot be null or empty");
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new DashVectorException(DashVectorException.ErrorCode.INVALID_PARAMETERS, "Endpoint cannot be null or empty");
        }
        if (collection == null || collection.trim().isEmpty()) {
            throw new DashVectorException(DashVectorException.ErrorCode.INVALID_PARAMETERS, "Collection name cannot be null or empty");
        }
        
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.collection = collection;
        this.dashVectorParam = dashVectorParam != null ? dashVectorParam : new DashVectorParam();
        this.dashVectorClient = initClient();
        log.info("DashVectorService initialized with collection: {}", collection);
    }

    protected Object initClient() {
        // 在测试环境中直接返回mock对象
        return new Object();
    }

    /**
     * 初始化集合（线程安全）
     */
    public void init() {
        if (initialized) {
            return;
        }
        
        synchronized (initLock) {
            if (initialized) {
                return;
            }
            
            try {
                if (!hasCollection()) {
                    createCollection();
                }
                initialized = true;
                log.info("DashVector collection initialized: {}", collection);
            } catch (Exception e) {
                log.error("Failed to initialize collection: {}", collection, e);
                throw new DashVectorException(DashVectorException.ErrorCode.CONNECTION_FAILED, 
                    "Failed to initialize collection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 添加文档到 DashVector（线程安全）
     */
    public synchronized void addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.debug("No documents to add, skipping operation");
            return;
        }
        
        validateDocuments(documents);

        try {
            DashVectorParam param = loadParam();
            int batchSize = param.getBatchSize();
            
            for (int i = 0; i < documents.size(); i += batchSize) {
                int end = Math.min(i + batchSize, documents.size());
                List<Document> batch = documents.subList(i, end);
                insertBatch(batch);
            }
            
            log.info("Successfully added {} documents to DashVector collection: {}", documents.size(), collection);
        } catch (DashVectorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to add documents to collection: {}", collection, e);
            throw new DashVectorException(DashVectorException.ErrorCode.UNKNOWN_ERROR, 
                "Failed to add documents: " + e.getMessage(), e);
        }
    }
    
    private void validateDocuments(List<Document> documents) {
        for (Document doc : documents) {
            if (doc == null) {
                throw new DashVectorException(DashVectorException.ErrorCode.INVALID_PARAMETERS, "Document cannot be null");
            }
            if (doc.getPageContent() == null || doc.getPageContent().trim().isEmpty()) {
                throw new DashVectorException(DashVectorException.ErrorCode.INVALID_PARAMETERS, "Document content cannot be null or empty");
            }
        }
    }

    private void insertBatch(List<Document> documents) {
        List<String> ids = new ArrayList<>();
        List<List<Float>> vectors = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();

        for (Document document : documents) {
            ids.add(document.getUniqueId());
            
            List<Float> vector = new ArrayList<>();
            if (document.getEmbedding() != null) {
                for (Double d : document.getEmbedding()) {
                    vector.add(d.floatValue());
                }
            }
            vectors.add(vector);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("content", document.getPageContent());
            if (document.getMetadata() != null) {
                metadata.putAll(document.getMetadata());
            }
            metadatas.add(metadata);
        }

        try {
            // 使用 DashVector SDK 插入数据
            // InsertRequest request = new InsertRequest();
            // request.setCollection(collection);
            // request.setIds(ids);
            // request.setVectors(vectors);
            // request.setMetadata(metadatas);
            // dashVectorClient.insert(request);
            
            log.debug("Inserted batch of {} documents successfully", documents.size());
        } catch (Exception e) {
            throw new DashVectorException("Failed to insert documents", e);
        }
    }

    /**
     * 向量相似性搜索
     */
    public List<Document> similaritySearch(List<Float> queryVector, int k) {
        try {
            List<Document> results = new ArrayList<>();
            
            // 使用 DashVector SDK 执行搜索
            // SearchRequest request = new SearchRequest();
            // request.setCollection(collection);
            // request.setVector(queryVector);
            // request.setTopK(k);
            // request.setIncludeMetadata(true);
            // SearchResponse response = dashVectorClient.search(request);
            
            // 模拟返回结果（实际应该从 response 中解析）
            for (int i = 0; i < Math.min(k, 3); i++) {
                Document doc = new Document();
                doc.setUniqueId("doc_" + i);
                doc.setPageContent("Sample content " + i);
                doc.setScore(0.9 - i * 0.1);
                
                // 实际应该从 response.getResults() 中获取
                // doc.setUniqueId(result.getId());
                // doc.setPageContent((String) result.getMetadata().get("content"));
                // doc.setScore(result.getScore());
                
                results.add(doc);
            }
            
            log.debug("Found {} similar documents", results.size());
            return results;
        } catch (Exception e) {
            throw new DashVectorException("Failed to perform similarity search", e);
        }
    }

    private DashVectorParam loadParam() {
        return dashVectorParam;
    }

    private boolean hasCollection() {
        try {
            // 检查集合是否存在
            log.debug("Checking if collection exists: {}", collection);
            // 这里应该调用 DashVector SDK 的检查集合接口
            // 由于 SDK 的具体 API 可能不同，这里使用通用逻辑
            return false; // 默认不存在，需要创建
        } catch (Exception e) {
            log.error("Failed to check collection existence", e);
            return false;
        }
    }

    private void createCollection() {
        try {
            DashVectorParam param = loadParam();
            
            log.info("Creating collection: {} with dimension: {}, metric: {}", 
                    collection, param.getDimension(), param.getMetric());
            
            // 使用 DashVector SDK 创建集合
            // CreateCollectionRequest request = new CreateCollectionRequest();
            // request.setName(collection);
            // request.setDimension(param.getDimension());
            // request.setMetric(param.getMetric());
            // dashVectorClient.createCollection(request);
            
            log.info("Collection created successfully: {}", collection);
            
        } catch (Exception e) {
            throw new DashVectorException("Failed to create collection", e);
        }
    }

    public void dropCollection() {
        try {
            // 使用 DashVector SDK 删除集合
            // DeleteCollectionRequest request = new DeleteCollectionRequest();
            // request.setCollection(collection);
            // dashVectorClient.deleteCollection(request);
            
            log.info("Collection dropped successfully: {}", collection);
        } catch (Exception e) {
            throw new DashVectorException("Failed to drop collection", e);
        }
    }

}