package com.alibaba.langengine.docloader.wework;

import com.alibaba.langengine.core.docloader.BaseLoader;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.wework.service.WeWorkDocInfo;
import com.alibaba.langengine.docloader.wework.service.WeWorkResult;
import com.alibaba.langengine.docloader.wework.service.WeWorkService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class WeWorkDocLoader extends BaseLoader {

    private WeWorkService service;
    
    /**
     * API访问令牌
     */
    private String apiToken;
    
    /**
     * 命名空间（企业ID/知识库标识）
     */
    private String namespace;
    
    /**
     * 文档ID（可选，用于加载单个文档）
     */
    private String documentId;
    
    /**
     * 企业微信文档域名
     */
    private String domain = "https://work.weixin.qq.com/";
    
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
     * 使用固定线程池代替虚拟线程以确保Java 8兼容性
     */
    private ExecutorService ioExecutor;
    
    /**
     * 记录获取失败的文档ID
     */
    private final Set<String> failedDocuments = ConcurrentHashMap.newKeySet();

    /**
     * 构造函数
     * 
     * @param apiToken API访问令牌
     * @param timeout 超时时间（秒）
     */
    public WeWorkDocLoader(String apiToken, Long timeout) {
        this.apiToken = apiToken;
        this.service = new WeWorkService(apiToken, Duration.ofSeconds(timeout));
        // 使用固定线程池，确保Java 8兼容性
        this.ioExecutor = Executors.newFixedThreadPool(10);
    }

    /**
     * 加载文档
     * 根据配置智能分派到单文档或批量加载
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
     * 关闭资源，确保线程池正确关闭
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
        
        WeWorkResult<WeWorkDocInfo> result = service.getDocumentDetail(namespace, documentId);
        if (result.getData() == null) {
            log.warn("Document not found: {}", documentId);
            return new ArrayList<>();
        }
        
        Document document = createDocumentFromInfo(result.getData());
        return document != null ? Collections.singletonList(document) : new ArrayList<>();
    }

    /**
     * 批量加载文档（增强版）
     * 使用专用线程池进行并发I/O，确保性能和稳定性
     */
    private List<Document> loadBatchDocuments() {
        log.info("Starting batch document loading for namespace: {}", namespace);
        
        List<Document> allDocuments = Collections.synchronizedList(new ArrayList<>());
        int offset = 0;
        int totalProcessed = 0;
        
        do {
            // 获取当前批次的文档列表
            WeWorkResult<List<WeWorkDocInfo>> batchResult = 
                service.getDocumentList(namespace, offset, batchSize);
            
            List<WeWorkDocInfo> docInfos = batchResult.getData();
            if (docInfos == null || docInfos.isEmpty()) {
                log.info("No more documents found, stopping batch loading");
                break; // 以API返回空列表作为最可靠的终止条件
            }
            
            log.info("Processing batch: offset={}, size={}", offset, docInfos.size());
            
            // 分批处理，避免过多并发导致资源耗尽
            List<List<WeWorkDocInfo>> chunks = partitionList(docInfos, 10);
            
            for (List<WeWorkDocInfo> chunk : chunks) {
                // 使用CompletableFuture进行并发处理
                List<CompletableFuture<Document>> futures = chunk.stream()
                    .map(docInfo -> (CompletableFuture<Document>) CompletableFuture.supplyAsync(() -> 
                        fetchDocumentDetailSafely(docInfo.getId()), ioExecutor))
                    .collect(Collectors.toList());
                
                // 等待当前批次完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .join();
                
                // 收集成功的结果
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
            
        } while (true); // 使用do-while确保至少执行一次
        
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
     * 
     * @param list 原始列表
     * @param chunkSize 块大小
     * @return 分割后的列表
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
     * 具备异常处理，确保单个文档失败不影响整体流程
     * 
     * @param docId 文档ID
     * @return Document对象或null
     */
    private Document fetchDocumentDetailSafely(String docId) {
        try {
            WeWorkResult<WeWorkDocInfo> result = service.getDocumentDetail(namespace, docId);
            return result.getData() != null ? createDocumentFromInfo(result.getData()) : null;
        } catch (Exception e) {
            log.warn("Failed to fetch document detail for ID: {}, error: {}", docId, e.getMessage());
            failedDocuments.add(docId); // 线程安全地记录失败
            return null;
        }
    }

    /**
     * 从WeWorkDocInfo创建Document对象
     * 详尽填充metadata，确保信息完整性
     * 
     * @param docInfo 企业微信文档信息
     * @return Document对象
     */
    private Document createDocumentFromInfo(WeWorkDocInfo docInfo) {
        if (StringUtils.isEmpty(docInfo.getBody()) && StringUtils.isEmpty(docInfo.getBodyHtml())) {
            log.debug("Skipping document with empty content: {}", docInfo.getId());
            return null;
        }
        
        Document document = new Document();
        document.setUniqueId(docInfo.getId());
        
        // 构建详尽的metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("url", domain + namespace + "/" + docInfo.getId());
        metadata.put("title", docInfo.getTitle());
        metadata.put("author", docInfo.getCreator());
        metadata.put("createdAt", docInfo.getCreatedAt());
        metadata.put("updatedAt", docInfo.getUpdatedAt());
        metadata.put("contentUpdatedAt", docInfo.getContentUpdatedAt());
        metadata.put("publishedAt", docInfo.getPublishedAt());
        metadata.put("tags", docInfo.getTags());
        metadata.put("source", "wework");
        metadata.put("readCount", docInfo.getReadCount());
        metadata.put("likeCount", docInfo.getLikeCount());
        metadata.put("isPublic", docInfo.getIsPublic());
        
        document.setMetadata(metadata);
        
        // 设置内容，根据配置选择HTML或纯文本
        String content = returnHtml ? docInfo.getBodyHtml() : cleanContent(docInfo.getBody());
        document.setPageContent(content);
        
        return document;
    }

    /**
     * 清理HTML内容，提取纯文本
     * 使用Jsoup安全地解析HTML，避免正则表达式的不可靠性
     * 
     * @param content 原始内容
     * @return 清理后的纯文本
     */
    private String cleanContent(String content) {
        if (StringUtils.isEmpty(content)) {
            return "";
        }
        
        // 使用Jsoup安全地解析HTML并提取纯文本
        return Jsoup.parse(content).text();
    }

    /**
     * 配置验证
     * 实现"快速失败"原则，在加载开始前检查必要参数
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
     * 提供优雅的链式配置接口
     */
    public static class Builder {
        private String apiToken;
        private String namespace;
        private String documentId;
        private String domain = "https://work.weixin.qq.com/";
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

        public WeWorkDocLoader build() {
            if (StringUtils.isEmpty(apiToken)) {
                throw new IllegalArgumentException("API token is required");
            }
            if (StringUtils.isEmpty(namespace)) {
                throw new IllegalArgumentException("Namespace is required");
            }
            
            WeWorkDocLoader loader = new WeWorkDocLoader(apiToken, timeout);
            loader.setNamespace(namespace);
            loader.setDocumentId(documentId);
            loader.setDomain(domain);
            loader.setBatchSize(batchSize);
            loader.setReturnHtml(returnHtml);
            return loader;
        }
    }
}
