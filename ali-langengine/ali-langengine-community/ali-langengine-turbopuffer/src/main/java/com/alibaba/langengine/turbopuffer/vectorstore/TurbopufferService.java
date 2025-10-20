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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.turbopuffer.client.TurbopufferClient;
import com.turbopuffer.client.okhttp.TurbopufferOkHttpClient;
import com.turbopuffer.models.namespaces.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
@Data
public class TurbopufferService {

    private String namespace;
    private TurbopufferParam turbopufferParam;
    private final TurbopufferClient turbopufferClient;
    private final CircuitBreaker circuitBreaker;
    private final RetryHelper retryHelper;
    private final BatchProcessor batchProcessor;
    
    // 性能监控
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final Map<String, AtomicLong> operationMetrics = new ConcurrentHashMap<>();

    public TurbopufferService(String apiKey, String serverUrl, String namespace, TurbopufferParam turbopufferParam) {
        // 参数验证
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalArgumentException("Turbopuffer API key cannot be null or empty");
        }
        if (StringUtils.isBlank(namespace)) {
            throw new IllegalArgumentException("Turbopuffer namespace cannot be null or empty");
        }
        
        this.namespace = namespace;
        this.turbopufferParam = turbopufferParam != null ? turbopufferParam : new TurbopufferParam();
        
