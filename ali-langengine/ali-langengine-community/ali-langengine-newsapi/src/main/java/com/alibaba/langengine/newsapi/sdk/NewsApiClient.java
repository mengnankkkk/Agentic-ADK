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

import com.alibaba.fastjson.JSON;
import com.alibaba.langengine.newsapi.model.NewsApiResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * NewsAPI Client
 * 
 * @author LangEngine Team
 */
@Slf4j
public class NewsApiClient {
    
    private final String apiKey;
    private final OkHttpClient httpClient;
    
    public NewsApiClient() {
        this(System.getenv(NewsApiConstant.ENV_NEWS_API_KEY));
    }
    
    public NewsApiClient(String apiKey) {
        if (StringUtils.isBlank(apiKey)) {
            throw new NewsApiException("NewsAPI key is required. Set NEWS_API_KEY environment variable or pass it to constructor.");
        }
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Get top headlines
     * 
     * @param country Country code (e.g., "us", "cn", "jp")
     * @param category Category (business, entertainment, general, health, science, sports, technology)
     * @param query Search query
     * @param pageSize Number of results per page
     * @param page Page number
     * @return NewsAPI response
     */
    public NewsApiResponse getTopHeadlines(String country, String category, String query, Integer pageSize, Integer page) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(NewsApiConstant.NEWS_API_BASE_URL + NewsApiConstant.ENDPOINT_TOP_HEADLINES)
                .newBuilder();
        
        if (StringUtils.isNotBlank(country)) {
            urlBuilder.addQueryParameter("country", country);
        }
        
        if (StringUtils.isNotBlank(category)) {
            urlBuilder.addQueryParameter("category", category);
        }
        
        if (StringUtils.isNotBlank(query)) {
            urlBuilder.addQueryParameter("q", query);
        }
        
        if (pageSize != null && pageSize > 0) {
            urlBuilder.addQueryParameter("pageSize", String.valueOf(Math.min(pageSize, NewsApiConstant.MAX_PAGE_SIZE)));
        }
        
        if (page != null && page > 0) {
            urlBuilder.addQueryParameter("page", String.valueOf(page));
        }
        
        return executeRequest(urlBuilder.build().toString());
    }
    
    /**
     * Search everything
     * 
     * @param query Search query (required)
     * @param from Starting date (format: yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss)
     * @param to Ending date (format: yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss)
     * @param sortBy Sort order (relevancy, popularity, publishedAt)
     * @param pageSize Number of results per page
     * @param page Page number
     * @return NewsAPI response
     */
    public NewsApiResponse searchEverything(String query, String from, String to, String sortBy, Integer pageSize, Integer page) {
        if (StringUtils.isBlank(query)) {
            throw new NewsApiException("Query parameter is required for everything endpoint");
        }
        
        HttpUrl.Builder urlBuilder = HttpUrl.parse(NewsApiConstant.NEWS_API_BASE_URL + NewsApiConstant.ENDPOINT_EVERYTHING)
                .newBuilder()
                .addQueryParameter("q", query);
        
        if (StringUtils.isNotBlank(from)) {
            urlBuilder.addQueryParameter("from", from);
        }
        
        if (StringUtils.isNotBlank(to)) {
            urlBuilder.addQueryParameter("to", to);
        }
        
        if (StringUtils.isNotBlank(sortBy)) {
            urlBuilder.addQueryParameter("sortBy", sortBy);
        }
        
        if (pageSize != null && pageSize > 0) {
            urlBuilder.addQueryParameter("pageSize", String.valueOf(Math.min(pageSize, NewsApiConstant.MAX_PAGE_SIZE)));
        }
        
        if (page != null && page > 0) {
            urlBuilder.addQueryParameter("page", String.valueOf(page));
        }
        
        return executeRequest(urlBuilder.build().toString());
    }
    
    /**
     * Execute HTTP request
     * 
     * @param url Request URL
     * @return NewsAPI response
     */
    private NewsApiResponse executeRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Api-Key", apiKey)
                .addHeader("User-Agent", "Ali-LangEngine-NewsAPI/1.0")
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                log.error("NewsAPI request failed. Status: {}, Body: {}", response.code(), responseBody);
                throw new NewsApiException("NewsAPI request failed with status: " + response.code());
            }
            
            NewsApiResponse newsApiResponse = JSON.parseObject(responseBody, NewsApiResponse.class);
            
            if (!"ok".equalsIgnoreCase(newsApiResponse.getStatus())) {
                String errorMsg = String.format("NewsAPI returned error. Code: %s, Message: %s", 
                        newsApiResponse.getCode(), newsApiResponse.getMessage());
                log.error(errorMsg);
                throw new NewsApiException(errorMsg);
            }
            
            return newsApiResponse;
            
        } catch (IOException e) {
            log.error("Failed to execute NewsAPI request", e);
            throw new NewsApiException("Failed to execute NewsAPI request", e);
        }
    }
}
