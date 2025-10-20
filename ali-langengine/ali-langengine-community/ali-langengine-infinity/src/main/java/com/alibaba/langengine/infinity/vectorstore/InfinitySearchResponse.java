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
public class InfinitySearchResponse {

    /**
     * 响应状态码
     */
    @JsonProperty("error_code")
    private Integer errorCode;

    /**
     * 响应消息
     */
    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * 搜索结果
     */
    @JsonProperty("output")
    private List<SearchResult> output;

    @Data
    public static class SearchResult {

        /**
         * 距离分数
         */
        @JsonProperty("distance")
        private Double distance;

        /**
         * 相似度分数
         */
        @JsonProperty("similarity")
        private Double similarity;

        /**
         * 行数据
         */
        @JsonProperty("row")
        private Map<String, Object> row;
    }
}
