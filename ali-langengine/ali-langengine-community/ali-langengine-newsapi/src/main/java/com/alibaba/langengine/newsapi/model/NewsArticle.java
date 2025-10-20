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

/**
 * News Article
 * 
 * @author LangEngine Team
 */
@Data
public class NewsArticle {
    
    /**
     * Source of the article
     */
    @JSONField(name = "source")
    private NewsSource source;
    
    /**
     * Author of the article
     */
    @JSONField(name = "author")
    private String author;
    
    /**
     * Title of the article
     */
    @JSONField(name = "title")
    private String title;
    
    /**
     * Description of the article
     */
    @JSONField(name = "description")
    private String description;
    
    /**
     * URL of the article
     */
    @JSONField(name = "url")
    private String url;
    
    /**
     * URL to the article image
     */
    @JSONField(name = "urlToImage")
    private String urlToImage;
    
    /**
     * Published date (ISO 8601 format)
     */
    @JSONField(name = "publishedAt")
    private String publishedAt;
    
    /**
     * Content of the article
     */
    @JSONField(name = "content")
    private String content;
}