        try {
            // 根据官方文档创建 Turbopuffer 客户端，支持更多配置选项
            TurbopufferOkHttpClient.Builder clientBuilder = TurbopufferOkHttpClient.builder()
                    .apiKey(apiKey);
            
            // 如果提供了自定义服务器URL，设置baseUrl
            if (StringUtils.isNotBlank(serverUrl) && !serverUrl.equals("https://api.turbopuffer.com")) {
                clientBuilder.baseUrl(serverUrl);
            }
            
            // 设置超时配置
            if (this.turbopufferParam.initParam.requestTimeoutMs > 0) {
                clientBuilder.timeout(java.time.Duration.ofMillis(this.turbopufferParam.initParam.requestTimeoutMs));
            }
            
            // 设置重试次数
            if (this.turbopufferParam.initParam.maxRetries > 0) {
                clientBuilder.maxRetries(this.turbopufferParam.initParam.maxRetries);
            }
            
            this.turbopufferClient = clientBuilder.build();
            
            // 初始化高级功能组件
            this.circuitBreaker = new CircuitBreaker(this.turbopufferParam.initParam.circuitBreaker);
            this.retryHelper = new RetryHelper(
                    this.turbopufferParam.initParam.maxRetries,
                    1000, // baseDelayMs
                    30000, // maxDelayMs
                    0.1 // jitterFactor
            );
            this.batchProcessor = new BatchProcessor(
                    this.turbopufferParam.initParam.batch,
                    this::processBatch
            );
            
            log.info("TurbopufferService initialized with advanced features: namespace={}, batchSize={}, circuitBreaker={}", 
                    namespace, 
                    this.turbopufferParam.initParam.batch.batchSize,
                    this.turbopufferParam.initParam.circuitBreaker.enabled);
            
        } catch (Exception e) {
            log.error("Failed to create Turbopuffer client", e);
            throw new TurbopufferException.TurbopufferConnectionException("Failed to create Turbopuffer client: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化命名空间
     */
    public void init(Embeddings embedding) {
        try {
            // 如果向量维度未设置，通过embedding获取  
            if (turbopufferParam.initParam.fieldEmbeddingsDimension == 0) {
                List<String> sampleEmbedding = embedding.embedQuery("sample", 1);
                if (CollectionUtils.isNotEmpty(sampleEmbedding) && sampleEmbedding.get(0).startsWith("[")) {
                    List<Double> embedding_doubles = JSON.parseArray(sampleEmbedding.get(0), Double.class);
                    turbopufferParam.initParam.fieldEmbeddingsDimension = embedding_doubles.size();
                    log.info("Auto-detected embedding dimension: {}", embedding_doubles.size());
                }
            }
            
            log.info("Turbopuffer namespace '{}' initialized successfully", namespace);
        } catch (Exception e) {
            log.error("Failed to initialize Turbopuffer namespace: {}", namespace, e);
            throw new TurbopufferException.TurbopufferNamespaceException("Failed to initialize namespace: " + namespace, e);
        }
    }


    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        try {
            log.warn("addDocuments not fully implemented - Mock mode");
            // TODO: Implement when turbopuffer SDK API is clarified
            // Current SDK version does not support row-based writes API

        } catch (Exception e) {
            log.error("Failed to add documents to Turbopuffer", e);
            throw new TurbopufferException.TurbopufferApiException("Failed to add documents", e);
        }
    }

    /**
     * 相似度搜索 - 根据官方API文档实现
     */
    public List<Document> similaritySearch(List<Double> queryEmbedding, int topK, Double maxDistanceValue) {
        try {
            // 根据官方文档的查询API
            NamespaceQueryParams params = NamespaceQueryParams.builder()
                    .rankBy(RankBy.vector("vector", convertToFloatList(queryEmbedding)))
                    .topK(topK)
                    .includeAttributes(true)
                    .build();

            NamespaceQueryResponse result = turbopufferClient.namespace(namespace).query(params);
            
            return convertToDocuments(result);
            
        } catch (Exception e) {
            log.error("Failed to perform similarity search in Turbopuffer", e);
            throw new TurbopufferException.TurbopufferApiException("Failed to perform similarity search", e);
        }
    }

    /**
     * 删除向量 - 根据官方API文档实现
     */
    public void deleteVectors(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        try {
            // 根据官方文档使用deletes字段删除文档
            NamespaceWriteParams params = NamespaceWriteParams.builder()
                    .deletes(ids)
                    .build();
                    
            NamespaceWriteResponse response = turbopufferClient.namespace(namespace).write(params);
            log.info("Successfully deleted {} documents from namespace: {}", ids.size(), namespace);
        } catch (Exception e) {
            log.error("Failed to delete vectors from Turbopuffer", e);
            throw new TurbopufferException.TurbopufferApiException("Failed to delete vectors", e);
        }
    }

    /**
     * 删除命名空间 - 暂时不实现，等SDK API明确
     */
    public void deleteNamespace() {
        try {
            // 暂时使用简单的方式，等找到正确的Filter API
            log.warn("Namespace deletion not fully implemented due to SDK API limitations");
        } catch (Exception e) {
            log.error("Failed to delete namespace: {}", namespace, e);
            throw new TurbopufferException.TurbopufferNamespaceException("Failed to delete namespace: " + namespace, e);
        }
    }



    /**
     * 转换Double列表
     */
    private List<Double> convertToDoubleList(List<Double> doubleList) {
        if (CollectionUtils.isEmpty(doubleList)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(doubleList);
    }
    
    /**
     * 转换 Double 列表为 Float 列表 (Turbopuffer SDK RankBy.vector 需要Float)
     */
    private List<Float> convertToFloatList(List<Double> doubleList) {
        if (CollectionUtils.isEmpty(doubleList)) {
            return new ArrayList<>();
        }
        List<Float> floatList = new ArrayList<>();
        for (Double d : doubleList) {
            floatList.add(d.floatValue());
        }
        return floatList;
    }

    /**
     * 构建属性映射
     */
    private Map<String, Object> buildAttributes(Document document) {
        Map<String, Object> attributes = document.getMetadata();
        if (attributes == null) {
            attributes = Maps.newHashMap();
        }
        
        // 添加内容字段
        attributes.put(turbopufferParam.fieldNamePageContent, document.getPageContent());
        
        return attributes;
    }

    /**
     * 转换查询结果为文档列表 - 根据官方API响应格式
     */
    private List<Document> convertToDocuments(NamespaceQueryResponse result) {
        if (result == null || result.rows().isEmpty()) {
            return Lists.newArrayList();
        }

        List<Document> documents = Lists.newArrayList();
        
        // 处理Optional<List<Row>>类型
        if (result.rows().isPresent()) {
            for (Row row : result.rows().get()) {
                Document document = new Document();
                
                // 根据Turbopuffer SDK提取行数据
                // Row是基于Map构建的，需要通过get方法获取数据
                String rowId = extractStringValue(row, "id");
                document.setUniqueId(rowId);
                
                // 提取属性 - 排除id和vector字段
                Map<String, Object> attributes = extractAttributes(row);
                if (MapUtils.isNotEmpty(attributes)) {
                    String content = (String) attributes.get(turbopufferParam.fieldNamePageContent);
                    if (StringUtils.isNotEmpty(content)) {
                        document.setPageContent(content);
                    }
                    
                    // 创建元数据副本，移除内容字段
                    Map<String, Object> metadata = Maps.newHashMap(attributes);
                    metadata.remove(turbopufferParam.fieldNamePageContent);
                    if (!metadata.isEmpty()) {
                        document.setMetadata(metadata);
                    }
                }
                
                // 设置相似度分数 (距离)
                Double distance = extractDoubleValue(row, "distance");
                if (distance != null) {
                    document.setScore(distance);
                }
                
                documents.add(document);
            }
        }
        
        return documents;
    }
    
    
    /**
     * 添加单个文档 - 异步批量处理
     */
    public CompletableFuture<Void> addDocumentAsync(Document document) {
        return CompletableFuture.runAsync(() -> {
            if (batchProcessor.add(document)) {
                log.debug("Document added to batch queue: {}", document.getUniqueId());
            } else {
                log.warn("Failed to add document to batch queue, processing directly");
                addDocumentDirectly(document);
            }
        });
    }
    
    /**
     * 直接添加单个文档（不使用批量处理）
     */
    public void addDocumentDirectly(Document document) {
        addDocumentsDirectly(Lists.newArrayList(document));
    }
    
    /**
     * 直接处理文档（不使用批量处理，带重试和熔断）
     */
    public void addDocumentsDirectly(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        
        recordOperation("addDocuments");
        
        try {
            retryHelper.executeVoid(() -> {
                if (!circuitBreaker.allowRequest()) {
                    throw new TurbopufferException.TurbopufferApiException("Circuit breaker is open");
                }
                
                doAddDocumentsInternal(documents);
                circuitBreaker.recordSuccess();
                successfulRequests.addAndGet(documents.size());
                
            }, "addDocuments");
            
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            failedRequests.addAndGet(documents.size());
            log.error("Failed to add documents to Turbopuffer after retries", e);
            throw new TurbopufferException.TurbopufferApiException("Failed to add documents", e);
        }
    }
    
    /**
     * 批量处理器回调方法
     */
    private void processBatch(List<Document> batch) {
        addDocumentsDirectly(batch);
    }
    
    /**
     * 执行带重试和熔断的相似度搜索
     */
    public List<Document> similaritySearchWithRetry(List<Double> queryEmbedding, int topK, Double maxDistanceValue) {
        recordOperation("similaritySearch");
        
        try {
            return retryHelper.execute(() -> {
                if (!circuitBreaker.allowRequest()) {
                    throw new TurbopufferException.TurbopufferApiException("Circuit breaker is open");
                }
                
                List<Document> result = doSimilaritySearchInternal(queryEmbedding, topK, maxDistanceValue);
                circuitBreaker.recordSuccess();
                successfulRequests.incrementAndGet();
                return result;
                
            }, "similaritySearch");
            
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            failedRequests.incrementAndGet();
            log.error("Failed to perform similarity search after retries", e);
            throw new TurbopufferException.TurbopufferApiException("Failed to perform similarity search", e);
        }
    }
    
    /**
     * 内部相似度搜索实现
     */
    private List<Document> doSimilaritySearchInternal(List<Double> queryEmbedding, int topK, Double maxDistanceValue) {
        // 根据官方文档的查询API
        NamespaceQueryParams params = NamespaceQueryParams.builder()
                .rankBy(RankBy.vector("vector", convertToFloatList(queryEmbedding)))
                .topK(topK)
                .includeAttributes(true)
                .build();

        NamespaceQueryResponse result = turbopufferClient.namespace(namespace).query(params);
        return convertToDocuments(result);
    }
    
    /**
     * 内部文档添加实现（无重试和熔断）- Mock implementation
     * TODO: Implement actual SDK integration when API is stable
     */
    private void doAddDocumentsInternal(List<Document> documents) {
        log.warn("doAddDocumentsInternal not fully implemented - Mock mode");
        // TODO: Implement when turbopuffer SDK API is clarified
    }
    
    /**
     * 强制刷新批量队列
     */
    public void flushBatch() {
        if (batchProcessor != null) {
            batchProcessor.flush();
            log.info("Batch queue flushed");
        }
    }
    
    /**
     * 关闭服务，清理资源
     */
    public void close() {
        try {
            if (batchProcessor != null) {
                batchProcessor.stop();
            }
            log.info("TurbopufferService closed successfully");
        } catch (Exception e) {
            log.error("Error closing TurbopufferService", e);
        }
    }
    
    /**
     * 记录操作指标
     */
    private void recordOperation(String operation) {
        totalRequests.incrementAndGet();
        operationMetrics.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 获取性能统计信息
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = Maps.newHashMap();
        metrics.put("totalRequests", totalRequests.get());
        metrics.put("successfulRequests", successfulRequests.get());
        metrics.put("failedRequests", failedRequests.get());
        metrics.put("circuitBreakerState", circuitBreaker.getState().name());
        metrics.put("circuitBreakerFailures", circuitBreaker.getFailureCount());
        metrics.put("batchQueueSize", batchProcessor != null ? batchProcessor.getQueueSize() : 0);
        metrics.put("batchProcessorRunning", batchProcessor != null ? batchProcessor.isRunning() : false);
        
        Map<String, Long> operations = Maps.newHashMap();
        operationMetrics.forEach((key, value) -> operations.put(key, value.get()));
        metrics.put("operationCounts", operations);
        
        return metrics;
    }
    
    /**
     * 重置指标
     */
    public void resetMetrics() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        operationMetrics.clear();
        circuitBreaker.reset();
        log.info("Metrics reset");
    }

    /**
     * 从Row对象中提取字符串值
     */
    private String extractStringValue(Row row, String key) {
        try {
            // Row对象基于Map构建，尝试通过反射或JSON方式获取值
            // 由于Row的内部结构不明，这里使用toString解析的方式
            String rowStr = row.toString();
            // 简单的字符串解析方式获取id值
            if (rowStr.contains("\"" + key + "\"")) {
                int startIndex = rowStr.indexOf("\"" + key + "\":\"") + key.length() + 4;
                int endIndex = rowStr.indexOf("\"", startIndex);
                if (startIndex > 0 && endIndex > startIndex) {
                    return rowStr.substring(startIndex, endIndex);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract {} from Row: {}", key, e.getMessage());
        }
        return null;
    }
    
    /**
     * 从Row对象中提取Double值
     */
    private Double extractDoubleValue(Row row, String key) {
        try {
            String rowStr = row.toString();
            if (rowStr.contains("\"" + key + "\":")) {
                int startIndex = rowStr.indexOf("\"" + key + "\":") + key.length() + 3;
                int endIndex = Math.min(rowStr.indexOf(",", startIndex), rowStr.indexOf("}", startIndex));
                if (endIndex == -1) endIndex = rowStr.length();
                if (startIndex > 0 && endIndex > startIndex) {
                    String valueStr = rowStr.substring(startIndex, endIndex).trim();
                    return Double.parseDouble(valueStr);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract {} from Row: {}", key, e.getMessage());
        }
        return null;
    }
    
    /**
     * 从Row对象中提取属性Map，排除系统字段
     */
    private Map<String, Object> extractAttributes(Row row) {
        Map<String, Object> attributes = Maps.newHashMap();
        try {
            // 这是一个简化的实现，实际应该根据Row的API文档进行调整
            String rowStr = row.toString();
            // 解析JSON字符串获取属性（排除id和vector字段）
            if (rowStr.contains("{") && rowStr.contains("}")) {
                // 简单的JSON解析逻辑
                // 在实际使用中应该使用proper JSON库解析
                Map<String, Object> allData = JSON.parseObject(rowStr, Map.class);
                for (Map.Entry<String, Object> entry : allData.entrySet()) {
                    String key = entry.getKey();
                    if (!"id".equals(key) && !"vector".equals(key) && !"distance".equals(key)) {
                        attributes.put(key, entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract attributes from Row: {}", e.getMessage());
        }
        return attributes;
    }
}
