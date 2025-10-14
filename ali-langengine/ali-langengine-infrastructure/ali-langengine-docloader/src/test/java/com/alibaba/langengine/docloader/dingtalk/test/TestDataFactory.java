package com.alibaba.langengine.docloader.dingtalk.test;

import com.alibaba.langengine.docloader.dingtalk.service.DingTalkDocInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TestDataFactory {

    /**
     * 创建标准的文档信息对象
     */
    public static DingTalkDocInfo createDocInfo(String id, String title, String content) {
        DingTalkDocInfo docInfo = new DingTalkDocInfo();
        docInfo.setId(id);
        docInfo.setTitle(title);
        docInfo.setBody(content);
        docInfo.setBodyHtml("<p>" + content + "</p>");
        docInfo.setCreator("test-user");
        docInfo.setCreatedAt("2025-01-01T00:00:00Z");
        docInfo.setUpdatedAt("2025-01-01T00:00:00Z");
        docInfo.setContentUpdatedAt("2025-01-01T00:00:00Z");
        docInfo.setPublishedAt("2025-01-01T00:00:00Z");
        docInfo.setStatus(1);
        docInfo.setIsPublic(true);
        docInfo.setReadCount(0);
        docInfo.setLikeCount(0);
        docInfo.setCommentCount(0);
        docInfo.setWordCount(content != null ? content.length() : 0);
        docInfo.setTags(Arrays.asList("test", "mock"));
        docInfo.setCategory("test-category");
        return docInfo;
    }

    /**
     * 创建文档信息列表
     */
    public static List<DingTalkDocInfo> createDocInfoList(int count, String prefix) {
        List<DingTalkDocInfo> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = prefix + "-doc-" + i;
            String title = prefix + " Document " + i;
            String content = prefix + " Content " + i;
            list.add(createDocInfo(id, title, content));
        }
        return list;
    }

    /**
     * 创建空内容的文档信息
     */
    public static DingTalkDocInfo createEmptyDocInfo(String id, String title) {
        DingTalkDocInfo docInfo = createDocInfo(id, title, "");
        docInfo.setBody("");
        docInfo.setBodyHtml("");
        docInfo.setWordCount(0);
        return docInfo;
    }

    /**
     * 创建包含特殊字符的文档信息
     */
    public static DingTalkDocInfo createSpecialCharDocInfo(String id) {
        String specialContent = "Special chars: <>&\"' 中文 emoji 😀 \n\t\r";
        return createDocInfo(id, "Special Document", specialContent);
    }

    /**
     * 创建大文档信息（用于性能测试）
     */
    public static DingTalkDocInfo createLargeDocInfo(String id, int sizeInKB) {
        StringBuilder content = new StringBuilder();
        int targetSize = sizeInKB * 1024;

        String repeatedText = "This is a test content for large document. ";
        while (content.length() < targetSize) {
            content.append(repeatedText);
        }

        return createDocInfo(id, "Large Document", content.toString());
    }

    /**
     * 创建包含HTML内容的文档信息
     */
    public static DingTalkDocInfo createHtmlDocInfo(String id, String title) {
        DingTalkDocInfo docInfo = createDocInfo(id, title, "Plain text content");
        docInfo.setBodyHtml(
                "<h1>" + title + "</h1>" +
                "<p>This is a paragraph with <strong>bold</strong> and <em>italic</em> text.</p>" +
                "<ul><li>Item 1</li><li>Item 2</li></ul>" +
                "<pre><code>code block</code></pre>"
        );
        return docInfo;
    }

    /**
     * 创建包含完整元数据的文档信息
     */
    public static DingTalkDocInfo createFullMetadataDocInfo(String id) {
        DingTalkDocInfo docInfo = createDocInfo(id, "Full Metadata Doc", "Content with metadata");
        docInfo.setDescription("This is a test document with full metadata");
        docInfo.setCreator("author@example.com");
        docInfo.setCreatedAt("2025-01-01T10:00:00Z");
        docInfo.setUpdatedAt("2025-01-15T15:30:00Z");
        docInfo.setContentUpdatedAt("2025-01-15T15:30:00Z");
        docInfo.setPublishedAt("2025-01-02T08:00:00Z");
        docInfo.setStatus(1);
        docInfo.setIsPublic(true);
        docInfo.setReadCount(1000);
        docInfo.setLikeCount(50);
        docInfo.setCommentCount(20);
        docInfo.setWordCount(500);
        docInfo.setTags(Arrays.asList("important", "draft", "review", "testing"));
        docInfo.setCategory("technical");
        return docInfo;
    }

    /**
     * 创建包含中文内容的文档信息
     */
    public static DingTalkDocInfo createChineseDocInfo(String id) {
        String chineseContent = "这是一个中文测试文档。包含各种中文字符和标点符号。" +
                "测试文档加载器是否能够正确处理中文内容。" +
                "中文、日文、韩文等多语言支持非常重要。";

        DingTalkDocInfo docInfo = createDocInfo(id, "中文测试文档", chineseContent);
        docInfo.setBodyHtml("<h1>中文标题</h1><p>" + chineseContent + "</p>");
        docInfo.setTags(Arrays.asList("中文", "测试", "多语言"));
        return docInfo;
    }

    /**
     * 创建包含Markdown内容的文档信息
     */
    public static DingTalkDocInfo createMarkdownDocInfo(String id) {
        String markdownContent = "# Heading 1\n\n" +
                "## Heading 2\n\n" +
                "This is a paragraph with **bold** and *italic* text.\n\n" +
                "- List item 1\n" +
                "- List item 2\n\n" +
                "```java\n" +
                "public class Test {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello\");\n" +
                "    }\n" +
                "}\n" +
                "```";

        return createDocInfo(id, "Markdown Document", markdownContent);
    }

    /**
     * 创建测试用的文档ID列表
     */
    public static List<String> createDocIdList(int count, String prefix) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(prefix + "-doc-" + i);
        }
        return ids;
    }
}
