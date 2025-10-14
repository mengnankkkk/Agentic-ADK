package com.alibaba.langengine.docloader.dingtalk;

import com.alibaba.langengine.core.docloader.BaseLoader;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.dingtalk.service.DingTalkDocInfo;
import com.alibaba.langengine.docloader.dingtalk.service.DingTalkResult;
import com.alibaba.langengine.docloader.dingtalk.service.DingTalkService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Slf4j
@Data
public class DingTalkDocLoader extends BaseLoader {

    private DingTalkService service;
    
    /**
     * API访问令牌
     */
    private String apiToken;
    
    /**
     * 命名空间（团队/知识库标识）
     */
    private String namespace;
    
    /**
     * 文档ID（可选，用于加载单个文档）
     */
    private String documentId;
    
    /**
     * 钉钉文档域名
     */
    private String domain = "https://docs.dingtalk.com/";
    
    /**
     * 批量加载时的批次大小
     */
    private int batchSize = 50;
    
    /**
     * 是否返回HTML内容
     */
    private boolean returnHtml = false;
    
    /**
     * 专用于I/O任务的线程池
     */
    private ExecutorService ioExecutor;
    
    /**
     * 记录获取失败的文档ID
     */
    private final Set<String> failedDocuments = ConcurrentHashMap.newKeySet();

    public DingTalkDocLoader(String apiToken, Long timeout) {
        this.apiToken = apiToken;
        this.service = new DingTalkService(apiToken, Duration.ofSeconds(timeout));
        // Use fixed thread pool instead of Virtual Threads (Java 8 compatible)
        this.ioExecutor = Executors.newFixedThreadPool(10);
    }

