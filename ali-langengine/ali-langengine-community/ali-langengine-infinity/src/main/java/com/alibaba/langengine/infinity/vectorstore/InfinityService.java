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
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Slf4j
@Data
public class InfinityService {

    private final String tableName;
    private final InfinityClient infinityClient;
    private InfinityParam infinityParam;

    /**
     * 构造函数
     * 
     * @param host 主机地址
     * @param port 端口号
     * @param databaseName 数据库名称
     * @param tableName 表名
     * @param infinityParam 参数配置
     */
    public InfinityService(String host, int port, String databaseName, String tableName, InfinityParam infinityParam) {
        this(tableName, new InfinityClient(host, port, databaseName), infinityParam);
        log.info("InfinityService initialized with host={}, port={}, database={}, table={}",
                host, port, databaseName, tableName);
    }

    InfinityService(String tableName, InfinityClient infinityClient, InfinityParam infinityParam) {
        this.tableName = tableName;
        this.infinityClient = Objects.requireNonNull(infinityClient, "infinityClient");
        this.infinityParam = infinityParam != null ? infinityParam : new InfinityParam();
    }

    /**
     * 获取加载的参数配置
     * 
     * @return 参数配置
     */
    private InfinityParam loadParam() {
        return infinityParam != null ? infinityParam : new InfinityParam();
    }

    /**
     * 初始化向量存储
     * 创建数据库、表和索引
     * 
     * @param embedding 嵌入模型
     */
    public void init(Embeddings embedding) {
        try {
            InfinityParam param = loadParam();
            
            // 检查并创建数据库
            if (!infinityClient.checkDatabaseExists()) {
                infinityClient.createDatabase();
                log.info("Database created successfully");
            }

            // 检查并创建表
            if (!infinityClient.checkTableExists(tableName)) {
                // 确定向量维度
                int dimension = param.getInitParam().getFieldEmbeddingsDimension();
                if (dimension <= 0 && embedding != null) {
                    // 通过嵌入模型查询维度
                    List<String> testEmbeddingStrings = embedding.embedQuery("test", 1);
                    if (CollectionUtils.isNotEmpty(testEmbeddingStrings) && testEmbeddingStrings.get(0).startsWith("[")) {
                        List<Float> embeddingList = JSON.parseArray(testEmbeddingStrings.get(0), Float.class);
                        dimension = embeddingList.size();
                        param.getInitParam().setFieldEmbeddingsDimension(dimension);
                        log.info("Auto-detected vector dimension: {}", dimension);
                    }
                }

                createTable(dimension);
                createIndex();
            }
        } catch (Exception e) {
            log.error("Failed to initialize Infinity vector store", e);
            throw new InfinityException("INIT_ERROR", "Failed to initialize vector store", e);
        }
    }

    /**
     * 创建表
     * 
     * @param dimension 向量维度
     */
    private void createTable(int dimension) {
        InfinityParam param = loadParam();
        Map<String, Object> schema = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();
        
        // 添加字段定义
        Map<String, Object> uniqueIdField = new HashMap<>();
        uniqueIdField.put("type", "varchar");
        uniqueIdField.put("default", "");
        fields.put(param.getFieldNameUniqueId(), uniqueIdField);

        Map<String, Object> contentField = new HashMap<>();
        contentField.put("type", "varchar");
        contentField.put("default", "");
        fields.put(param.getFieldNamePageContent(), contentField);

        Map<String, Object> metadataField = new HashMap<>();
        metadataField.put("type", "varchar");
        metadataField.put("default", "{}");
        fields.put(param.getFieldNameMetadata(), metadataField);

        Map<String, Object> embeddingField = new HashMap<>();
        embeddingField.put("type", "vector");
        embeddingField.put("dimension", dimension);
        embeddingField.put("element_type", "float");
        fields.put(param.getFieldNameEmbedding(), embeddingField);

        schema.put("fields", fields);

        // 设置主键
        if (param.getInitParam().isFieldUniqueIdAsPrimaryKey()) {
            schema.put("primary_key", param.getFieldNameUniqueId());
        }

        infinityClient.createTable(tableName, schema);
        log.info("Table '{}' created with vector dimension {}", tableName, dimension);
    }

    /**
     * 创建向量索引
     */
    private void createIndex() {
        InfinityParam param = loadParam();
        String indexName = param.getFieldNameEmbedding() + "_idx";
        
        Map<String, Object> indexDefinition = new HashMap<>();
        indexDefinition.put("type", param.getInitParam().getIndexType());
        indexDefinition.put("metric", param.getInitParam().getDistanceType());
        
        // 添加索引参数
        if (MapUtils.isNotEmpty(param.getInitParam().getIndexParams())) {
            indexDefinition.put("params", param.getInitParam().getIndexParams());
        }

        Map<String, Object> indexSpec = new HashMap<>();
        indexSpec.put("fields", Lists.newArrayList(param.getFieldNameEmbedding()));
        indexSpec.put("index", indexDefinition);

        infinityClient.createIndex(tableName, indexName, indexSpec);
        log.info("Index '{}' created on table '{}'", indexName, tableName);
    }

