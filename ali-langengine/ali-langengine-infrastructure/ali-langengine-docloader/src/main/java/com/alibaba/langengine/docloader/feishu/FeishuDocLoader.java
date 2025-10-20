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
package com.alibaba.langengine.docloader.feishu;

import com.alibaba.langengine.core.docloader.BaseLoader;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.feishu.service.*;
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
 * 飞书文档加载器
 *
 * @author Libres-coder
 */
@Slf4j
@Data
public class FeishuDocLoader extends BaseLoader {

    private FeishuService service;

    /**
     * 文档Token，用于加载单个文档
     */
    private String docToken;

    /**
     * 知识库ID，用于批量加载知识库中的文档
     */
    private String spaceId;

    /**
     * 分页大小，默认50
     */
    private Integer pageSize = 50;

    /**
     * 文档类型：docx（新版文档）、doc（旧版文档）
     */
    private String docType = "docx";

    /**
     * 飞书开放平台域名
     */
    private String domain = "https://open.feishu.cn";

    public FeishuDocLoader(String appId, String appSecret, Long timeout) {
        service = new FeishuService(appId, appSecret, Duration.ofSeconds(timeout));
    }
    
    /**
     * 加载文档。
     * 根据配置加载单个文档或批量加载知识库文档。
     *
     * @return 文档列表
     */
    @Override
    public List<Document> load() {
        if (StringUtils.isNotEmpty(docToken)) {
            return loadSingleDocument();
        } else if (StringUtils.isNotEmpty(spaceId)) {
            return loadSpaceDocuments();
        } else {
            log.warn("Neither docToken nor spaceId is provided, returning empty list");
            return new ArrayList<>();
        }
    }
    
    /**
     * 加载单个文档。
     * 当docToken配置时，只加载特定的文档。
     *
     * @return 单个文档的列表
     */
    private List<Document> loadSingleDocument() {
        try {
            FeishuResult<FeishuDocContent> result = service.getDocxRawContent(docToken);
            if (result.getCode() != 0) {
                log.error("Failed to load Feishu document {}: {}", docToken, result.getMsg());
                return new ArrayList<>();
            }

            if (result.getData() == null || StringUtils.isEmpty(result.getData().getContent())) {
                log.warn("Feishu document {} has no content", docToken);
                return new ArrayList<>();
            }

            Document document = createDocumentFromContent(docToken, result.getData());
            List<Document> documents = new ArrayList<>();
            documents.add(document);
            return documents;
        } catch (Exception e) {
            log.error("Error loading Feishu document: {}", docToken, e);
            throw new RuntimeException("Failed to load Feishu document: " + docToken, e);
        }
    }
    
    /**
     * 批量加载知识库文档。
     * 当spaceId配置时，批量加载知识库中的所有文档。
     *
     * @return 文档列表
     */
    private List<Document> loadSpaceDocuments() {
        List<Document> allDocuments = new ArrayList<>();
        String pageToken = null;
        boolean hasMore = true;

        try {
            while (hasMore) {
                FeishuResult<FeishuNodeList> result = service.getSpaceNodes(spaceId, pageToken, pageSize);
                
                if (result.getCode() != 0) {
                    log.error("Failed to load space nodes: {}", result.getMsg());
                    break;
                }

                FeishuNodeList nodeList = result.getData();
                if (nodeList == null || nodeList.getItems() == null) {
                    break;
                }

                // 过滤并加载文档类型的节点
                List<Document> batchDocuments = nodeList.getItems().stream()
                    .filter(node -> "docx".equals(node.getObjType()) || "doc".equals(node.getObjType()))
                    .map(this::loadNodeDocument)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
                
                allDocuments.addAll(batchDocuments);

                // 更新分页信息
                hasMore = nodeList.getHasMore() != null && nodeList.getHasMore();
                pageToken = nodeList.getPageToken();
            }

            log.info("Loaded {} documents from Feishu space {}", allDocuments.size(), spaceId);
            return allDocuments;
        } catch (Exception e) {
            log.error("Error loading Feishu space documents: {}", spaceId, e);
            throw new RuntimeException("Failed to load Feishu space documents: " + spaceId, e);
        }
    }
    
    /**
     * 加载知识库节点对应的文档
     *
     * @param node 知识库节点
     * @return 文档对象或null（如果无法获取文档详情）
     */
    private Document loadNodeDocument(FeishuNodeList.Node node) {
        try {
            String objToken = node.getObjToken();
            FeishuResult<FeishuDocContent> result = service.getDocxRawContent(objToken);
            
            if (result.getCode() != 0) {
                log.warn("Failed to load document for node {}: {}", objToken, result.getMsg());
                return null;
            }

            if (result.getData() == null || StringUtils.isEmpty(result.getData().getContent())) {
                log.debug("Node {} has no content", objToken);
                return null;
            }

            return createDocumentFromNode(objToken, node, result.getData());
        } catch (Exception e) {
            log.error("Error loading document for node {}: {}", node.getNodeToken(), e.getMessage());
            return null;
        }
    }

    /**
     * 根据文档内容创建Document对象
     *
     * @param docToken 文档token
     * @param content 文档内容
     * @return Document对象
     */
    private Document createDocumentFromContent(String docToken, FeishuDocContent content) {
        Document document = new Document();
        document.setUniqueId(docToken);
        document.setPageContent(content.getContent());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "feishu");
        metadata.put("doc_token", docToken);
        metadata.put("doc_type", docType);
        metadata.put("url", domain + "/docx/" + docToken);

        document.setMetadata(metadata);
        return document;
    }

    /**
     * 根据节点信息和内容创建Document对象
     *
     * @param objToken 对象token
     * @param node 节点信息
     * @param content 文档内容
     * @return Document对象
     */
    private Document createDocumentFromNode(String objToken, FeishuNodeList.Node node, FeishuDocContent content) {
        Document document = new Document();
        document.setUniqueId(objToken);
        document.setPageContent(content.getContent());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "feishu");
        metadata.put("space_id", spaceId);
        metadata.put("doc_token", objToken);
        metadata.put("node_token", node.getNodeToken());
        metadata.put("doc_type", node.getObjType());
        metadata.put("title", node.getTitle());
        metadata.put("url", domain + "/docx/" + objToken);

        document.setMetadata(metadata);
        return document;
    }
    
    @Override
    public List<Document> fetchContent(Map<String, Object> documentMeta) {
        if (documentMeta.get("docToken") != null) {
            String tempDocToken = (String) documentMeta.get("docToken");
            String originalDocToken = this.docToken;
            try {
                this.docToken = tempDocToken;
                return load();
            } finally {
                this.docToken = originalDocToken;
            }
        } else if (documentMeta.get("spaceId") != null) {
            String tempSpaceId = (String) documentMeta.get("spaceId");
            String originalSpaceId = this.spaceId;
            try {
                this.spaceId = tempSpaceId;
                return load();
            } finally {
                this.spaceId = originalSpaceId;
            }
        }
        return load();
    }
}
