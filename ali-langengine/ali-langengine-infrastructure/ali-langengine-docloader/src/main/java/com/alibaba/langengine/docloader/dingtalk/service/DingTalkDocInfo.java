package com.alibaba.langengine.docloader.dingtalk.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DingTalkDocInfo {

    /**
     * 文档ID
     */
    private String id;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档描述
     */
    private String description;

    /**
     * 文档内容（纯文本）
     */
    private String body;

    /**
     * 文档内容（HTML格式）
     */
    @JsonProperty("body_html")
    private String bodyHtml;

    /**
     * 创建者信息
     */
    private String creator;

    /**
     * 创建时间
     */
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * 更新时间
     */
    @JsonProperty("updated_at")
    private String updatedAt;

    /**
     * 内容更新时间
     */
    @JsonProperty("content_updated_at")
    private String contentUpdatedAt;

    /**
     * 发布时间
     */
    @JsonProperty("published_at")
    private String publishedAt;

    /**
     * 文档状态
     */
    private Integer status;

    /**
     * 是否公开
     */
    @JsonProperty("is_public")
    private Boolean isPublic;

    /**
     * 阅读次数
     */
    @JsonProperty("read_count")
    private Integer readCount;

    /**
     * 点赞次数
     */
    @JsonProperty("like_count")
    private Integer likeCount;

    /**
     * 评论次数
     */
    @JsonProperty("comment_count")
    private Integer commentCount;

    /**
     * 字数统计
     */
    @JsonProperty("word_count")
    private Integer wordCount;

    /**
     * 文档标签
     */
    private List<String> tags;

    /**
     * 文档分类
     */
    private String category;

    /**
     * 最后编辑者
     */
    @JsonProperty("last_editor")
    private Map<String, Object> lastEditor;

    /**
     * 协作者列表
     */
    private List<Map<String, Object>> collaborators;

    /**
     * 文档权限
     */
    private Map<String, Object> permissions;

    /**
     * 扩展属性
     */
    private Map<String, Object> extra;
}