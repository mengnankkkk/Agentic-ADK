package com.alibaba.langengine.docloader.feishu;

import com.alibaba.langengine.core.indexes.Document;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FeishuDocLoaderExample {

    public static void main(String[] args) {
        FeishuDocLoader loader = null;
        try {
            // 使用Builder模式创建加载器
            loader = new FeishuDocLoader.Builder()
                    .appId("your_app_id")
                    .appSecret("your_app_secret")
                    .appToken("your_app_token")
                    .timeout(30L)
                    .batchSize(20)
                    .returnHtml(false)
                    .build();

            // 加载单个文档
            loader.setDocumentId("specific_document_id");
            List<Document> singleDoc = loader.load();
            log.info("Single document loaded successfully, count: {}", singleDoc.size());

            // 批量加载文档
            loader.setDocumentId(null);
            loader.setBatchSize(10);
            List<Document> batchDocs = loader.load();
            log.info("Batch documents loaded successfully, count: {}", batchDocs.size());

        } catch (IllegalArgumentException e) {
            log.error("Invalid configuration: {}", e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.error("Service state error: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during document loading: {}", e.getMessage(), e);
        } finally {
            if (loader != null) {
                try {
                    loader.close();
                    log.debug("Resources closed successfully");
                } catch (Exception e) {
                    log.warn("Error closing resources: {}", e.getMessage());
                }
            }
        }
    }
}