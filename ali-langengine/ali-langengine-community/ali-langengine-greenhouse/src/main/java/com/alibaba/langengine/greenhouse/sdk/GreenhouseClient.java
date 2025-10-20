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
package com.alibaba.langengine.greenhouse.sdk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.langengine.greenhouse.model.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Greenhouse API Client
 * 
 * @author LangEngine Team
 */
@Slf4j
public class GreenhouseClient {
    
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final String authHeader;
    
    public GreenhouseClient() {
        this(System.getenv(GreenhouseConstant.ENV_GREENHOUSE_API_KEY));
    }
    
    public GreenhouseClient(String apiKey) {
        if (StringUtils.isBlank(apiKey)) {
            throw new GreenhouseException("Greenhouse API key is required. Set GREENHOUSE_API_KEY environment variable or pass it to constructor.");
        }
        this.apiKey = apiKey;
        // Greenhouse uses Basic Auth with API key as username and empty password
        String credentials = apiKey + ":";
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * List all jobs
     * 
     * @param page Page number
     * @param perPage Results per page
     * @return List of jobs
     */
    public List<GreenhouseJob> listJobs(Integer page, Integer perPage) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(GreenhouseConstant.GREENHOUSE_API_BASE_URL + GreenhouseConstant.ENDPOINT_JOBS)
                .newBuilder();
        
        if (page != null && page > 0) {
            urlBuilder.addQueryParameter("page", String.valueOf(page));
        }
        
        if (perPage != null && perPage > 0) {
            urlBuilder.addQueryParameter("per_page", String.valueOf(Math.min(perPage, GreenhouseConstant.MAX_PAGE_SIZE)));
        }
        
        String response = executeRequest(urlBuilder.build().toString());
        return JSONArray.parseArray(response, GreenhouseJob.class);
    }
    
    /**
     * Get a single job by ID
     * 
     * @param jobId Job ID
     * @return Job details
     */
    public GreenhouseJob getJob(Long jobId) {
        if (jobId == null || jobId <= 0) {
            throw new GreenhouseException("Valid job ID is required");
        }
        
        String url = GreenhouseConstant.GREENHOUSE_API_BASE_URL + 
                GreenhouseConstant.ENDPOINT_JOB.replace("{id}", String.valueOf(jobId));
        
        String response = executeRequest(url);
        return JSON.parseObject(response, GreenhouseJob.class);
    }
    
    /**
     * List all candidates
     * 
     * @param page Page number
     * @param perPage Results per page
     * @param createdBefore Filter by created before date (ISO 8601)
     * @param createdAfter Filter by created after date (ISO 8601)
     * @param updatedBefore Filter by updated before date (ISO 8601)
     * @param updatedAfter Filter by updated after date (ISO 8601)
     * @return List of candidates
     */
    public List<GreenhouseCandidate> listCandidates(Integer page, Integer perPage, 
            String createdBefore, String createdAfter, String updatedBefore, String updatedAfter) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(GreenhouseConstant.GREENHOUSE_API_BASE_URL + GreenhouseConstant.ENDPOINT_CANDIDATES)
                .newBuilder();
        
        if (page != null && page > 0) {
            urlBuilder.addQueryParameter("page", String.valueOf(page));
        }
        
        if (perPage != null && perPage > 0) {
            urlBuilder.addQueryParameter("per_page", String.valueOf(Math.min(perPage, GreenhouseConstant.MAX_PAGE_SIZE)));
        }
        
        if (StringUtils.isNotBlank(createdBefore)) {
            urlBuilder.addQueryParameter("created_before", createdBefore);
        }
        
        if (StringUtils.isNotBlank(createdAfter)) {
            urlBuilder.addQueryParameter("created_after", createdAfter);
        }
        
        if (StringUtils.isNotBlank(updatedBefore)) {
            urlBuilder.addQueryParameter("updated_before", updatedBefore);
        }
        
        if (StringUtils.isNotBlank(updatedAfter)) {
            urlBuilder.addQueryParameter("updated_after", updatedAfter);
        }
        
        String response = executeRequest(urlBuilder.build().toString());
        return JSONArray.parseArray(response, GreenhouseCandidate.class);
    }
    
    /**
     * Get a single candidate by ID
     * 
     * @param candidateId Candidate ID
     * @return Candidate details
     */
    public GreenhouseCandidate getCandidate(Long candidateId) {
        if (candidateId == null || candidateId <= 0) {
            throw new GreenhouseException("Valid candidate ID is required");
        }
        
        String url = GreenhouseConstant.GREENHOUSE_API_BASE_URL + 
                GreenhouseConstant.ENDPOINT_CANDIDATE.replace("{id}", String.valueOf(candidateId));
        
        String response = executeRequest(url);
        return JSON.parseObject(response, GreenhouseCandidate.class);
    }
    
    /**
     * List all departments
     * 
     * @param page Page number
     * @param perPage Results per page
     * @return List of departments
     */
    public List<GreenhouseDepartment> listDepartments(Integer page, Integer perPage) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(GreenhouseConstant.GREENHOUSE_API_BASE_URL + GreenhouseConstant.ENDPOINT_DEPARTMENTS)
                .newBuilder();
        
        if (page != null && page > 0) {
            urlBuilder.addQueryParameter("page", String.valueOf(page));
        }
        
        if (perPage != null && perPage > 0) {
            urlBuilder.addQueryParameter("per_page", String.valueOf(Math.min(perPage, GreenhouseConstant.MAX_PAGE_SIZE)));
        }
        
        String response = executeRequest(urlBuilder.build().toString());
        return JSONArray.parseArray(response, GreenhouseDepartment.class);
    }
    
    /**
     * List all offices
     * 
     * @param page Page number
     * @param perPage Results per page
     * @return List of offices
     */
    public List<GreenhouseOffice> listOffices(Integer page, Integer perPage) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(GreenhouseConstant.GREENHOUSE_API_BASE_URL + GreenhouseConstant.ENDPOINT_OFFICES)
                .newBuilder();
        
        if (page != null && page > 0) {
            urlBuilder.addQueryParameter("page", String.valueOf(page));
        }
        
        if (perPage != null && perPage > 0) {
            urlBuilder.addQueryParameter("per_page", String.valueOf(Math.min(perPage, GreenhouseConstant.MAX_PAGE_SIZE)));
        }
        
        String response = executeRequest(urlBuilder.build().toString());
        return JSONArray.parseArray(response, GreenhouseOffice.class);
    }
    
    /**
     * Execute HTTP GET request
     * 
     * @param url Request URL
     * @return Response body
     */
    private String executeRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .addHeader("User-Agent", "Ali-LangEngine-Greenhouse/1.0")
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                log.error("Greenhouse API request failed. Status: {}, Body: {}", response.code(), responseBody);
                throw new GreenhouseException("Greenhouse API request failed with status: " + response.code() + ", Body: " + responseBody);
            }
            
            return responseBody;
            
        } catch (IOException e) {
            log.error("Failed to execute Greenhouse API request", e);
            throw new GreenhouseException("Failed to execute Greenhouse API request", e);
        }
    }
}
