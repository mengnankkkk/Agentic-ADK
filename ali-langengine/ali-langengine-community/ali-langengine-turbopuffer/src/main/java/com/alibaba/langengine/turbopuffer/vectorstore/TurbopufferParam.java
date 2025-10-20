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
import lombok.Data;

import java.util.Map;


@Data
public class TurbopufferParam {

    /**
     * 向量字段名
     */
    String fieldNameEmbedding = "embeddings";

    /**
     * 内容字段名
     */
    String fieldNamePageContent = "content";

    /**
     * 唯一ID字段名
     */
    String fieldNameUniqueId = "id";

    /**
     * 距离度量类型 (cosine, euclidean, dot_product)
     */
    String distanceMetric = "cosine";

    /**
     * 自定义搜索扩展参数
     */
    Map<String, Object> searchParams = JSON.parseObject("{}");

    /**
     * 初始化参数, 用于创建Namespace
     */
    InitParam initParam = new InitParam();

    @Data
    public static class InitParam {

        /**
         * 是否使用uniqueId作为唯一键, 如果是的话, addDocuments的时候uniqueId不要为空
         */
        boolean fieldUniqueIdAsPrimaryKey = true;

        /**
         * embeddings字段向量维度, 如果设置为0, 则会通过embedding模型查询一条数据, 看维度是多少
         */
        int fieldEmbeddingsDimension = 1536;

        /**
         * 请求超时时间（毫秒）
         */
        int requestTimeoutMs = 30000;

        /**
         * 连接超时时间（毫秒）
         */
        int connectTimeoutMs = 10000;

        /**
         * 读取超时时间（毫秒）
         */
        int readTimeoutMs = 30000;

        /**
         * 最大重试次数
         */
        int maxRetries = 3;

        /**
         * 连接池配置
         */
        ConnectionPoolConfig connectionPool = new ConnectionPoolConfig();

        /**
         * 批量处理配置
         */
        BatchConfig batch = new BatchConfig();

        /**
         * 熔断器配置
         */
        CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    }

    @Data
    public static class ConnectionPoolConfig {
        /**
         * 最大空闲连接数
         */
        int maxIdleConnections = 5;

        /**
         * 连接存活时间（分钟）
         */
        long keepAliveDurationMinutes = 5;

        /**
         * 最大连接数
         */
        int maxConnections = 20;
    }

    @Data
    public static class BatchConfig {
        /**
         * 批量写入大小
         */
        int batchSize = 100;

        /**
         * 批量写入超时时间（毫秒）
         */
        int batchTimeoutMs = 5000;

        /**
         * 是否启用批量处理
         */
        boolean enableBatch = true;

        /**
         * 批量队列最大大小
         */
        int maxQueueSize = 1000;
    }

    @Data
    public static class CircuitBreakerConfig {
        /**
         * 是否启用熔断器
         */
        boolean enabled = true;

        /**
         * 失败阈值
         */
        int failureThreshold = 5;

        /**
         * 恢复超时时间（毫秒）
         */
        long recoveryTimeoutMs = 60000;

        /**
         * 监控窗口大小
         */
        int monitoringWindowSize = 10;
    }

}
