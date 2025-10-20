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
package com.alibaba.langengine.docloader.feishu.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 飞书知识库节点列表
 *
 * @author Libres-coder
 */
@Data
public class FeishuNodeList {

    private List<Node> items;

    @JsonProperty("page_token")
    private String pageToken;

    @JsonProperty("has_more")
    private Boolean hasMore;

    @Data
    public static class Node {

        @JsonProperty("node_token")
        private String nodeToken;

        @JsonProperty("obj_token")
        private String objToken;

        @JsonProperty("obj_type")
        private String objType;

        @JsonProperty("parent_node_token")
        private String parentNodeToken;

        @JsonProperty("node_type")
        private String nodeType;

        @JsonProperty("origin_node_token")
        private String originNodeToken;

        @JsonProperty("origin_space_id")
        private String originSpaceId;

        @JsonProperty("has_child")
        private Boolean hasChild;

        private String title;

        @JsonProperty("obj_create_time")
        private String objCreateTime;

        @JsonProperty("obj_edit_time")
        private String objEditTime;

        @JsonProperty("node_create_time")
        private String nodeCreateTime;

        @JsonProperty("creator")
        private String creator;

        @JsonProperty("owner")
        private String owner;
    }
}
