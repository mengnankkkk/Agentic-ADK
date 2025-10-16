package com.alibaba.langengine.docloader.wework.service;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;


public interface WeWorkApi {

    /**
     * 获取文档列表
     * 
     * @param accessToken 访问令牌
     * @param namespace 命名空间（企业ID/知识库标识）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 文档列表结果
     */
    @GET("cgi-bin/wedoc/doc_list")
    Single<WeWorkResult<List<WeWorkDocInfo>>> getDocumentList(
            @Query("access_token") String accessToken,
            @Query("spaceid") String namespace,
            @Query("offset") Integer offset,
            @Query("limit") Integer limit
    );

    /**
     * 获取文档详情
     * 
     * @param accessToken 访问令牌
     * @param namespace 命名空间（企业ID/知识库标识）
     * @param documentId 文档ID
     * @return 文档详情结果
     */
    @GET("cgi-bin/wedoc/doc_get")
    Single<WeWorkResult<WeWorkDocInfo>> getDocumentDetail(
            @Query("access_token") String accessToken,
            @Query("spaceid") String namespace,
            @Query("docid") String documentId
    );

    /**
     * 获取文档内容
     * 
     * @param accessToken 访问令牌
     * @param documentId 文档ID
     * @return 文档内容结果
     */
    @GET("cgi-bin/wedoc/doc_content")
    Single<WeWorkResult<WeWorkDocInfo>> getDocumentContent(
            @Query("access_token") String accessToken,
            @Query("docid") String documentId
    );

    /**
     * 搜索文档
     * 
     * @param accessToken 访问令牌
     * @param namespace 命名空间
     * @param keyword 搜索关键字
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 搜索结果
     */
    @GET("cgi-bin/wedoc/doc_search")
    Single<WeWorkResult<List<WeWorkDocInfo>>> searchDocuments(
            @Query("access_token") String accessToken,
            @Query("spaceid") String namespace,
            @Query("keyword") String keyword,
            @Query("offset") Integer offset,
            @Query("limit") Integer limit
    );
}
