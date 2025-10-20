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
package com.alibaba.langengine.turbopuffer.vectorstore;

import com.alibaba.fastjson.JSON;
import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.core.vectorstore.VectorStore;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.Map;

import java.util.List;

import static com.alibaba.langengine.turbopuffer.TurbopufferConfiguration.TURBOPUFFER_API_KEY;
import static com.alibaba.langengine.turbopuffer.TurbopufferConfiguration.TURBOPUFFER_SERVER_URL;


@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class Turbopuffer extends VectorStore {

    /**
     * embedding模型
     */
    private Embeddings embedding;

    /**
     * 命名空间名称
     */
    private final String namespace;

    private final TurbopufferService turbopufferService;

    public Turbopuffer(String namespace) {
        this(namespace, null);
    }

    public Turbopuffer(String namespace, TurbopufferParam turbopufferParam) {
        this.namespace = namespace;
        String apiKey = TURBOPUFFER_API_KEY;
        String serverUrl = TURBOPUFFER_SERVER_URL;
        
        if (turbopufferParam == null) {
            turbopufferParam = new TurbopufferParam();
        }
        
        turbopufferService = new TurbopufferService(apiKey, serverUrl, namespace, turbopufferParam);
    }

    public Turbopuffer(String apiKey, String serverUrl, String namespace, TurbopufferParam turbopufferParam) {
        this.namespace = namespace;
        
        if (turbopufferParam == null) {
            turbopufferParam = new TurbopufferParam();
        }
        
        turbopufferService = new TurbopufferService(apiKey, serverUrl, namespace, turbopufferParam);
    }

    /**
     * 初始化会在Namespace不存在的情况下创建Turbopuffer的Namespace
     * 1. 根据embedding模型结果维度设置向量维度
     * 2. 配置向量字段和属性字段
     * 3. 设置距离度量类型
     * 如果需要自定义Namespace，请提前创建并配置相应的参数
     */
    public void init() {
        try {
            turbopufferService.init(embedding);
        } catch (Exception e) {
            log.error("init turbopuffer failed", e);
            throw new TurbopufferException("Failed to initialize Turbopuffer", e);
        }
    }

    @Override
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        documents = embedding.embedDocument(documents);
        turbopufferService.addDocumentsDirectly(documents);
    }
    
    /**
     * 异步添加文档
     */
    public void addDocumentsAsync(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        documents = embedding.embedDocument(documents);
        for (Document doc : documents) {
            turbopufferService.addDocumentAsync(doc);
        }
    }
    
    /**
     * 批量添加文档（使用批量处理器）
     */
    public void addDocumentsBatch(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        documents = embedding.embedDocument(documents);
        turbopufferService.addDocuments(documents);
    }

    @Override
    public List<Document> similaritySearch(String query, int k, Double maxDistanceValue, Integer type) {
        List<String> embeddingStrings = embedding.embedQuery(query, k);
        if (CollectionUtils.isEmpty(embeddingStrings) || !embeddingStrings.get(0).startsWith("[")) {
            return Lists.newArrayList();
        }
        List<Double> queryEmbedding = JSON.parseArray(embeddingStrings.get(0), Double.class);
        return turbopufferService.similaritySearchWithRetry(queryEmbedding, k, maxDistanceValue);
    }

    @Override
    public List<Document> similaritySearch(String query, int k) {
        return similaritySearch(query, k, null, null);
    }

    public List<Document> similaritySearchByVector(List<Double> embedding, int k) {
        return turbopufferService.similaritySearch(embedding, k, null);
    }

    /**
     * 删除向量
     *
     * @param ids 向量ID列表
     */
    public void deleteVectors(List<String> ids) {
        turbopufferService.deleteVectors(ids);
    }

    /**
     * 删除命名空间
     */
    public void deleteNamespace() {
        turbopufferService.deleteNamespace();
    }

    /**
     * 强制刷新批量队列
     */
    public void flushBatch() {
        turbopufferService.flushBatch();
    }
    
    /**
     * 获取性能统计信息
     */
    public Map<String, Object> getMetrics() {
        return turbopufferService.getMetrics();
    }
    
    /**
     * 重置性能指标
     */
    public void resetMetrics() {
        turbopufferService.resetMetrics();
    }

    /**
     * 关闭连接
     */
    public void close() {
        turbopufferService.close();
    }

    /**
     * 获取命名空间名称
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 设置embedding模型
     */
    public void setEmbedding(Embeddings embedding) {
        this.embedding = embedding;
    }

}
