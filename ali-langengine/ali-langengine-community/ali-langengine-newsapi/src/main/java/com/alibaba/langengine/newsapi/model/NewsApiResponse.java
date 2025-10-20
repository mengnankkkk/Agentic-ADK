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
package com.alibaba.langengine.newsapi.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * NewsAPI Response
 * 
 * @author LangEngine Team
 */
@Data
public class NewsApiResponse {
    
    /**
     * Status of the request
     */
    @JSONField(name = "status")
    private String status;
    
    /**
     * Total number of results
     */
    @JSONField(name = "totalResults")
    private Integer totalResults;
    
    /**
     * List of articles
     */
    @JSONField(name = "articles")
    private List<NewsArticle> articles;
    
    /**
     * Error code (if any)
     */
    @JSONField(name = "code")
    private String code;
    
    /**
     * Error message (if any)
     */
    @JSONField(name = "message")
    private String message;
}
