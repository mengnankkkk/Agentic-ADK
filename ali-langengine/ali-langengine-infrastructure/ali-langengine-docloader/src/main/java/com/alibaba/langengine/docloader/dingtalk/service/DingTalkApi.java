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
package com.alibaba.langengine.docloader.dingtalk.service;

import io.reactivex.Single;
import retrofit2.http.*;

/**
 * 钉钉API
 *
 * @author Libres-coder
 */
public interface DingTalkApi {

    @GET("/gettoken")
    Single<DingTalkResult<DingTalkAccessToken>> getAccessToken(@Query("appkey") String appKey,
                                                                 @Query("appsecret") String appSecret);

    @POST("/topapi/wiki/workspace/listbyuser")
    Single<DingTalkResult<DingTalkWorkspaceList>> getWorkspaceList(@Query("access_token") String accessToken,
                                                                     @Body DingTalkUserRequest body);

    @POST("/topapi/wiki/doc/list")
    Single<DingTalkResult<DingTalkDocList>> getDocList(@Query("access_token") String accessToken,
                                                         @Body DingTalkDocListRequest body);

    @POST("/topapi/wiki/doc/get")
    Single<DingTalkResult<DingTalkDocContent>> getDocContent(@Query("access_token") String accessToken,
                                                               @Body DingTalkDocRequest body);
}
