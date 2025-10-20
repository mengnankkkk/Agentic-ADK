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

/**
 * Greenhouse API Constants
 * 
 * @author LangEngine Team
 */
public class GreenhouseConstant {
    
    /**
     * Greenhouse Harvest API base URL
     */
    public static final String GREENHOUSE_API_BASE_URL = "https://harvest.greenhouse.io/v1/";
    
    /**
     * Jobs endpoint
     */
    public static final String ENDPOINT_JOBS = "jobs";
    
    /**
     * Job endpoint (single job)
     */
    public static final String ENDPOINT_JOB = "jobs/{id}";
    
    /**
     * Candidates endpoint
     */
    public static final String ENDPOINT_CANDIDATES = "candidates";
    
    /**
     * Candidate endpoint (single candidate)
     */
    public static final String ENDPOINT_CANDIDATE = "candidates/{id}";
    
    /**
     * Applications endpoint
     */
    public static final String ENDPOINT_APPLICATIONS = "applications";
    
    /**
     * Departments endpoint
     */
    public static final String ENDPOINT_DEPARTMENTS = "departments";
    
    /**
     * Offices endpoint
     */
    public static final String ENDPOINT_OFFICES = "offices";
    
    /**
     * Users endpoint
     */
    public static final String ENDPOINT_USERS = "users";
    
    /**
     * Default page size
     */
    public static final int DEFAULT_PAGE_SIZE = 50;
    
    /**
     * Max page size
     */
    public static final int MAX_PAGE_SIZE = 500;
    
    /**
     * Environment variable for API key
     */
    public static final String ENV_GREENHOUSE_API_KEY = "GREENHOUSE_API_KEY";
}
