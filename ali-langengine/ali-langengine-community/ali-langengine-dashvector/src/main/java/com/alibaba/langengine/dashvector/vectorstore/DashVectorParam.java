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

import lombok.Data;

@Data
public class DashVectorParam {

    /**
     * 向量维度
     */
    private int dimension = 1536;

    /**
     * 距离度量类型
     */
    private String metric = "cosine";

    /**
     * 副本数量
     */
    private int replicas = 1;

    /**
     * 分片数量
     */
    private int shards = 1;

    /**
     * 批量操作大小
     */
    private int batchSize = 100;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 30000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 60000;

}