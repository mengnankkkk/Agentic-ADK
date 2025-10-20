package com.alibaba.langengine.amap;

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

import com.alibaba.langengine.core.util.WorkPropertiesUtils;

import static com.alibaba.langengine.amap.sdk.AMapConstant.AMAP_BASE_URL;

public class AMapConfiguration {

	/**
	 * AMap API key, retrieved from work properties
	 */
	public static String AMAP_API_KEY = WorkPropertiesUtils.get("amap_api_key");

	/**
	 * AMap API base URL, defaults to the constant BASE_URL if not configured
	 */
	public static String AMAP_API_URL = WorkPropertiesUtils.get("amap_api_url", AMAP_BASE_URL);

}
