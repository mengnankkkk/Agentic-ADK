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
package com.alibaba.langengine.newsapi.sdk;

/**
 * NewsAPI Constants
 * 
 * @author LangEngine Team
 */
public class NewsApiConstant {
    
    /**
     * NewsAPI base URL
     */
    public static final String NEWS_API_BASE_URL = "https://newsapi.org/v2/";
    
    /**
     * Top headlines endpoint
     */
    public static final String ENDPOINT_TOP_HEADLINES = "top-headlines";
    
    /**
     * Everything endpoint
     */
    public static final String ENDPOINT_EVERYTHING = "everything";
    
    /**
     * Sources endpoint
     */
    public static final String ENDPOINT_SOURCES = "sources";
    
    /**
     * Default page size
     */
    public static final int DEFAULT_PAGE_SIZE = 20;
    
    /**
     * Max page size
     */
    public static final int MAX_PAGE_SIZE = 100;
    
    /**
     * Environment variable for API key
     */
    public static final String ENV_NEWS_API_KEY = "NEWS_API_KEY";
}
