package com.alibaba.langengine.docloader.feishu.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeishuDocContent {

    @JsonProperty("document_id")
    private String documentId;

    @JsonProperty("revision_id")
    private String revisionId;

    /**
     * 文档内容块列表
     */
    private List<ContentBlock> blocks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentBlock {

        @JsonProperty("block_id")
        private String blockId;

        @JsonProperty("block_type")
        private String blockType;

        @JsonProperty("parent_id")
        private String parentId;

        /**
         * 文本内容
         */
        private TextContent text;

        /**
         * 其他类型内容
         */
        private Map<String, Object> content;

        /**
         * 子块
         */
        private List<ContentBlock> children;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextContent {

        /**
         * 纯文本内容
         */
        private String content;

        /**
         * 文本样式
         */
        private List<TextStyle> style;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextStyle {

        /**
         * 样式类型
         */
        private String type;

        /**
         * 样式属性
         */
        private Map<String, Object> attrs;
    }
}