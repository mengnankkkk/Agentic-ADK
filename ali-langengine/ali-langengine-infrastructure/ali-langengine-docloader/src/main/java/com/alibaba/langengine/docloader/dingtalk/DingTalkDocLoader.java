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
package com.alibaba.langengine.docloader.dingtalk;

import com.alibaba.langengine.core.docloader.BaseLoader;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.dingtalk.service.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 钉钉文档加载器
 *
 * @author Libres-coder
 */
@Slf4j
@Data
@lombok.EqualsAndHashCode(callSuper=false)
public class DingTalkDocLoader extends BaseLoader {

    private DingTalkService service;

    /**
     * 文档ID，用于加载单个文档
     */
    private String docId;

    /**
     * 知识库ID，用于批量加载知识库中的文档
     */
    private String workspaceId;

    /**
     * 用户ID，用于获取知识库列表
     */
    private String userId;

    /**
     * 每次请求的最大文档数，默认20
     */
    private Integer maxResults = 20;

    /**
     * 钉钉开放平台域名
     */
    private String domain = "https://www.dingtalk.com";

    public DingTalkDocLoader(String appKey, String appSecret, Long timeout) {
        service = new DingTalkService(appKey, appSecret, Duration.ofSeconds(timeout));
    }

    /**
     * 加载文档。
     * 根据配置加载单个文档或批量加载知识库文档。
     *
     * @return 文档列表
     */
    @Override
    public List<Document> load() {
        if (StringUtils.isNotEmpty(docId)) {
            return loadSingleDocument();
        } else if (StringUtils.isNotEmpty(workspaceId)) {
            return loadWorkspaceDocuments();
        } else {
            log.warn("Neither docId nor workspaceId is provided, returning empty list");
            return new ArrayList<>();
        }
    }

    /**
     * 加载单个文档。
     *
     * @return 单个文档的列表
     */
    private List<Document> loadSingleDocument() {
        try {
            DingTalkResult<DingTalkDocContent> result = service.getDocContent(docId);
            
            if (result.getErrCode() != 0) {
                log.error("Failed to load DingTalk document {}: {}", docId, result.getErrMsg());
                return new ArrayList<>();
            }

            DingTalkDocContent content = result.getResult();
            if (content == null || StringUtils.isEmpty(content.getDocContent())) {
                log.warn("DingTalk document {} has no content", docId);
                return new ArrayList<>();
            }

            Document document = new Document();
            document.setUniqueId(docId);
            document.setPageContent(content.getDocContent());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "dingtalk");
            metadata.put("doc_id", docId);
            metadata.put("title", content.getDocTitle());
            metadata.put("doc_type", content.getDocType());
            metadata.put("create_time", content.getCreateTime());
            metadata.put("modified_time", content.getModifiedTime());
            metadata.put("creator_id", content.getCreatorId());
            metadata.put("modifier_id", content.getModifierId());
            metadata.put("url", domain + "/doc/" + docId);

            document.setMetadata(metadata);
            List<Document> documents = new ArrayList<>();
            documents.add(document);
            return documents;
        } catch (Exception e) {
            log.error("Error loading DingTalk document: {}", docId, e);
            throw new RuntimeException("Failed to load DingTalk document: " + docId, e);
        }
    }

    /**
     * 批量加载知识库文档。
     *
     * @return 文档列表
     */
    private List<Document> loadWorkspaceDocuments() {
        List<Document> allDocuments = new ArrayList<>();
        String nextToken = null;
        boolean hasMore = true;

        try {
            while (hasMore) {
                DingTalkResult<DingTalkDocList> result = service.getDocList(workspaceId, maxResults, nextToken);
                
                if (result.getErrCode() != 0) {
                    log.error("Failed to load workspace documents: {}", result.getErrMsg());
                    break;
                }

                DingTalkDocList docList = result.getResult();
                if (docList == null || docList.getDocList() == null) {
                    break;
                }

                // 处理当前页的文档
                List<Document> batchDocuments = docList.getDocList().stream()
                    .map(this::loadDocFromInfo)
                    .filter(doc -> doc != null)
                    .collect(Collectors.toList());
                
                allDocuments.addAll(batchDocuments);

                // 更新分页信息
                hasMore = docList.getHasMore() != null && docList.getHasMore();
                nextToken = docList.getNextToken();
            }

            log.info("Loaded {} documents from DingTalk workspace {}", allDocuments.size(), workspaceId);
            return allDocuments;
        } catch (Exception e) {
            log.error("Error loading DingTalk workspace documents: {}", workspaceId, e);
            throw new RuntimeException("Failed to load DingTalk workspace documents: " + workspaceId, e);
        }
    }

    /**
     * 从文档信息加载完整文档
     *
     * @param docInfo 文档信息
     * @return 文档对象
     */
    private Document loadDocFromInfo(DingTalkDocList.DocInfo docInfo) {
        try {
            String docId = docInfo.getDocId();
            DingTalkResult<DingTalkDocContent> result = service.getDocContent(docId);
            
            if (result.getErrCode() != 0) {
                log.warn("Failed to load document {}: {}", docId, result.getErrMsg());
                return null;
            }

            DingTalkDocContent content = result.getResult();
            if (content == null || StringUtils.isEmpty(content.getDocContent())) {
                log.debug("Document {} has no content", docId);
                return null;
            }

            Document document = new Document();
            document.setUniqueId(docId);
            document.setPageContent(content.getDocContent());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "dingtalk");
            metadata.put("workspace_id", workspaceId);
            metadata.put("doc_id", docId);
            metadata.put("title", docInfo.getDocTitle());
            metadata.put("doc_type", docInfo.getDocType());
            metadata.put("create_time", docInfo.getCreateTime());
            metadata.put("modified_time", docInfo.getModifiedTime());
            metadata.put("creator_id", docInfo.getCreatorId());
            metadata.put("modifier_id", docInfo.getModifierId());
            metadata.put("url", domain + "/doc/" + docId);

            document.setMetadata(metadata);
            return document;
        } catch (Exception e) {
            log.error("Error loading document {}: {}", docInfo.getDocId(), e.getMessage());
            return null;
        }
    }

    @Override
    public List<Document> fetchContent(Map<String, Object> documentMeta) {
        if (documentMeta.get("docId") != null) {
            String tempDocId = (String) documentMeta.get("docId");
            String originalDocId = this.docId;
            try {
                this.docId = tempDocId;
                return load();
            } finally {
                this.docId = originalDocId;
            }
        } else if (documentMeta.get("workspaceId") != null) {
            String tempWorkspaceId = (String) documentMeta.get("workspaceId");
            String originalWorkspaceId = this.workspaceId;
            try {
                this.workspaceId = tempWorkspaceId;
                return load();
            } finally {
                this.workspaceId = originalWorkspaceId;
            }
        }
        return load();
    }
}
