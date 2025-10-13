package com.alibaba.langengine.docloader.feishu.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeishuDocInfo {

    @JsonProperty("document_id")
    private String documentId;

    @JsonProperty("revision_id")
    private String revisionId;

    private String title;

    @JsonProperty("owner_id")
    private String ownerId;

    @JsonProperty("create_time")
    private String createdAt;

    @JsonProperty("update_time")
    private String updatedAt;

    @JsonProperty("document_type")
    private String documentType;

    @JsonProperty("parent_node_token")
    private String parentNodeToken;

    @JsonProperty("node_token")
    private String nodeToken;

    @JsonProperty("obj_token")
    private String objToken;

    @JsonProperty("owner_type")
    private Integer ownerType;

    private Map<String, Object> owner;

    /**
     * 文档内容（纯文本）
     */
    private String content;

    /**
     * 文档内容（HTML格式）
     */
    @JsonProperty("content_html")
    private String contentHtml;

    /**
     * 文档状态
     */
    private Integer status;

    /**
     * 文档URL
     */
    private String url;

    /**
     * 文档大小
     */
    @JsonProperty("document_size")
    private Long documentSize;

    /**
     * 是否为模板
     */
    @JsonProperty("is_template")
    private Boolean isTemplate;

    /**
     * 文档权限
     */
    private Map<String, Object> permission;

    /**
     * 扩展属性
     */
    private Map<String, Object> extra;
}