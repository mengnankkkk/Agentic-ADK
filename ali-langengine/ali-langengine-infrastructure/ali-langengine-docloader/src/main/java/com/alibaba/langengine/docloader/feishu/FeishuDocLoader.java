package com.alibaba.langengine.docloader.feishu;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.alibaba.langengine.core.docloader.BaseLoader;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.feishu.config.FeishuConfig;
import com.alibaba.langengine.docloader.feishu.exception.FeishuDocLoaderException;
import com.alibaba.langengine.docloader.feishu.service.FeishuDocInfo;
import com.alibaba.langengine.docloader.feishu.service.FeishuResult;
import com.alibaba.langengine.docloader.feishu.service.FeishuService;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
public class FeishuDocLoader extends BaseLoader {

    private FeishuService service;

    /**
     * 应用Token，用于API认证
     */
    private String appToken;

    /**
     * 文档ID，指定时只加载单个文档
     */
    private String documentId;

    /**
     * 批量加载的起始偏移量
     */
    private Integer offset = 0;

    /**
     * 批量加载的每页大小
     */
    private Integer batchSize = FeishuConfig.DEFAULT_BATCH_SIZE;

    /**
     * 飞书文档的域名
     */
    private String domain = FeishuConfig.DEFAULT_DOMAIN;

    /**
     * 是否返回HTML内容
     */
    private boolean returnHtml = false;

    /**
     * I/O专用线程池
     */
    private ExecutorService ioExecutor;
    


    /**
     * 构造函数
     */
    public FeishuDocLoader(String appToken) {
        this.appToken = appToken;
        this.service = new FeishuService();
        this.ioExecutor = new ThreadPoolExecutor(
            FeishuConfig.DEFAULT_CORE_POOL_SIZE, FeishuConfig.DEFAULT_MAX_POOL_SIZE, 
            FeishuConfig.DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> new Thread(r, "feishu-doc-loader-" + System.currentTimeMillis()),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    /**
     * 设置服务（用于测试）
     */
    public void setService(FeishuService service) {
        this.service = service;
    }

    /**
     * 加载文档
     * 根据配置加载单个文档或批量加载文档
     *
     * @return 文档列表
     */
    @Override
    public List<Document> load() {
        validateConfiguration();
        return documentId == null || documentId.isEmpty() ? loadBatchDocuments() : loadSingleDocument();
    }

    /**
     * 配置验证
     */
    private void validateConfiguration() {
        if (service == null) {
            throw new IllegalStateException("FeishuService not initialized");
        }
        if (appToken == null || appToken.isEmpty()) {
            throw new IllegalStateException("appToken is required");
        }
        if (batchSize > FeishuConfig.MAX_BATCH_SIZE) {
            log.warn("batchSize {} exceeds maximum {}, using maximum", batchSize, FeishuConfig.MAX_BATCH_SIZE);
            batchSize = FeishuConfig.MAX_BATCH_SIZE;
        }
    }

    /**
     * 加载单个文档
     */
    private List<Document> loadSingleDocument() {
        log.info("Loading single document: {}", documentId);
        
        try {
            FeishuResult<FeishuDocInfo> detailResult = service.getDocumentDetail(appToken, documentId);
            if (detailResult == null || detailResult.getData() == null) {
                log.warn("Document not found or empty result: {}", documentId);
                return Collections.emptyList();
            }

            Document document = createDocumentFromInfo(documentId, detailResult.getData());
            if (document == null) {
                log.warn("Failed to create document from info: {}", documentId);
                return Collections.emptyList();
            }
            
            return Collections.singletonList(document);
        } catch (Exception e) {
            log.error("Error loading single document {}: {}", documentId, e.getMessage(), e);
            throw new FeishuDocLoaderException("LOAD_ERROR", "Failed to load document: " + documentId, e);
        }
    }

    /**
     * 批量加载文档
     * 使用专用I/O线程池进行并发处理
     */
    private List<Document> loadBatchDocuments() {
        log.info("Starting batch document loading with batchSize: {}", batchSize);
        
        List<Document> documents = new ArrayList<>();
        ConcurrentLinkedQueue<String> failedDocuments = new ConcurrentLinkedQueue<>();
        int currentOffset = offset;
        int totalProcessed = 0;

        do {
            FeishuResult<List<FeishuDocInfo>> batchResult = service.getDocumentList(appToken, currentOffset, batchSize);
            List<FeishuDocInfo> docInfos = batchResult.getData();
            
            if (docInfos == null || docInfos.isEmpty()) {
                log.info("No more documents found, stopping batch loading");
                break;
            }

            log.info("Processing batch: offset={}, size={}", currentOffset, docInfos.size());

            // 并发获取文档详情
            List<CompletableFuture<Document>> futures = docInfos.parallelStream()
                .map(docInfo -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return fetchDocumentDetail(docInfo.getDocumentId());
                    } catch (Exception e) {
                        log.warn("Failed to fetch document detail: {}", docInfo.getDocumentId(), e);
                        failedDocuments.add(docInfo.getDocumentId());
                        return null;
                    }
                }, ioExecutor))
                .collect(Collectors.toList());

            // 收集结果
            List<Document> batchDocuments = futures.stream()
                .map(CompletableFuture::join)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

            documents.addAll(batchDocuments);
            totalProcessed += docInfos.size();
            currentOffset += batchSize;

            log.info("Batch completed: processed={}, successful={}, total_processed={}", 
                docInfos.size(), batchDocuments.size(), totalProcessed);

        } while (true);

