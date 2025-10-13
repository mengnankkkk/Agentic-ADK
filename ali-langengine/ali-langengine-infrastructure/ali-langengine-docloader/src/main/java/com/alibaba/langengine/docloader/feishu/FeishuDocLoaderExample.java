package com.alibaba.langengine.docloader.feishu;

import com.alibaba.langengine.core.indexes.Document;

import java.util.List;


public class FeishuDocLoaderExample {

    public static void main(String[] args) {
        // 使用Builder模式创建加载器
        FeishuDocLoader loader = new FeishuDocLoader.Builder()
                .appId("your_app_id")
                .appSecret("your_app_secret")
                .appToken("your_app_token")
                .timeout(30L)
                .batchSize(20)
                .returnHtml(false)
                .build();

        try {
            // 加载单个文档
            loader.setDocumentId("specific_document_id");
            List<Document> singleDoc = loader.load();
            System.out.println("Single document loaded: " + singleDoc.size());

            // 批量加载文档
            loader.setDocumentId(null);
            loader.setBatchSize(10);
            List<Document> batchDocs = loader.load();
            System.out.println("Batch documents loaded: " + batchDocs.size());

        } finally {
            // 关闭资源
            loader.close();
        }
    }
}