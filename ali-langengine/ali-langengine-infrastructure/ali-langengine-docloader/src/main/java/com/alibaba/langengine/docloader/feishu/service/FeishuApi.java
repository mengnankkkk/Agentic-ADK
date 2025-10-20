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
package com.alibaba.langengine.docloader.feishu.service;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Path;
import io.reactivex.Single;

/**
 * 飞书API
 *
 * @author Libres-coder
 */
public interface FeishuApi {

    @POST("/open-apis/auth/v3/tenant_access_token/internal")
    Single<FeishuResult<FeishuAccessToken>> getTenantAccessToken(@Body FeishuAuthRequest request);

    @GET("/open-apis/docx/v1/documents/{doc_token}/raw_content")
    Single<FeishuResult<FeishuDocContent>> getDocxRawContent(@Path("doc_token") String docToken);

    @GET("/open-apis/docx/v1/documents/{doc_token}")
    Single<FeishuResult<FeishuDocMeta>> getDocMeta(@Path("doc_token") String docToken);

    @GET("/open-apis/doc/v2/content/{doc_token}")
    Single<FeishuResult<FeishuDocBlocks>> getDocBlocks(@Path("doc_token") String docToken);

    @GET("/open-apis/wiki/v2/spaces")
    Single<FeishuResult<FeishuSpaceList>> getSpaces(@Query("page_token") String pageToken,
                                                     @Query("page_size") Integer pageSize);

    @GET("/open-apis/wiki/v2/spaces/{space_id}/nodes")
    Single<FeishuResult<FeishuNodeList>> getSpaceNodes(@Path("space_id") String spaceId,
                                                       @Query("page_token") String pageToken,
                                                       @Query("page_size") Integer pageSize);
}
