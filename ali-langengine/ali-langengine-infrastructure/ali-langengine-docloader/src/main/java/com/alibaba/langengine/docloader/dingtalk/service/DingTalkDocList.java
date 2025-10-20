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
package com.alibaba.langengine.docloader.dingtalk.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 钉钉文档列表
 *
 * @author Libres-coder
 */
@Data
public class DingTalkDocList {

    @JsonProperty("doc_list")
    private List<DocInfo> docList;

    @JsonProperty("has_more")
    private Boolean hasMore;

    @JsonProperty("next_token")
    private String nextToken;

    @Data
    public static class DocInfo {

        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_title")
        private String docTitle;

        @JsonProperty("doc_type")
        private String docType;

        @JsonProperty("create_time")
        private Long createTime;

        @JsonProperty("modified_time")
        private Long modifiedTime;

        @JsonProperty("creator_id")
        private String creatorId;

        @JsonProperty("modifier_id")
        private String modifierId;
    }
}
