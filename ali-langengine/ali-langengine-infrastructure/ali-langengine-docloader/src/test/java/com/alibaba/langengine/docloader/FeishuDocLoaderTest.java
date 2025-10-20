/**
 * Copyright (C) 2024 AIDC-AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.langengine.docloader;

import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.feishu.FeishuDocLoader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 飞书文档加载器测试
 *
 * @author Libres-coder
 */
@Slf4j
public class FeishuDocLoaderTest {

    /**
     * 测试加载单个飞书文档
     * 需要配置真实的APP_ID、APP_SECRET和DOC_TOKEN
     */
    @Test
    public void testLoadSingleDocument() {
        // 配置飞书应用凭证
        String appId = System.getenv("FEISHU_APP_ID");
        String appSecret = System.getenv("FEISHU_APP_SECRET");
        String docToken = System.getenv("FEISHU_DOC_TOKEN");

        if (appId == null || appSecret == null || docToken == null) {
            log.info("Skipping test: FEISHU_APP_ID, FEISHU_APP_SECRET or FEISHU_DOC_TOKEN not set");
            return;
        }

        FeishuDocLoader loader = new FeishuDocLoader(appId, appSecret, 60L);
        loader.setDocToken(docToken);

        List<Document> documents = loader.load();
        
        log.info("Loaded {} documents", documents.size());
        for (Document doc : documents) {
            log.info("Document ID: {}", doc.getUniqueId());
            log.info("Content length: {}", doc.getPageContent().length());
            log.info("Metadata: {}", doc.getMetadata());
            log.info("Content preview: {}", doc.getPageContent().substring(0, Math.min(100, doc.getPageContent().length())));
        }
    }

    /**
     * 测试批量加载知识库文档
     * 需要配置真实的APP_ID、APP_SECRET和SPACE_ID
     */
    @Test
    public void testLoadSpaceDocuments() {
        String appId = System.getenv("FEISHU_APP_ID");
        String appSecret = System.getenv("FEISHU_APP_SECRET");
        String spaceId = System.getenv("FEISHU_SPACE_ID");

        if (appId == null || appSecret == null || spaceId == null) {
            log.info("Skipping test: FEISHU_APP_ID, FEISHU_APP_SECRET or FEISHU_SPACE_ID not set");
            return;
        }

        FeishuDocLoader loader = new FeishuDocLoader(appId, appSecret, 60L);
        loader.setSpaceId(spaceId);
        loader.setPageSize(10);

        List<Document> documents = loader.load();
        
        log.info("Loaded {} documents from space", documents.size());
        for (Document doc : documents) {
            log.info("Document ID: {}, Title: {}", 
                doc.getUniqueId(), 
                doc.getMetadata().get("title"));
        }
    }

    /**
     * 测试fetchContent方法
     */
    @Test
    public void testFetchContent() {
        String appId = System.getenv("FEISHU_APP_ID");
        String appSecret = System.getenv("FEISHU_APP_SECRET");
        String docToken = System.getenv("FEISHU_DOC_TOKEN");

        if (appId == null || appSecret == null || docToken == null) {
            log.info("Skipping test: FEISHU_APP_ID, FEISHU_APP_SECRET or FEISHU_DOC_TOKEN not set");
            return;
        }

        FeishuDocLoader loader = new FeishuDocLoader(appId, appSecret, 60L);
        
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("docToken", docToken);
        
        List<Document> documents = loader.fetchContent(meta);
        
        log.info("Fetched {} documents", documents.size());
        for (Document doc : documents) {
            log.info("Document ID: {}", doc.getUniqueId());
        }
    }
}
