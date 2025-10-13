package com.alibaba.langengine.docloader.feishu.service;

import lombok.Data;

import java.util.List;


@Data
public class FeishuService {

    public FeishuService() {
        // 简化构造函数
    }

    /**
     * 获取文档列表
     */
    public FeishuResult<List<FeishuDocInfo>> getDocumentList(String appToken, Integer offset, Integer limit) {
        // Mock实现，实际使用时需要真实的API调用
        return new FeishuResult<>();
    }

    /**
     * 获取文档详情
     */
    public FeishuResult<FeishuDocInfo> getDocumentDetail(String appToken, String documentId) {
        // Mock实现，实际使用时需要真实的API调用
        return new FeishuResult<>();
    }
}