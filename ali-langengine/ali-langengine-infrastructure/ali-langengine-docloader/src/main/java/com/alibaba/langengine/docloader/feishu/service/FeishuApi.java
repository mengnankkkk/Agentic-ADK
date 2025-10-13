package com.alibaba.langengine.docloader.feishu.service;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

/**
 * 飞书API接口定义
 * 使用Retrofit定义清晰的API接口
 *
 * @author AI Assistant
 */
public interface FeishuApi {

    /**
     * 获取文档列表
     */
    @GET("open-apis/docx/v1/documents")
    Single<FeishuResult<List<FeishuDocInfo>>> getDocumentList(
            @Header("Authorization") String authorization,
            @Query("page_token") String pageToken,
            @Query("page_size") Integer pageSize);

    /**
     * 获取文档详情
     */
    @GET("open-apis/docx/v1/documents/{document_id}")
    Single<FeishuResult<FeishuDocInfo>> getDocumentDetail(
            @Header("Authorization") String authorization,
            @Path("document_id") String documentId);

    /**
     * 获取文档内容
     */
    @GET("open-apis/docx/v1/documents/{document_id}/content")
    Single<FeishuResult<FeishuDocContent>> getDocumentContent(
            @Header("Authorization") String authorization,
            @Path("document_id") String documentId);
}