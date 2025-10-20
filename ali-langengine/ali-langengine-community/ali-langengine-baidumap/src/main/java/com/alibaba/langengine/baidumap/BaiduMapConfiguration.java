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

package com.alibaba.langengine.baidumap;

import com.alibaba.langengine.core.util.WorkPropertiesUtils;

import static com.alibaba.langengine.baidumap.sdk.BaiduMapConstant.BAIDU_MAP_BASE_URL;

public class BaiduMapConfiguration {

	/**
	 * Baidu Map API key, retrieved from work properties
	 */
	public static String BAIDU_MAP_API_KEY = WorkPropertiesUtils.get("baidu_map_api_key");

	/**
	 * Baidu Map API base URL, defaults to the constant BASE_URL if not configured
	 */
	public static String BAIDU_MAP_API_URL = WorkPropertiesUtils.get("baidu_map_api_url", BAIDU_MAP_BASE_URL);

}
