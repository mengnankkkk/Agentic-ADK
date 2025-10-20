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
package com.alibaba.langengine.turbopuffer;

import com.alibaba.langengine.core.util.WorkPropertiesUtils;


public class TurbopufferConfiguration {

    /**
     * Turbopuffer API key
     */
    public static String TURBOPUFFER_API_KEY = WorkPropertiesUtils.get("turbopuffer_api_key");

    /**
     * Turbopuffer server URL
     */
    public static String TURBOPUFFER_SERVER_URL = WorkPropertiesUtils.get("turbopuffer_server_url", "https://api.turbopuffer.com");
}
