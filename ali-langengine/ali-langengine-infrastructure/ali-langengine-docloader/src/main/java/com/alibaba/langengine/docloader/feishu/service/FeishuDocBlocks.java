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
import java.util.Map;

/**
 * 飞书文档块内容（旧版文档API）
 *
 * @author Libres-coder
 */
@Data
public class FeishuDocBlocks {

    private List<Block> blocks;

    @Data
    public static class Block {

        @JsonProperty("block_id")
        private String blockId;

        @JsonProperty("block_type")
        private Integer blockType;

        @JsonProperty("parent_id")
        private String parentId;

        private Map<String, Object> text;

        private List<String> children;
    }
}
