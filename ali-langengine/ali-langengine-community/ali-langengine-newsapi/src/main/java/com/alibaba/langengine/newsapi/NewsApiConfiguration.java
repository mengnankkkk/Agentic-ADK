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
package com.alibaba.langengine.newsapi;

import lombok.Data;

/**
 * NewsAPI Configuration
 * 
 * @author LangEngine Team
 */
@Data
public class NewsApiConfiguration {
    
    /**
     * NewsAPI key
     */
    private String apiKey;
    
    /**
     * Default page size
     */
    private int defaultPageSize = 20;
    
    /**
     * Max page size
     */
    private int maxPageSize = 100;
    
    /**
     * Connection timeout in seconds
     */
    private int connectTimeout = 30;
    
    /**
     * Read timeout in seconds
     */
    private int readTimeout = 30;
    
    public NewsApiConfiguration() {
        this.apiKey = System.getenv("NEWS_API_KEY");
    }
    
    public NewsApiConfiguration(String apiKey) {
        this.apiKey = apiKey;
    }
}
