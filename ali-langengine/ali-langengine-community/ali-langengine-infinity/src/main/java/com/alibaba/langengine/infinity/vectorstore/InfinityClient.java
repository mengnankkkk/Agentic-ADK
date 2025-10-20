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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class InfinityClient {

    private final String baseUrl;
    private final String databaseName;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * 
     * @param host 主机地址
     * @param port 端口号
     * @param databaseName 数据库名称
     */
    public InfinityClient(String host, int port, String databaseName) {
        this(host, port, databaseName, HttpClients.createDefault(), new ObjectMapper());
    }

    InfinityClient(String host,
                   int port,
                   String databaseName,
                   CloseableHttpClient httpClient,
                   ObjectMapper objectMapper) {
        this.baseUrl = String.format("http://%s:%d", host, port);
        this.databaseName = databaseName;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        log.info("InfinityClient initialized with baseUrl={}, databaseName={}", baseUrl, databaseName);
    }

    /**
     * 检查数据库是否存在
     * 
     * @return 数据库是否存在
     */
    public boolean checkDatabaseExists() {
        try {
            String url = String.format("%s/databases/%s", baseUrl, databaseName);
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            return statusCode == 200;
        } catch (IOException e) {
            log.error("Error checking database existence", e);
            throw new InfinityException("DATABASE_CHECK_ERROR", "Failed to check database existence", e);
        }
    }

    /**
     * 创建数据库
     */
    public void createDatabase() {
        try {
            String url = String.format("%s/databases/%s", baseUrl, databaseName);
            HttpPost request = new HttpPost(url);
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 201) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                throw new InfinityException("DATABASE_CREATE_ERROR", 
                    String.format("Failed to create database, status: %d, response: %s", statusCode, responseBody));
            }
            log.info("Database '{}' created successfully", databaseName);
        } catch (IOException e) {
            log.error("Error creating database", e);
            throw new InfinityException("DATABASE_CREATE_ERROR", "Failed to create database", e);
        }
    }

    /**
     * 检查表是否存在
     * 
     * @param tableName 表名
     * @return 表是否存在
     */
    public boolean checkTableExists(String tableName) {
        try {
            String url = String.format("%s/databases/%s/tables/%s", baseUrl, databaseName, tableName);
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            return statusCode == 200;
        } catch (IOException e) {
            log.error("Error checking table existence", e);
            throw new InfinityException("TABLE_CHECK_ERROR", "Failed to check table existence", e);
        }
    }

    /**
     * 创建表
     * 
     * @param tableName 表名
     * @param schema 表结构定义
     */
    public void createTable(String tableName, Map<String, Object> schema) {
        try {
            String url = String.format("%s/databases/%s/tables/%s", baseUrl, databaseName, tableName);
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");
            
            String jsonSchema = objectMapper.writeValueAsString(schema);
            request.setEntity(new StringEntity(jsonSchema, StandardCharsets.UTF_8));
            
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 201) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                throw new InfinityException("TABLE_CREATE_ERROR", 
                    String.format("Failed to create table, status: %d, response: %s", statusCode, responseBody));
            }
            log.info("Table '{}' created successfully", tableName);
        } catch (IOException e) {
            log.error("Error creating table", e);
            throw new InfinityException("TABLE_CREATE_ERROR", "Failed to create table", e);
        }
    }

    /**
     * 创建向量索引
     * 
     * @param tableName 表名
     * @param indexName 索引名
     * @param indexDefinition 索引定义
     */
    public void createIndex(String tableName, String indexName, Map<String, Object> indexDefinition) {
        try {
            String url = String.format("%s/databases/%s/tables/%s/indexes/%s", baseUrl, databaseName, tableName, indexName);
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");
            
            String jsonIndex = objectMapper.writeValueAsString(indexDefinition);
            request.setEntity(new StringEntity(jsonIndex, StandardCharsets.UTF_8));
            
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 201) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                throw new InfinityException("INDEX_CREATE_ERROR", 
                    String.format("Failed to create index, status: %d, response: %s", statusCode, responseBody));
            }
            log.info("Index '{}' created successfully on table '{}'", indexName, tableName);
        } catch (IOException e) {
            log.error("Error creating index", e);
            throw new InfinityException("INDEX_CREATE_ERROR", "Failed to create index", e);
        }
    }

    /**
     * 插入数据
     * 
     * @param tableName 表名
     * @param request 插入请求
     */
    public void insert(String tableName, InfinityInsertRequest request) {
        try {
            String url = String.format("%s/databases/%s/tables/%s/docs", baseUrl, databaseName, tableName);
            HttpPost httpRequest = new HttpPost(url);
            httpRequest.setHeader("Content-Type", "application/json");
            
            String jsonRequest = objectMapper.writeValueAsString(request);
            httpRequest.setEntity(new StringEntity(jsonRequest, StandardCharsets.UTF_8));
            
            HttpResponse response = httpClient.execute(httpRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 201) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                throw new InfinityException("INSERT_ERROR", 
                    String.format("Failed to insert data, status: %d, response: %s", statusCode, responseBody));
            }
            log.debug("Data inserted successfully into table '{}'", tableName);
        } catch (IOException e) {
            log.error("Error inserting data", e);
            throw new InfinityException("INSERT_ERROR", "Failed to insert data", e);
        }
    }

    /**
     * 向量搜索
     * 
     * @param tableName 表名
     * @param request 搜索请求
     * @return 搜索响应
     */
    public InfinitySearchResponse search(String tableName, InfinitySearchRequest request) {
        try {
            String url = String.format("%s/databases/%s/tables/%s/docs/search", baseUrl, databaseName, tableName);
            HttpPost httpRequest = new HttpPost(url);
            httpRequest.setHeader("Content-Type", "application/json");
            
            String jsonRequest = objectMapper.writeValueAsString(request);
            httpRequest.setEntity(new StringEntity(jsonRequest, StandardCharsets.UTF_8));
            
            HttpResponse response = httpClient.execute(httpRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            if (statusCode != 200) {
                throw new InfinityException("SEARCH_ERROR", 
                    String.format("Failed to search, status: %d, response: %s", statusCode, responseBody));
            }
            
            InfinitySearchResponse searchResponse = objectMapper.readValue(responseBody, InfinitySearchResponse.class);
            log.debug("Search completed successfully on table '{}'", tableName);
            return searchResponse;
        } catch (IOException e) {
            log.error("Error performing search", e);
            throw new InfinityException("SEARCH_ERROR", "Failed to perform search", e);
        }
    }

    /**
     * 删除数据
     * 
     * @param tableName 表名
     * @param condition 删除条件
     */
    public void delete(String tableName, Map<String, Object> condition) {
        try {
            String url = String.format("%s/databases/%s/tables/%s/docs", baseUrl, databaseName, tableName);
            HttpDelete request = new HttpDelete(url);
            request.setHeader("Content-Type", "application/json");
            
            // 如果有删除条件，可以通过请求体传递
            if (condition != null && !condition.isEmpty()) {
                String jsonCondition = objectMapper.writeValueAsString(condition);
                // Note: HttpDelete doesn't typically support request body, this might need adjustment
                log.warn("Delete with condition not fully implemented, condition: {}", jsonCondition);
            }
            
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 204) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                throw new InfinityException("DELETE_ERROR", 
                    String.format("Failed to delete data, status: %d, response: %s", statusCode, responseBody));
            }
            log.debug("Data deleted successfully from table '{}'", tableName);
        } catch (IOException e) {
            log.error("Error deleting data", e);
            throw new InfinityException("DELETE_ERROR", "Failed to delete data", e);
        }
    }

    /**
     * 关闭客户端连接
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
                log.info("InfinityClient closed successfully");
            }
        } catch (IOException e) {
            log.error("Error closing InfinityClient", e);
        }
    }
}
