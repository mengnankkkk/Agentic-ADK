package com.alibaba.langengine.docloader.wework.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeWorkDocInfo {

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
     * 创建者ID
     */
    @JsonProperty("creator_id")
    private String creatorId;

    /**
     * 创建时间（时间戳）
     */
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * 更新时间（时间戳）
     */
    @JsonProperty("updated_at")
    private String updatedAt;

    /**
     * 内容更新时间（时间戳）
     */
    @JsonProperty("content_updated_at")
    private String contentUpdatedAt;

    /**
     * 发布时间（时间戳）
     */
    @JsonProperty("published_at")
    private String publishedAt;

    /**
     * 文档状态
     * 0: 草稿
     * 1: 已发布
     * 2: 已删除
     */
    private Integer status;

    /**
     * 是否公开
     */
    @JsonProperty("is_public")
    private Boolean isPublic;

    /**
     * 是否允许外部访问
     */
    @JsonProperty("allow_external")
    private Boolean allowExternal;

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
     * 收藏次数
     */
    @JsonProperty("favorite_count")
    private Integer favoriteCount;

    /**
     * 文档类型
     * doc: 文档
     * sheet: 表格
     * slide: 演示文稿
     */
    @JsonProperty("doc_type")
    private String docType;

    /**
     * 文档格式
     * markdown, html, text等
     */
    private String format;

    /**
     * 文档大小（字节）
     */
    private Long size;

    /**
     * 文档版本号
     */
    private String version;

    /**
     * 文档标签
     */
    private List<String> tags;

    /**
     * 文档分类
     */
    private String category;

    /**
     * 文档权限级别
     * 0: 公开
     * 1: 企业内可见
     * 2: 部门内可见
     * 3: 私有
     */
    @JsonProperty("permission_level")
    private Integer permissionLevel;

    /**
     * 最后编辑者信息
     */
    @JsonProperty("last_editor")
    private Map<String, Object> lastEditor;

    /**
     * 最后编辑者ID
     */
    @JsonProperty("last_editor_id")
    private String lastEditorId;

    /**
     * 协作者列表
     */
    private List<Map<String, Object>> collaborators;

    /**
     * 文档路径
     */
    private String path;

    /**
     * 父目录ID
     */
    @JsonProperty("parent_id")
    private String parentId;

    /**
     * 空间ID（知识库ID）
     */
    @JsonProperty("space_id")
    private String spaceId;

    /**
     * 企业ID
     */
    @JsonProperty("corp_id")
    private String corpId;

    /**
     * 文档URL
     */
    private String url;

    /**
     * 分享链接
     */
    @JsonProperty("share_url")
    private String shareUrl;

    /**
     * 文档摘要
     */
    private String summary;

    /**
     * 文档关键词
     */
    private List<String> keywords;

    /**
     * 附件列表
     */
    private List<Map<String, Object>> attachments;

    /**
     * 外部链接
     */
    @JsonProperty("external_links")
    private List<String> externalLinks;

    /**
     * 自定义字段
     */
    @JsonProperty("custom_fields")
    private Map<String, Object> customFields;

    /**
     * 文档语言
     */
    private String language;

    /**
     * 字数统计
     */
    @JsonProperty("word_count")
    private Integer wordCount;

    /**
     * 是否加密
     */
    @JsonProperty("is_encrypted")
    private Boolean isEncrypted;

    /**
     * 水印设置
     */
    private Map<String, Object> watermark;

    /**
     * 下载限制
     */
    @JsonProperty("download_restriction")
    private Map<String, Object> downloadRestriction;

    /**
     * 打印限制
     */
    @JsonProperty("print_restriction")
    private Map<String, Object> printRestriction;

    /**
     * 审批状态
     */
    @JsonProperty("approval_status")
    private Integer approvalStatus;

    /**
     * 审批者信息
     */
    private Map<String, Object> approver;

    /**
     * 文档模板ID
     */
    @JsonProperty("template_id")
    private String templateId;

    /**
     * 是否是模板
     */
    @JsonProperty("is_template")
    private Boolean isTemplate;

    /**
     * 文档来源
     */
    private String source;

    /**
     * 同步状态
     */
    @JsonProperty("sync_status")
    private Integer syncStatus;

    /**
     * 最后同步时间
     */
    @JsonProperty("last_sync_at")
    private String lastSyncAt;
}