    /**
     * 加载文档
     */
    @Override
    public List<Document> load() {
        validateConfiguration();
        
        try {
            return StringUtils.isEmpty(documentId) ? loadBatchDocuments() : loadSingleDocument();
        } finally {
            shutdown();
        }
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
            try {
                if (!ioExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    ioExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                ioExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (service != null) {
            service.shutdown();
        }
    }

    /**
     * 加载单个文档
     */
    private List<Document> loadSingleDocument() {
        log.info("Loading single document: {}", documentId);
        
        DingTalkResult<DingTalkDocInfo> result = service.getDocumentDetail(namespace, documentId);
        if (result.getData() == null) {
            log.warn("Document not found: {}", documentId);
            return new ArrayList<>();
        }
        
        Document document = createDocumentFromInfo(result.getData());
        return document != null ? Collections.singletonList(document) : new ArrayList<>();
    }

    /**
     * 批量加载文档（增强版）
     */
    private List<Document> loadBatchDocuments() {
        log.info("Starting batch document loading for namespace: {}", namespace);
        
        List<Document> allDocuments = Collections.synchronizedList(new ArrayList<>());
        int offset = 0;
        int totalProcessed = 0;
        
        do {
            DingTalkResult<List<DingTalkDocInfo>> batchResult = 
                service.getDocumentList(namespace, offset, batchSize);
            
            List<DingTalkDocInfo> docInfos = batchResult.getData();
            if (docInfos == null || docInfos.isEmpty()) {
                log.info("No more documents found, stopping batch loading");
                break;
            }
            
            log.info("Processing batch: offset={}, size={}", offset, docInfos.size());
            
            // 分批处理，避免过多并发
            List<List<DingTalkDocInfo>> chunks = partitionList(docInfos, 10);
            
            for (List<DingTalkDocInfo> chunk : chunks) {
                List<CompletableFuture<Document>> futures = chunk.stream()
                    .map(docInfo -> CompletableFuture.supplyAsync(() -> 
                        fetchDocumentDetailSafely(docInfo.getId()), ioExecutor)
                        .orTimeout(30, TimeUnit.SECONDS))
                    .collect(Collectors.toList());
                
                // 等待当前批次完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .join();
                
                List<Document> chunkDocuments = futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            log.warn("Future execution failed: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                
                allDocuments.addAll(chunkDocuments);
            }
            
            totalProcessed += docInfos.size();
            
            log.info("Batch completed: processed={}, valid_documents={}, total_processed={}", 
                docInfos.size(), allDocuments.size() - (totalProcessed - docInfos.size()), totalProcessed);
            
            offset += batchSize;
            
        } while (true);
        
        // 报告失败的文档
        if (!failedDocuments.isEmpty()) {
            log.warn("Failed to load {} documents: {}", failedDocuments.size(), failedDocuments);
        }
        
        log.info("Batch loading completed: total_documents={}, failed={}", 
            allDocuments.size(), failedDocuments.size());
        
        return allDocuments;
    }
    
    /**
     * 将列表分割成指定大小的块
     */
    private <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            partitions.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return partitions;
    }

    /**
     * 安全地获取文档详情
     */
    private Document fetchDocumentDetailSafely(String docId) {
        try {
            DingTalkResult<DingTalkDocInfo> result = service.getDocumentDetail(namespace, docId);
            return result.getData() != null ? createDocumentFromInfo(result.getData()) : null;
        } catch (Exception e) {
            log.warn("Failed to fetch document detail for ID: {}, error: {}", docId, e.getMessage());
            failedDocuments.add(docId);
            return null;
        }
    }

    /**
     * 从DingTalkDocInfo创建Document对象
     */
    private Document createDocumentFromInfo(DingTalkDocInfo docInfo) {
        if (StringUtils.isEmpty(docInfo.getBody()) && StringUtils.isEmpty(docInfo.getBodyHtml())) {
            log.debug("Skipping document with empty content: {}", docInfo.getId());
            return null;
        }
        
        Document document = new Document();
        document.setUniqueId(docInfo.getId());
        
        // 构建metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("url", domain + namespace + "/" + docInfo.getId());
        metadata.put("title", docInfo.getTitle());
        metadata.put("author", docInfo.getCreator());
        metadata.put("createdAt", docInfo.getCreatedAt());
        metadata.put("updatedAt", docInfo.getUpdatedAt());
        metadata.put("tags", docInfo.getTags());
        metadata.put("source", "dingtalk");
        
        document.setMetadata(metadata);
        
        // 设置内容
        String content = returnHtml ? docInfo.getBodyHtml() : cleanContent(docInfo.getBody());
        document.setPageContent(content);
        
        return document;
    }

    /**
     * 清理HTML内容，提取纯文本
     */
    private String cleanContent(String content) {
        if (StringUtils.isEmpty(content)) {
            return "";
        }
        
        // 使用Jsoup安全地解析HTML并提取纯文本
        return Jsoup.parse(content).text();
    }

    /**
     * 验证配置
     */
    private void validateConfiguration() {
        if (StringUtils.isEmpty(apiToken)) {
            throw new IllegalArgumentException("API token is required");
        }
        if (StringUtils.isEmpty(namespace)) {
            throw new IllegalArgumentException("Namespace is required");
        }
        if (batchSize <= 0 || batchSize > 100) {
            throw new IllegalArgumentException("Batch size must be between 1 and 100");
        }
    }

    /**
     * Builder模式
     */
    public static class Builder {
        private String apiToken;
        private String namespace;
        private String documentId;
        private String domain = "https://docs.dingtalk.com/";
        private int batchSize = 50;
        private boolean returnHtml = false;
        private Long timeout = 60L;

        public Builder apiToken(String apiToken) {
            this.apiToken = apiToken;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder returnHtml(boolean returnHtml) {
            this.returnHtml = returnHtml;
            return this;
        }

        public Builder timeout(Long timeout) {
            this.timeout = timeout;
            return this;
        }

        public DingTalkDocLoader build() {
            DingTalkDocLoader loader = new DingTalkDocLoader(apiToken, timeout);
            loader.setNamespace(namespace);
            loader.setDocumentId(documentId);
            loader.setDomain(domain);
            loader.setBatchSize(batchSize);
            loader.setReturnHtml(returnHtml);
            return loader;
        }
    }
}