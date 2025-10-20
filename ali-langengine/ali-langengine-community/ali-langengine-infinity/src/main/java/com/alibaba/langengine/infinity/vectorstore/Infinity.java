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
package com.alibaba.langengine.infinity.vectorstore;

import com.alibaba.fastjson.JSON;
import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.core.vectorstore.VectorStore;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.alibaba.langengine.infinity.InfinityConfiguration.*;


@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class Infinity extends VectorStore {

    /**
     * 嵌入模型
     */
    private Embeddings embedding;

    /**
     * 表名
     */
    private final String tableName;

    /**
     * 数据库名称
     */
    private final String databaseName;

    /**
     * Infinity服务层
     */
    private final InfinityService infinityService;

    /**
     * 构造函数
     * 
     * @param tableName 表名
     */
    public Infinity(String tableName) {
        this(tableName, "default");
    }

    /**
     * 构造函数
     * 
     * @param tableName 表名
     * @param databaseName 数据库名称
     */
    public Infinity(String tableName, String databaseName) {
        this(tableName, databaseName, null);
    }

    /**
     * 构造函数
     * 
     * @param tableName 表名
     * @param databaseName 数据库名称
     * @param infinityParam 参数配置
     */
    public Infinity(String tableName, String databaseName, InfinityParam infinityParam) {
        this(tableName, databaseName, infinityParam, null);
    }

    Infinity(String tableName, String databaseName, InfinityParam infinityParam, InfinityService providedService) {
        this.tableName = tableName;
        this.databaseName = StringUtils.defaultIfEmpty(databaseName, "default");

        String serverUrl = System.getProperty(INFINITY_SERVER_URL, "127.0.0.1");
        String serverPort = System.getProperty(INFINITY_SERVER_PORT, "23817");

        String host = serverUrl;
        int port = Integer.parseInt(serverPort);

        if (serverUrl.contains(":")) {
            String[] parts = serverUrl.split(":");
            host = parts[0];
            if (parts.length > 1) {
                port = Integer.parseInt(parts[1]);
            }
        }

        this.infinityService = providedService != null
            ? providedService
            : new InfinityService(host, port, this.databaseName, tableName, infinityParam);

        log.info("Infinity vector store initialized with table='{}', database='{}'", tableName, this.databaseName);
    }

    /**
     * 初始化向量存储
     * 在表不存在的情况下创建表和索引
     * 
     * 1. 根据embedding模型结果维度创建embeddings向量字段
     * 2. 创建varchar的unique_id字段
     * 3. 创建长度可配置的page_content字符串字段
     * 4. 创建metadata字段存储文档元数据
     * 5. 对embeddings字段创建向量索引
     * 
     * 如果需要自定义表结构，请按照上面的字段类型规范进行提前创建:
     * 1. 线下创建表(可以修改字段的长度，字段名称，但字段类型不可变），建议以unique_id作为主键，这样在文档更新的时候可以覆盖
     * 2. 同时需要创建向量索引
     */
    public void init() {
        try {
            infinityService.init(embedding);
            log.info("Infinity vector store initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Infinity vector store", e);
            throw new InfinityException("INIT_ERROR", "Failed to initialize vector store", e);
        }
    }

    @Override
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            log.warn("Empty documents list, skipping addition");
            return;
        }
        
        try {
            // 如果文档没有嵌入向量，则通过嵌入模型生成
            documents = embedding.embedDocument(documents);
            infinityService.addDocuments(documents);
            log.info("Successfully added {} documents to Infinity", documents.size());
        } catch (Exception e) {
            log.error("Failed to add documents to Infinity", e);
            throw new InfinityException("ADD_DOCUMENTS_ERROR", "Failed to add documents", e);
        }
    }

    @Override
    public List<Document> similaritySearch(String query, int k, Double maxDistanceValue, Integer type) {
        if (StringUtils.isEmpty(query)) {
            log.warn("Empty query string, returning empty results");
            return Lists.newArrayList();
        }

        try {
            // 通过嵌入模型将查询文本转换为向量
            List<String> queryEmbeddingStrings = embedding.embedQuery(query, k);
            if (CollectionUtils.isEmpty(queryEmbeddingStrings) || !queryEmbeddingStrings.get(0).startsWith("[")) {
                log.warn("Failed to get valid embedding for query: {}", query);
                return Lists.newArrayList();
            }
            
            // 解析向量字符串为Double列表
            List<Float> floatEmbeddings = JSON.parseArray(queryEmbeddingStrings.get(0), Float.class);
            List<Double> queryVector = Lists.newArrayList();
            for (Float f : floatEmbeddings) {
                queryVector.add(f.doubleValue());
            }
            
            // 执行相似性搜索
            List<Document> results = infinityService.similaritySearch(queryVector, k, maxDistanceValue);
            
            log.info("Similarity search completed for query='{}', found {} results", query, results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to perform similarity search in Infinity", e);
            throw new InfinityException("SEARCH_ERROR", "Failed to perform similarity search", e);
        }
    }

    public void delete(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            log.warn("Empty ids list, skipping deletion");
            return;
        }

        try {
            infinityService.deleteDocuments(ids);
            log.info("Successfully deleted {} documents from Infinity", ids.size());
        } catch (Exception e) {
            log.error("Failed to delete documents from Infinity", e);
            throw new InfinityException("DELETE_ERROR", "Failed to delete documents", e);
        }
    }

    /**
     * 根据向量进行相似性搜索
     * 
     * @param queryVector 查询向量
     * @param k 返回结果数量
     * @param maxDistanceValue 最大距离阈值
     * @return 搜索结果文档列表
     */
    public List<Document> similaritySearchByVector(List<Double> queryVector, int k, Double maxDistanceValue) {
        if (CollectionUtils.isEmpty(queryVector)) {
            log.warn("Empty query vector, returning empty results");
            return Lists.newArrayList();
        }

        try {
            List<Document> results = infinityService.similaritySearch(queryVector, k, maxDistanceValue);
            log.info("Vector similarity search completed, found {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to perform vector similarity search in Infinity", e);
            throw new InfinityException("VECTOR_SEARCH_ERROR", "Failed to perform vector similarity search", e);
        }
    }

    /**
     * 获取Infinity服务层实例
     * 主要用于测试和高级操作
     * 
     * @return Infinity服务层实例
     */
    public InfinityService getInfinityService() {
        return infinityService;
    }

    /**
     * 关闭向量存储连接
     * 释放相关资源
     */
    public void close() {
        try {
            if (infinityService != null) {
                infinityService.close();
                log.info("Infinity vector store closed successfully");
            }
        } catch (Exception e) {
            log.error("Error closing Infinity vector store", e);
        }
    }

    /**
     * 获取表名
     * 
     * @return 表名
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * 获取数据库名称
     * 
     * @return 数据库名称
     */
    public String getDatabaseName() {
        return databaseName;
    }
}
