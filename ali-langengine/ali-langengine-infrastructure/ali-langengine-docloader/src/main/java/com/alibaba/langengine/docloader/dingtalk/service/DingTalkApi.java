package com.alibaba.langengine.docloader.dingtalk.service;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;


public interface DingTalkApi {

    /**
     * 获取文档列表
     *
     * @param accessToken 访问令牌
     * @param namespace   命名空间
     * @param offset      偏移量
     * @param limit       限制数量
     * @return 文档列表结果
     */
    @GET("v1.0/wiki/{namespace}/docs")
    Single<DingTalkResult<List<DingTalkDocInfo>>> getDocumentList(
            @Query("access_token") String accessToken,
            @Path("namespace") String namespace,
            @Query("offset") Integer offset,
            @Query("limit") Integer limit
    );

    /**
     * 获取文档详情
     *
     * @param accessToken 访问令牌
     * @param namespace   命名空间
     * @param documentId  文档ID
     * @return 文档详情结果
     */
    @GET("v1.0/wiki/{namespace}/docs/{documentId}")
    Single<DingTalkResult<DingTalkDocInfo>> getDocumentDetail(
            @Query("access_token") String accessToken,
            @Path("namespace") String namespace,
            @Path("documentId") String documentId
    );
}