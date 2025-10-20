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
package com.alibaba.langengine.newsapi.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.langengine.core.tool.DefaultTool;
import com.alibaba.langengine.core.tool.ToolExecuteResult;
import com.alibaba.langengine.newsapi.model.NewsApiResponse;
import com.alibaba.langengine.newsapi.model.NewsArticle;
import com.alibaba.langengine.newsapi.sdk.NewsApiClient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * NewsAPI Top Headlines Tool
 * Get top headlines from NewsAPI
 * 
 * @author LangEngine Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class NewsApiTopHeadlinesTool extends DefaultTool {
    
    private NewsApiClient newsApiClient;
    
    public NewsApiTopHeadlinesTool() {
        this.newsApiClient = new NewsApiClient();
        init();
    }
    
    public NewsApiTopHeadlinesTool(String apiKey) {
        this.newsApiClient = new NewsApiClient(apiKey);
        init();
    }
    
    public NewsApiTopHeadlinesTool(NewsApiClient newsApiClient) {
        this.newsApiClient = newsApiClient;
        init();
    }
    
    private void init() {
        setName("NewsApiTopHeadlinesTool");
        setDescription("Get top headlines from NewsAPI. You can filter by country, category, and search query. " +
                "Input parameters: country (2-letter ISO code, e.g., 'us', 'cn', 'jp'), " +
                "category (business, entertainment, general, health, science, sports, technology), " +
                "query (search keywords), pageSize (number of results, max 100), page (page number)");
        setParameters("{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"country\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"2-letter ISO 3166-1 country code (e.g., 'us', 'cn', 'jp')\"\n" +
                "    },\n" +
                "    \"category\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"Category of news (business, entertainment, general, health, science, sports, technology)\"\n" +
                "    },\n" +
                "    \"query\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"Keywords or phrases to search for in the article title and body\"\n" +
                "    },\n" +
                "    \"pageSize\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"description\": \"The number of results to return per page, default 20, max 100\",\n" +
                "      \"default\": 20\n" +
                "    },\n" +
                "    \"page\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"description\": \"Page number for pagination, default 1\",\n" +
                "      \"default\": 1\n" +
                "    }\n" +
                "  }\n" +
                "}");
    }
    
    @Override
    public ToolExecuteResult run(String toolInput) {
        log.info("NewsAPI top headlines tool input: {}", toolInput);
        
        try {
            Map<String, Object> inputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {});
            
            String country = (String) inputMap.get("country");
            String category = (String) inputMap.get("category");
            String query = (String) inputMap.get("query");
            Integer pageSize = (Integer) inputMap.getOrDefault("pageSize", 20);
            Integer page = (Integer) inputMap.getOrDefault("page", 1);
            
            // Validate that at least one filter is provided
            if (StringUtils.isBlank(country) && StringUtils.isBlank(category) && StringUtils.isBlank(query)) {
                return new ToolExecuteResult("Error: At least one of country, category, or query must be provided");
            }
            
            // Validate page size
            if (pageSize != null && pageSize > 100) {
                pageSize = 100;
            }
            if (pageSize != null && pageSize <= 0) {
                pageSize = 20;
            }
            
            // Validate page number
            if (page != null && page <= 0) {
                page = 1;
            }
            
            NewsApiResponse response = newsApiClient.getTopHeadlines(country, category, query, pageSize, page);
            
            if (response.getArticles() == null || response.getArticles().isEmpty()) {
                return new ToolExecuteResult("No news articles found matching your criteria");
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Found %d top headlines (showing %d):\n\n", 
                    response.getTotalResults(), response.getArticles().size()));
            
            int index = 1;
            for (NewsArticle article : response.getArticles()) {
                result.append(String.format("[%d] %s\n", index++, article.getTitle()));
                if (StringUtils.isNotBlank(article.getDescription())) {
                    result.append(String.format("    Description: %s\n", article.getDescription()));
                }
                if (article.getSource() != null && StringUtils.isNotBlank(article.getSource().getName())) {
                    result.append(String.format("    Source: %s\n", article.getSource().getName()));
                }
                if (StringUtils.isNotBlank(article.getAuthor())) {
                    result.append(String.format("    Author: %s\n", article.getAuthor()));
                }
                if (StringUtils.isNotBlank(article.getPublishedAt())) {
                    result.append(String.format("    Published: %s\n", article.getPublishedAt()));
                }
                if (StringUtils.isNotBlank(article.getUrl())) {
                    result.append(String.format("    URL: %s\n", article.getUrl()));
                }
                result.append("\n");
            }
            
            return new ToolExecuteResult(result.toString());
            
        } catch (Exception e) {
            log.error("Error executing NewsAPI top headlines tool", e);
            return new ToolExecuteResult("Error: " + e.getMessage());
        }
    }
}
