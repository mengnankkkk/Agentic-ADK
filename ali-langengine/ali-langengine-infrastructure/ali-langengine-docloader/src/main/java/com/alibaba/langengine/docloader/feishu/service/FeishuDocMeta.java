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
 * 飞书文档元信息
 *
 * @author Libres-coder
 */
@Data
public class FeishuDocMeta {

    private List<MetaItem> metas;

    @Data
    public static class MetaItem {

        @JsonProperty("doc_token")
        private String docToken;

        @JsonProperty("doc_type")
        private String docType;

        private String title;

        @JsonProperty("owner_id")
        private String ownerId;

        @JsonProperty("create_time")
        private String createTime;

        @JsonProperty("latest_modify_time")
        private String latestModifyTime;

        @JsonProperty("latest_modify_user")
        private String latestModifyUser;

        private String url;
    }
}
