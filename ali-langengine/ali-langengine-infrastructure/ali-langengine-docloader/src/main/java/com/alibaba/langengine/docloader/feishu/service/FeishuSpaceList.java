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
 * 飞书知识库列表
 *
 * @author Libres-coder
 */
@Data
public class FeishuSpaceList {

    private List<Space> items;

    @JsonProperty("page_token")
    private String pageToken;

    @JsonProperty("has_more")
    private Boolean hasMore;

    @Data
    public static class Space {

        @JsonProperty("space_id")
        private String spaceId;

        private String name;

        private String description;

        @JsonProperty("space_type")
        private String spaceType;

        private String visibility;
    }
}
