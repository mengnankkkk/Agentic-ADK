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
package com.alibaba.langengine.greenhouse;

import lombok.Data;

/**
 * Greenhouse Configuration
 * 
 * @author LangEngine Team
 */
@Data
public class GreenhouseConfiguration {
    
    /**
     * Greenhouse API key
     */
    private String apiKey;
    
    /**
     * Default page size
     */
    private int defaultPageSize = 50;
    
    /**
     * Max page size
     */
    private int maxPageSize = 500;
    
    /**
     * Connection timeout in seconds
     */
    private int connectTimeout = 30;
    
    /**
     * Read timeout in seconds
     */
    private int readTimeout = 30;
    
    public GreenhouseConfiguration() {
        this.apiKey = System.getenv("GREENHOUSE_API_KEY");
    }
    
    public GreenhouseConfiguration(String apiKey) {
        this.apiKey = apiKey;
    }
}
