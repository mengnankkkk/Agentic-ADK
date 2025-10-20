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
import com.alibaba.langengine.docloader.dingtalk.DingTalkDocLoader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 钉钉文档加载器测试
 *
 * @author Libres-coder
 */
@Slf4j
public class DingTalkDocLoaderTest {

    /**
     * 测试加载单个钉钉文档
     * 需要配置真实的APP_KEY、APP_SECRET和DOC_ID
     */
    @Test
    public void testLoadSingleDocument() {
        String appKey = System.getenv("DINGTALK_APP_KEY");
        String appSecret = System.getenv("DINGTALK_APP_SECRET");
        String docId = System.getenv("DINGTALK_DOC_ID");

        if (appKey == null || appSecret == null || docId == null) {
            log.info("Skipping test: DINGTALK_APP_KEY, DINGTALK_APP_SECRET or DINGTALK_DOC_ID not set");
            return;
        }

        DingTalkDocLoader loader = new DingTalkDocLoader(appKey, appSecret, 60L);
        loader.setDocId(docId);

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
     * 需要配置真实的APP_KEY、APP_SECRET和WORKSPACE_ID
     */
    @Test
    public void testLoadWorkspaceDocuments() {
        String appKey = System.getenv("DINGTALK_APP_KEY");
        String appSecret = System.getenv("DINGTALK_APP_SECRET");
        String workspaceId = System.getenv("DINGTALK_WORKSPACE_ID");

        if (appKey == null || appSecret == null || workspaceId == null) {
            log.info("Skipping test: DINGTALK_APP_KEY, DINGTALK_APP_SECRET or DINGTALK_WORKSPACE_ID not set");
            return;
        }

        DingTalkDocLoader loader = new DingTalkDocLoader(appKey, appSecret, 60L);
        loader.setWorkspaceId(workspaceId);
        loader.setMaxResults(10);

        List<Document> documents = loader.load();
        
        log.info("Loaded {} documents from workspace", documents.size());
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
        String appKey = System.getenv("DINGTALK_APP_KEY");
        String appSecret = System.getenv("DINGTALK_APP_SECRET");
        String docId = System.getenv("DINGTALK_DOC_ID");

        if (appKey == null || appSecret == null || docId == null) {
            log.info("Skipping test: DINGTALK_APP_KEY, DINGTALK_APP_SECRET or DINGTALK_DOC_ID not set");
            return;
        }

        DingTalkDocLoader loader = new DingTalkDocLoader(appKey, appSecret, 60L);
        
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("docId", docId);
        
        List<Document> documents = loader.fetchContent(meta);
        
        log.info("Fetched {} documents", documents.size());
        for (Document doc : documents) {
            log.info("Document ID: {}", doc.getUniqueId());
        }
    }
}
