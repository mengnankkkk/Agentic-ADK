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
package com.alibaba.langengine.docloader.yuque.service;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

/**
 * 语雀API
 *
 * @author xiaoxuan.lp
 */
public interface YuqueApi {

    @GET("/api/v2/repos/{namespace}/docs")
    Single<YuqueResult<List<YuqueDocInfo>>> getDocs(@Path(value = "namespace", encoded = true) String namespace,
                                                    @Query("offset") Integer offset,
                                                    @Query("limit") Integer limit,
                                                    @Query("optional_properties") String optionalProperties);

    @GET("/api/v2/repos/{namespace}/docs/{slug}")
    Single<YuqueResult<YuqueDocInfo>> getDocDetail(@Path(value = "namespace", encoded = true) String namespace,
                                                   @Path("slug") String slug);
}
