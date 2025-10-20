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
package com.alibaba.langengine.dashvector;

import com.alibaba.langengine.core.util.WorkPropertiesUtils;


public class DashVectorConfiguration {

    /**
     * DashVector API key
     */
    public static String DASHVECTOR_API_KEY = WorkPropertiesUtils.get("dashvector_api_key");

    /**
     * DashVector endpoint
     */
    public static String DASHVECTOR_ENDPOINT = WorkPropertiesUtils.get("dashvector_endpoint");
    
    public static String getApiKey() {
        String value = DASHVECTOR_API_KEY;
        if (value == null || value.trim().isEmpty()) {
            return "test_api_key"; // 默认返回测试值
        }
        return value.trim();
    }
    
    public static String getEndpoint() {
        String value = DASHVECTOR_ENDPOINT;
        if (value == null || value.trim().isEmpty()) {
            return "test_endpoint"; // 默认返回测试值
        }
        return value.trim();
    }

}