        // 报告失败的文档
        if (!failedDocuments.isEmpty()) {
            log.warn("Failed to load {} documents: {}", failedDocuments.size(), failedDocuments);
        }

        log.info("Batch loading completed: total_documents={}, failed={}", documents.size(), failedDocuments.size());
        return documents;
    }

    /**
     * 获取文档详情
     */
    private Document fetchDocumentDetail(String docId) {
        FeishuResult<FeishuDocInfo> detailResult = service.getDocumentDetail(appToken, docId);
        return detailResult.getData() != null ? createDocumentFromInfo(docId, detailResult.getData()) : null;
    }

    /**
     * 从FeishuDocInfo创建Document对象
     */
    private Document createDocumentFromInfo(String docId, FeishuDocInfo docInfo) {
        if (docInfo == null) {
            log.warn("DocInfo is null for document: {}", docId);
            return null;
        }
        
        if (isContentEmpty(docInfo)) {
            log.debug("Document has no content: {}", docId);
            return null;
        }

        Document document = new Document();
        document.setUniqueId(docId);
        
        Map<String, Object> metadata = createMetadata(docId, docInfo);
        document.setMetadata(metadata);
        
        String content = extractContent(docInfo);
        document.setPageContent(content);
        
        return document;
    }
    
    /**
     * 检查内容是否为空
     */
    private boolean isContentEmpty(FeishuDocInfo docInfo) {
        return (docInfo.getContent() == null || docInfo.getContent().trim().isEmpty()) && 
               (docInfo.getContentHtml() == null || docInfo.getContentHtml().trim().isEmpty());
    }
    
    /**
     * 创建元数据
     */
    private Map<String, Object> createMetadata(String docId, FeishuDocInfo docInfo) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("url", getDomain() + "docs/" + docId);
        metadata.put("title", Optional.ofNullable(docInfo.getTitle()).orElse("Untitled"));
        metadata.put("ownerId", docInfo.getOwnerId());
        metadata.put("createdAt", docInfo.getCreatedAt());
        metadata.put("updatedAt", docInfo.getUpdatedAt());
        metadata.put("documentType", docInfo.getDocumentType());
        metadata.put("documentSize", docInfo.getDocumentSize());
        return metadata;
    }
    
    /**
     * 提取文档内容
     */
    private String extractContent(FeishuDocInfo docInfo) {
        if (returnHtml && docInfo.getContentHtml() != null) {
            return docInfo.getContentHtml();
        }
        
        if (docInfo.getContent() != null && !docInfo.getContent().trim().isEmpty()) {
            return docInfo.getContent();
        }
        
        if (docInfo.getContentHtml() != null && !docInfo.getContentHtml().trim().isEmpty()) {
            return cleanContent(docInfo.getContentHtml());
        }
        
        return "";
    }

    /**
     * 清理HTML内容，提取纯文本
     */
    private String cleanContent(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return "";
        }
        // 简单的HTML标签清理
        return htmlContent.replaceAll("<[^>]+>", "").trim();
    }

    /**
     * 关闭资源
     */
    public void close() {
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
            try {
                if (!ioExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("Executor did not terminate gracefully, forcing shutdown");
                    ioExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for executor termination");
                ioExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Builder模式
     */
    public static class Builder {
        private String appId;
        private String appSecret;
        private Long timeout = 30L;
        private String appToken;
        private String documentId;
        private Integer offset = 0;
        private Integer batchSize = FeishuConfig.DEFAULT_BATCH_SIZE;
        private String domain = FeishuConfig.DEFAULT_DOMAIN;
        private boolean returnHtml = false;

        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder appSecret(String appSecret) {
            this.appSecret = appSecret;
            return this;
        }

        public Builder timeout(Long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder appToken(String appToken) {
            this.appToken = appToken;
            return this;
        }

        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder offset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public Builder batchSize(Integer batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder returnHtml(boolean returnHtml) {
            this.returnHtml = returnHtml;
            return this;
        }

        public FeishuDocLoader build() {
            if (appToken == null || appToken.isEmpty()) {
                throw new IllegalArgumentException("appToken is required");
            }

            FeishuDocLoader loader = new FeishuDocLoader(appToken);
            loader.setDocumentId(documentId);
            loader.setOffset(offset);
            loader.setBatchSize(batchSize);
            loader.setDomain(domain);
            loader.setReturnHtml(returnHtml);
            
            return loader;
        }
    }
}