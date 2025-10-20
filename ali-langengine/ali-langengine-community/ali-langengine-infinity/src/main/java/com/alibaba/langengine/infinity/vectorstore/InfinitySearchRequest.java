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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
public class InfinitySearchRequest {

    /**
     * 查询向量
     */
    @JsonProperty("vector")
    private List<Float> vector;

    /**
     * 返回结果数量
     */
    @JsonProperty("top_k")
    private Integer topK;

    /**
     * 距离阈值
     */
    @JsonProperty("distance_threshold")
    private Double distanceThreshold;

    /**
     * 过滤条件
     */
    @JsonProperty("filter")
    private Map<String, Object> filter;

    /**
     * 搜索参数
     */
    @JsonProperty("params")
    private Map<String, Object> params;

    /**
     * 是否返回向量
     */
    @JsonProperty("output_fields")
    private List<String> outputFields;
}
