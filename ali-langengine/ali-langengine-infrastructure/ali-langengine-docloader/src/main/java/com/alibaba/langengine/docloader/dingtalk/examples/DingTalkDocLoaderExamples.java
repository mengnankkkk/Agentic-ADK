package com.alibaba.langengine.docloader.dingtalk.examples;

import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.dingtalk.DingTalkDocLoader;
import com.alibaba.langengine.docloader.dingtalk.config.DingTalkConfig;
import com.alibaba.langengine.docloader.dingtalk.exception.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
public class DingTalkDocLoaderExamples {

    /**
     * 示例1: 基础使用 - 加载命名空间下所有文档
     */
    public static void example1_BasicUsage() {
        log.info("=== Example 1: Basic Usage ===");

        DingTalkDocLoader loader = null;
        try {
            // 构建加载器
            loader = new DingTalkDocLoader.Builder()
                    .apiToken("your-api-token-here")
                    .namespace("your-workspace-id")
                    .batchSize(50)
                    .timeout(60L)
                    .build();

            // 加载文档
            List<Document> documents = loader.load();

            log.info("Loaded {} documents", documents.size());

            // 处理文档
            documents.forEach(doc -> {
                log.info("Document: {}", doc.getMetadata().get("title"));
                log.info("Author: {}", doc.getMetadata().get("author"));
                log.info("URL: {}", doc.getMetadata().get("url"));
            });

        } catch (Exception e) {
            log.error("Failed to load documents", e);
        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }
    }

    /**
     * 示例2: 加载单个文档
     */
    public static void example2_LoadSingleDocument() {
        log.info("=== Example 2: Load Single Document ===");

        DingTalkDocLoader loader = null;
        try {
            loader = new DingTalkDocLoader.Builder()
                    .apiToken("your-api-token-here")
                    .namespace("workspace-id")
                    .documentId("doc-id-12345")  // 指定文档ID
                    .build();

            List<Document> documents = loader.load();

            if (!documents.isEmpty()) {
                Document doc = documents.get(0);
                log.info("Title: {}", doc.getMetadata().get("title"));
                log.info("Content length: {}", doc.getPageContent().length());
            }

        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }
    }

    /**
     * 示例3: 使用预设配置
     */
    public static void example3_UsingPresetConfig() {
        log.info("=== Example 3: Using Preset Config ===");

        // 场景1: 高性能配置（生产环境）
        DingTalkConfig highPerfConfig = DingTalkConfig.highPerformanceConfig();
        log.info("High performance config: permits={}/s, connections={}",
                highPerfConfig.getRateLimitPermitsPerSecond(),
                highPerfConfig.getMaxConnections());

        // 场景2: 保守配置（受限环境）
        DingTalkConfig conservativeConfig = DingTalkConfig.conservativeConfig();
        log.info("Conservative config: permits={}/s, connections={}",
                conservativeConfig.getRateLimitPermitsPerSecond(),
                conservativeConfig.getMaxConnections());

        // 场景3: 自定义配置
        DingTalkConfig customConfig = DingTalkConfig.builder()
                .maxRetries(5)
                .rateLimitPermitsPerSecond(15.0)
                .circuitBreakerEnabled(true)
                .metricsEnabled(true)
                .build();

        // 验证配置
        customConfig.validate();

        log.info("Custom config created successfully");
    }

    /**
     * 示例4: 异常处理
     */
    public static void example4_ExceptionHandling() {
        log.info("=== Example 4: Exception Handling ===");

        DingTalkDocLoader loader = new DingTalkDocLoader.Builder()
                .apiToken("invalid-token")
                .namespace("workspace-id")
                .build();

        try {
            loader.load();

        } catch (DingTalkAuthenticationException e) {
            // 认证失败 - 检查 API Token
            log.error("Authentication failed: {}", e.getMessage());
            log.error("Error code: {}", e.getErrorCode());
            log.error("HTTP status: {}", e.getHttpStatusCode());

        } catch (DingTalkRateLimitException e) {
            // 限流 - 等待后重试
            log.warn("Rate limited!");
            if (e.getRetryAfterSeconds() != null) {
                log.warn("Retry after: {} seconds", e.getRetryAfterSeconds());
            }

        } catch (DingTalkResourceNotFoundException e) {
            // 资源未找到
            log.error("Resource not found: {} ({})",
                    e.getResourceType(), e.getResourceId());

        } catch (DingTalkCircuitBreakerException e) {
            // 熔断器打开 - 系统保护状态
            log.error("Circuit breaker is {}", e.getCircuitBreakerState());
            log.error("Service temporarily unavailable");

        } catch (DingTalkNetworkException e) {
            // 网络问题
            log.error("Network error: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
            }

        } catch (DingTalkException e) {
            // 其他钉钉API异常
            log.error("DingTalk API error: {}", e.getMessage());
            log.error("Error code: {}", e.getErrorCode());

        } finally {
            loader.shutdown();
        }
    }

