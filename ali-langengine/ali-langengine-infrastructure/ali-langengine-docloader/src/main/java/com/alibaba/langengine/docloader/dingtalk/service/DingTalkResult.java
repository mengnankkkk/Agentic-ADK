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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 钉钉响应结果
 *
 * @author Libres-coder
 */
@Data
public class DingTalkResult<T> {

    /**
     * 返回码，0表示成功
     */
    @JsonProperty("errcode")
    private Integer errCode;

    /**
     * 错误信息
     */
    @JsonProperty("errmsg")
    private String errMsg;

    /**
     * 响应数据
     */
    private T result;

    /**
     * access_token（仅获取token接口返回）
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 过期时间（仅获取token接口返回）
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;
}
