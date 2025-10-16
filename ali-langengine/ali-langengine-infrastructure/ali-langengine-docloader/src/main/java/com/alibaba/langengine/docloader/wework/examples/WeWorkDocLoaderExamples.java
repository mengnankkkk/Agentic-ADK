package com.alibaba.langengine.docloader.wework.examples;

import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.wework.WeWorkDocLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class WeWorkDocLoaderExamples {

    /**
     * 示例1: 批量加载文档
     */
    public static void example1_BatchLoadDocuments() {
        log.info("=== Example 1: Batch Load Documents ===");

        WeWorkDocLoader loader = null;
        try {
            loader = new WeWorkDocLoader.Builder()
                    .apiToken("your-api-token-here")
                    .namespace("your-corp-id")
                    .batchSize(20)  // 设置批次大小
                    .returnHtml(false)  // 返回纯文本
                    .timeout(30L)  // 30秒超时
                    .build();

            List<Document> documents = loader.load();

            log.info("Loaded {} documents", documents.size());

            // 处理文档
            for (Document doc : documents) {
                log.info("Document: {} - {}", 
                    doc.getMetadata().get("title"), 
                    doc.getPageContent().length() + " chars");
            }

        } catch (Exception e) {
            log.error("Error loading documents", e);
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

        WeWorkDocLoader loader = null;
        try {
            loader = new WeWorkDocLoader.Builder()
                    .apiToken("your-api-token-here")
                    .namespace("your-corp-id")
                    .documentId("doc-id-12345")  // 指定文档ID
                    .returnHtml(true)  // 返回HTML格式
                    .build();

            List<Document> documents = loader.load();

            if (!documents.isEmpty()) {
                Document doc = documents.get(0);
                log.info("Title: {}", doc.getMetadata().get("title"));
                log.info("Author: {}", doc.getMetadata().get("author"));
                log.info("Created: {}", doc.getMetadata().get("createdAt"));
                log.info("Content length: {}", doc.getPageContent().length());
                log.info("Tags: {}", doc.getMetadata().get("tags"));
            }

        } catch (Exception e) {
            log.error("Error loading single document", e);
        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }
    }

    /**
     * 示例3: 高性能批量加载
     */
    public static void example3_HighPerformanceBatchLoad() {
        log.info("=== Example 3: High Performance Batch Load ===");

        WeWorkDocLoader loader = null;
        try {
            loader = new WeWorkDocLoader.Builder()
                    .apiToken("your-api-token-here")
                    .namespace("your-corp-id")
                    .batchSize(50)  // 较大的批次大小
                    .timeout(60L)  // 较长的超时时间
                    .returnHtml(false)
                    .build();

            long startTime = System.currentTimeMillis();
            List<Document> documents = loader.load();
            long endTime = System.currentTimeMillis();

            log.info("Loaded {} documents in {} ms", 
                documents.size(), endTime - startTime);

            // 统计信息
            int totalChars = documents.stream()
                .mapToInt(doc -> doc.getPageContent().length())
                .sum();
            
            log.info("Total content: {} characters", totalChars);
            log.info("Average document size: {} characters", 
                documents.isEmpty() ? 0 : totalChars / documents.size());

        } catch (Exception e) {
            log.error("Error in high performance batch load", e);
        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }
    }

    /**
     * 示例4: 错误处理和重试
     */
    public static void example4_ErrorHandlingAndRetry() {
        log.info("=== Example 4: Error Handling and Retry ===");

        WeWorkDocLoader loader = null;
        try {
            loader = new WeWorkDocLoader.Builder()
                    .apiToken("invalid-token")  // 故意使用无效token
                    .namespace("your-corp-id")
                    .batchSize(10)
                    .build();

            List<Document> documents = loader.load();
            log.info("Loaded {} documents", documents.size());

        } catch (IllegalArgumentException e) {
            log.error("Configuration error: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Runtime error (possibly network/auth): {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error", e);
        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }
    }

    /**
     * 示例5: 使用不同的配置
     */
    public static void example5_DifferentConfigurations() {
        log.info("=== Example 5: Different Configurations ===");

        // 配置1: 快速预览模式
        WeWorkDocLoader quickLoader = new WeWorkDocLoader.Builder()
                .apiToken("your-api-token-here")
                .namespace("your-corp-id")
                .batchSize(5)  // 小批次，快速获取
                .timeout(10L)  // 短超时
                .returnHtml(false)
                .build();

        // 配置2: 完整内容模式
        WeWorkDocLoader fullLoader = new WeWorkDocLoader.Builder()
                .apiToken("your-api-token-here")
                .namespace("your-corp-id")
                .batchSize(100)  // 大批次
                .timeout(120L)  // 长超时
                .returnHtml(true)  // 包含HTML格式
                .build();

        try {
            // 快速模式
            log.info("Quick mode:");
            List<Document> quickDocs = quickLoader.load();
            log.info("Quick load: {} documents", quickDocs.size());

            // 完整模式
            log.info("Full mode:");
            List<Document> fullDocs = fullLoader.load();
            log.info("Full load: {} documents", fullDocs.size());

        } catch (Exception e) {
            log.error("Error in configuration examples", e);
        } finally {
            quickLoader.shutdown();
            fullLoader.shutdown();
        }
    }

    /**
     * 示例6: 文档处理和分析
     */
    public static void example6_DocumentProcessingAndAnalysis() {
        log.info("=== Example 6: Document Processing and Analysis ===");

        WeWorkDocLoader loader = null;
        try {
            loader = new WeWorkDocLoader.Builder()
                    .apiToken("your-api-token-here")
                    .namespace("your-corp-id")
                    .batchSize(20)
                    .build();

            List<Document> documents = loader.load();

            // 分析文档
            analyzeDocuments(documents);

        } catch (Exception e) {
            log.error("Error in document analysis", e);
        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }
    }

    /**
     * 文档分析辅助方法
     */
    private static void analyzeDocuments(List<Document> documents) {
        log.info("=== Document Analysis ===");
        
        if (documents.isEmpty()) {
            log.info("No documents to analyze");
            return;
        }

        // 基本统计
        log.info("Total documents: {}", documents.size());
        
        // 按作者分组
        java.util.Map<String, Long> authorCounts = documents.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                doc -> doc.getMetadata().getOrDefault("author", "Unknown").toString(),
                java.util.stream.Collectors.counting()
            ));
        
        log.info("Documents by author:");
        authorCounts.forEach((author, count) -> 
            log.info("  {}: {} documents", author, count));

        // 内容长度统计
        java.util.IntSummaryStatistics contentLengths = documents.stream()
            .mapToInt(doc -> doc.getPageContent().length())
            .summaryStatistics();
        
        log.info("Content length statistics:");
        log.info("  Min: {} chars", contentLengths.getMin());
        log.info("  Max: {} chars", contentLengths.getMax());
        log.info("  Average: {} chars", (int) contentLengths.getAverage());
        log.info("  Total: {} chars", contentLengths.getSum());

        // 查找最长的文档
        Document longestDoc = documents.stream()
            .max(java.util.Comparator.comparingInt(doc -> doc.getPageContent().length()))
            .orElse(null);
        
        if (longestDoc != null) {
            log.info("Longest document: {} ({} chars)", 
                longestDoc.getMetadata().get("title"), 
                longestDoc.getPageContent().length());
        }
    }

    /**
     * 主方法：运行所有示例
     */
    public static void main(String[] args) {
        log.info("Starting WeWork DocLoader Examples...");
        
        try {
            example1_BatchLoadDocuments();
            example2_LoadSingleDocument();
            example3_HighPerformanceBatchLoad();
            example4_ErrorHandlingAndRetry();
            example5_DifferentConfigurations();
            example6_DocumentProcessingAndAnalysis();
        } catch (Exception e) {
            log.error("Error running examples", e);
        }
        
        log.info("WeWork DocLoader Examples completed.");
    }
}
