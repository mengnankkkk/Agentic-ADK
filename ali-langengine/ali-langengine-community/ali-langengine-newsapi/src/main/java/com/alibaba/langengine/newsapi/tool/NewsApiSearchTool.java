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
 * NewsAPI Search Everything Tool
 * Search all articles from NewsAPI
 * 
 * @author LangEngine Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class NewsApiSearchTool extends DefaultTool {
    
    private NewsApiClient newsApiClient;
    
    public NewsApiSearchTool() {
        this.newsApiClient = new NewsApiClient();
        init();
    }
    
    public NewsApiSearchTool(String apiKey) {
        this.newsApiClient = new NewsApiClient(apiKey);
        init();
    }
    
    public NewsApiSearchTool(NewsApiClient newsApiClient) {
        this.newsApiClient = newsApiClient;
        init();
    }
    
    private void init() {
        setName("NewsApiSearchTool");
        setDescription("Search all news articles from NewsAPI by keywords. You can filter by date range and sort order. " +
                "Input parameters: query (required, search keywords), from (start date in yyyy-MM-dd format), " +
                "to (end date in yyyy-MM-dd format), sortBy (relevancy, popularity, or publishedAt), " +
                "pageSize (number of results, max 100), page (page number)");
        setParameters("{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"query\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"Keywords or phrases to search for. Supports AND/OR/NOT operators and phrase search with quotes\"\n" +
                "    },\n" +
                "    \"from\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"Start date for articles in yyyy-MM-dd format (e.g., '2024-01-01')\"\n" +
                "    },\n" +
                "    \"to\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"End date for articles in yyyy-MM-dd format (e.g., '2024-12-31')\"\n" +
                "    },\n" +
                "    \"sortBy\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"Sort order: 'relevancy' (default), 'popularity', or 'publishedAt'\",\n" +
                "      \"enum\": [\"relevancy\", \"popularity\", \"publishedAt\"],\n" +
                "      \"default\": \"relevancy\"\n" +
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
                "  },\n" +
                "  \"required\": [\"query\"]\n" +
                "}");
    }
    
    @Override
    public ToolExecuteResult run(String toolInput) {
        log.info("NewsAPI search tool input: {}", toolInput);
        
        try {
            Map<String, Object> inputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {});
            
            String query = (String) inputMap.get("query");
            String from = (String) inputMap.get("from");
            String to = (String) inputMap.get("to");
            String sortBy = (String) inputMap.get("sortBy");
            Integer pageSize = (Integer) inputMap.getOrDefault("pageSize", 20);
            Integer page = (Integer) inputMap.getOrDefault("page", 1);
            
            // Validate query
            if (StringUtils.isBlank(query)) {
                return new ToolExecuteResult("Error: Query parameter is required");
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
            
            // Validate sortBy
            if (StringUtils.isNotBlank(sortBy)) {
                if (!sortBy.equals("relevancy") && !sortBy.equals("popularity") && !sortBy.equals("publishedAt")) {
                    sortBy = "relevancy";
                }
            }
            
            NewsApiResponse response = newsApiClient.searchEverything(query, from, to, sortBy, pageSize, page);
            
            if (response.getArticles() == null || response.getArticles().isEmpty()) {
                return new ToolExecuteResult("No news articles found for query: " + query);
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Found %d articles for query '%s' (showing %d):\n\n", 
                    response.getTotalResults(), query, response.getArticles().size()));
            
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
            log.error("Error executing NewsAPI search tool", e);
            return new ToolExecuteResult("Error: " + e.getMessage());
        }
    }
}