    /**
     * 添加文档到向量存储
     * 
     * @param documents 文档列表
     */
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            log.warn("Empty documents list, skipping insertion");
            return;
        }

        InfinityParam param = loadParam();
        List<Map<String, Object>> dataList = new ArrayList<>();

        for (Document document : documents) {
            Map<String, Object> row = new HashMap<>();
            
            // 设置唯一ID
            String uniqueId = StringUtils.isNotEmpty(document.getUniqueId()) ? 
                document.getUniqueId() : String.valueOf(System.currentTimeMillis());
            row.put(param.getFieldNameUniqueId(), uniqueId);
            
            // 设置页面内容
            row.put(param.getFieldNamePageContent(), 
                StringUtils.defaultString(document.getPageContent(), ""));
            
            // 设置元数据
            String metadata = MapUtils.isNotEmpty(document.getMetadata()) ? 
                JSON.toJSONString(document.getMetadata()) : "{}";
            row.put(param.getFieldNameMetadata(), metadata);
            
            // 设置向量
            if (CollectionUtils.isNotEmpty(document.getEmbedding())) {
                List<Float> embedding = Lists.newArrayList();
                for (Double d : document.getEmbedding()) {
                    embedding.add(d.floatValue());
                }
                row.put(param.getFieldNameEmbedding(), embedding);
            }
            
            dataList.add(row);
        }

        // 构建插入请求
        InfinityInsertRequest request = new InfinityInsertRequest();
        request.setData(dataList);
        request.setReplace(true); // 允许覆盖已存在的数据

        infinityClient.insert(tableName, request);
        log.info("Inserted {} documents into table '{}'", documents.size(), tableName);
    }

    /**
     * 相似性搜索
     * 
     * @param queryVector 查询向量
     * @param k 返回结果数量
     * @param maxDistanceValue 最大距离阈值
     * @return 搜索结果文档列表
     */
    public List<Document> similaritySearch(List<Double> queryVector, int k, Double maxDistanceValue) {
        if (CollectionUtils.isEmpty(queryVector)) {
            log.warn("Empty query vector, returning empty results");
            return Lists.newArrayList();
        }

        InfinityParam param = loadParam();
        
        // 转换查询向量
        List<Float> floatVector = Lists.newArrayList();
        for (Double d : queryVector) {
            floatVector.add(d.floatValue());
        }

        // 构建搜索请求
        InfinitySearchRequest request = new InfinitySearchRequest();
        request.setVector(floatVector);
        request.setTopK(k);
        request.setDistanceThreshold(maxDistanceValue);
        request.setParams(param.getSearchParams());
        
        // 设置返回字段
        List<String> outputFields = Lists.newArrayList(
            param.getFieldNameUniqueId(),
            param.getFieldNamePageContent(),
            param.getFieldNameMetadata()
        );
        request.setOutputFields(outputFields);

        // 执行搜索
        InfinitySearchResponse response = infinityClient.search(tableName, request);
        
        // 检查响应
        if (response.getErrorCode() != null && response.getErrorCode() != 0) {
            throw new InfinityException("SEARCH_ERROR", 
                String.format("Search failed with error code: %d, message: %s", 
                    response.getErrorCode(), response.getErrorMessage()));
        }

        // 转换结果
        List<Document> documents = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(response.getOutput())) {
            for (InfinitySearchResponse.SearchResult result : response.getOutput()) {
                Document document = new Document();
                Map<String, Object> row = result.getRow();
                
                if (row != null) {
                    document.setUniqueId(String.valueOf(row.get(param.getFieldNameUniqueId())));
                    document.setPageContent(String.valueOf(row.get(param.getFieldNamePageContent())));
                    
                    // 解析元数据
                    String metadataStr = String.valueOf(row.get(param.getFieldNameMetadata()));
                    if (StringUtils.isNotEmpty(metadataStr) && !"null".equals(metadataStr)) {
                        try {
                            Map<String, Object> metadata = JSON.parseObject(metadataStr, Map.class);
                            document.setMetadata(metadata);
                        } catch (Exception e) {
                            log.warn("Failed to parse metadata: {}", metadataStr, e);
                            document.setMetadata(new HashMap<>());
                        }
                    }
                    
                    // 设置距离分数
                    if (result.getDistance() != null) {
                        Map<String, Object> metadata = document.getMetadata();
                        if (metadata == null) {
                            metadata = new HashMap<>();
                            document.setMetadata(metadata);
                        }
                        metadata.put("distance", result.getDistance());
                        metadata.put("similarity", result.getSimilarity());
                    }
                }
                
                documents.add(document);
            }
        }

        log.info("Search completed, found {} documents", documents.size());
        return documents;
    }

    /**
     * 删除文档
     * 
     * @param uniqueIds 要删除的文档唯一ID列表
     */
    public void deleteDocuments(List<String> uniqueIds) {
        if (CollectionUtils.isEmpty(uniqueIds)) {
            log.warn("Empty uniqueIds list, skipping deletion");
            return;
        }

        InfinityParam param = loadParam();
        Map<String, Object> condition = new HashMap<>();
        condition.put(param.getFieldNameUniqueId(), uniqueIds);

        infinityClient.delete(tableName, condition);
        log.info("Deleted {} documents from table '{}'", uniqueIds.size(), tableName);
    }

    /**
     * 关闭服务
     */
    public void close() {
        if (infinityClient != null) {
            infinityClient.close();
            log.info("InfinityService closed successfully");
        }
    }
}
