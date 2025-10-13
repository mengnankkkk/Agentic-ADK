package com.alibaba.langengine.docloader.feishu.service;

import com.alibaba.langengine.docloader.feishu.exception.FeishuDocLoaderException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
@Data
public class FeishuService {

    public FeishuService() {
        log.debug("FeishuService initialized");
    }

    /**
     * 获取文档列表
     * 
     * @param appToken 应用令牌
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 文档列表结果
     * @throws IllegalArgumentException 当参数无效时
     */
    public FeishuResult<List<FeishuDocInfo>> getDocumentList(String appToken, Integer offset, Integer limit) {
        validateParameters(appToken, "appToken");
        validateParameters(offset, "offset");
        validateParameters(limit, "limit");
        
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        
        try {
            log.debug("Getting document list: offset={}, limit={}", offset, limit);
            // Mock实现，实际使用时需要真实的API调用
            FeishuResult<List<FeishuDocInfo>> result = new FeishuResult<>();
            result.setCode(0);
            result.setMsg("success");
            return result;
        } catch (Exception e) {
            log.error("Error getting document list: {}", e.getMessage(), e);
            throw new FeishuDocLoaderException("API_ERROR", "Failed to get document list", e);
        }
    }

    /**
     * 获取文档详情
     * 
     * @param appToken 应用令牌
     * @param documentId 文档ID
     * @return 文档详情结果
     * @throws IllegalArgumentException 当参数无效时
     */
    public FeishuResult<FeishuDocInfo> getDocumentDetail(String appToken, String documentId) {
        validateParameters(appToken, "appToken");
        validateParameters(documentId, "documentId");
        
        try {
            log.debug("Getting document detail: {}", documentId);
            // Mock实现，实际使用时需要真实的API调用
            FeishuResult<FeishuDocInfo> result = new FeishuResult<>();
            result.setCode(0);
            result.setMsg("success");
            return result;
        } catch (Exception e) {
            log.error("Error getting document detail for {}: {}", documentId, e.getMessage(), e);
            throw new FeishuDocLoaderException("API_ERROR", "Failed to get document detail: " + documentId, e);
        }
    }
    
    /**
     * 验证参数
     */
    private void validateParameters(Object param, String paramName) {
        if (param == null) {
            throw new IllegalArgumentException(paramName + " cannot be null");
        }
        if (param instanceof String && ((String) param).trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be empty");
        }
    }
}