    /**
     * 示例5: 批量加载并处理失败
     */
    public static void example5_BatchLoadWithFailureHandling() {
        log.info("=== Example 5: Batch Load with Failure Handling ===");

        DingTalkDocLoader loader = null;
        try {
            loader = new DingTalkDocLoader.Builder()
                    .apiToken("your-api-token-here")
                    .namespace("workspace-id")
                    .batchSize(100)
                    .returnHtml(false)  // 返回纯文本
                    .build();

            // 加载所有文档
            List<Document> documents = loader.load();

            log.info("Successfully loaded {} documents", documents.size());

            // 检查失败的文档
            Set<String> failedDocs = loader.getFailedDocuments();
            if (!failedDocs.isEmpty()) {
                log.warn("Failed to load {} documents:", failedDocs.size());
                failedDocs.forEach(docId ->
                        log.warn("  - Failed document ID: {}", docId));

                // 可以选择重试失败的文档
                retryFailedDocuments(loader, failedDocs);
            }

        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }
    }

    /**
     * 示例6: 获取HTML格式内容
     */
    public static void example6_LoadHtmlContent() {
        log.info("=== Example 6: Load HTML Content ===");

        DingTalkDocLoader loader = null;
        try {
            loader = new DingTalkDocLoader.Builder()
                    .apiToken("your-api-token-here")
                    .namespace("workspace-id")
                    .returnHtml(true)  // 返回HTML格式
                    .build();

            List<Document> documents = loader.load();

            documents.forEach(doc -> {
                String htmlContent = doc.getPageContent();
                log.info("HTML content length: {} bytes", htmlContent.length());

                // 可以进一步处理HTML
                // - 提取特定标签
                // - 转换为Markdown
                // - 提取图片链接等
            });

        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }
    }

    /**
     * 示例7: 文档元数据提取
     */
    public static void example7_ExtractMetadata() {
        log.info("=== Example 7: Extract Metadata ===");

        DingTalkDocLoader loader = null;
        try {
            loader = new DingTalkDocLoader.Builder()
                    .apiToken("your-api-token-here")
                    .namespace("workspace-id")
                    .build();

            List<Document> documents = loader.load();

            documents.forEach(doc -> {
                Map<String, Object> metadata = doc.getMetadata();

                log.info("Document Metadata:");
                log.info("  Title: {}", metadata.get("title"));
                log.info("  Author: {}", metadata.get("author"));
                log.info("  Created: {}", metadata.get("createdAt"));
                log.info("  Updated: {}", metadata.get("updatedAt"));
                log.info("  URL: {}", metadata.get("url"));
                log.info("  Tags: {}", metadata.get("tags"));
                log.info("  Source: {}", metadata.get("source"));

                // 根据元数据进行分类或过滤
                if (metadata.get("tags") != null) {
                    @SuppressWarnings("unchecked")
                    List<String> tags = (List<String>) metadata.get("tags");
                    if (tags.contains("important")) {
                        log.info("  -> This is an important document!");
                    }
                }
            });

        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }
    }

    /**
     * 示例8: 与向量数据库集成
     */
    public static void example8_VectorDatabaseIntegration() {
        log.info("=== Example 8: Vector Database Integration ===");

        DingTalkDocLoader loader = null;
        try {
            loader = new DingTalkDocLoader.Builder()
                    .apiToken("your-api-token-here")
                    .namespace("workspace-id")
                    .build();

            List<Document> documents = loader.load();

            // 文档预处理
            documents.forEach(doc -> {
                // 1. 清理文本
                String content = doc.getPageContent();
                content = content.replaceAll("\\s+", " ").trim();

                // 2. 分块（如果文档很大）
                if (content.length() > 2000) {
                    // 可以使用 TextSplitter 分块
                    log.info("Large document, consider splitting");
                }

                // 3. 添加到向量数据库
                // vectorDB.add(doc);
                log.info("Ready to add to vector DB: {}", doc.getMetadata().get("title"));
            });

        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }
    }

    /**
     * 重试失败的文档
     */
    private static void retryFailedDocuments(DingTalkDocLoader loader, java.util.Set<String> failedDocIds) {
        log.info("Retrying {} failed documents...", failedDocIds.size());

        // 这里可以实现重试逻辑
        // 例如：创建新的加载器，逐个重试失败的文档
        failedDocIds.forEach(docId -> {
            DingTalkDocLoader retryLoader = null;
            try {
                retryLoader = new DingTalkDocLoader.Builder()
                        .apiToken("your-api-token-here")
                        .namespace(loader.getNamespace())
                        .documentId(docId)
                        .build();

                List<Document> docs = retryLoader.load();
                if (!docs.isEmpty()) {
                    log.info("Successfully retried document: {}", docId);
                }

            } catch (Exception e) {
                log.error("Retry failed for document {}: {}", docId, e.getMessage());
            } finally {
                if (retryLoader != null) {
                    retryLoader.shutdown();
                }
            }
        });
    }

    /**
     * 主函数 - 运行所有示例
     */
    public static void main(String[] args) {
        log.info("DingTalk DocLoader Examples");
        log.info("==========================");

        // 运行示例（注释掉需要实际API Token的示例）

        // example1_BasicUsage();
        // example2_LoadSingleDocument();
        example3_UsingPresetConfig();
        // example4_ExceptionHandling();
        // example5_BatchLoadWithFailureHandling();
        // example6_LoadHtmlContent();
        // example7_ExtractMetadata();
        // example8_VectorDatabaseIntegration();

        log.info("Examples completed");
    }
}
