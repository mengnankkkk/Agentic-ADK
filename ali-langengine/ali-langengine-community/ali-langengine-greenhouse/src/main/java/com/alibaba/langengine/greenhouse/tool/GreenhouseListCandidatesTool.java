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
package com.alibaba.langengine.greenhouse.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.langengine.core.tool.DefaultTool;
import com.alibaba.langengine.core.tool.ToolExecuteResult;
import com.alibaba.langengine.greenhouse.model.*;
import com.alibaba.langengine.greenhouse.sdk.GreenhouseClient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Greenhouse List Candidates Tool
 * List candidates from Greenhouse
 * 
 * @author LangEngine Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class GreenhouseListCandidatesTool extends DefaultTool {
    
    private GreenhouseClient greenhouseClient;
    
    public GreenhouseListCandidatesTool() {
        this.greenhouseClient = new GreenhouseClient();
        init();
    }
    
    public GreenhouseListCandidatesTool(String apiKey) {
        this.greenhouseClient = new GreenhouseClient(apiKey);
        init();
    }
    
    public GreenhouseListCandidatesTool(GreenhouseClient greenhouseClient) {
        this.greenhouseClient = greenhouseClient;
        init();
    }
    
    private void init() {
        setName("GreenhouseListCandidatesTool");
        setDescription("List candidates from Greenhouse recruiting platform. " +
                "Input parameters: page (page number), perPage (results per page), " +
                "createdBefore (ISO 8601 date), createdAfter (ISO 8601 date), " +
                "updatedBefore (ISO 8601 date), updatedAfter (ISO 8601 date)");
        setParameters("{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"page\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"description\": \"Page number for pagination, default 1\",\n" +
                "      \"default\": 1\n" +
                "    },\n" +
                "    \"perPage\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"description\": \"Number of results per page, max 500, default 50\",\n" +
                "      \"default\": 50\n" +
                "    },\n" +
                "    \"createdBefore\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"Filter candidates created before this date (ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ)\"\n" +
                "    },\n" +
                "    \"createdAfter\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"Filter candidates created after this date (ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ)\"\n" +
                "    },\n" +
                "    \"updatedBefore\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"Filter candidates updated before this date (ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ)\"\n" +
                "    },\n" +
                "    \"updatedAfter\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"Filter candidates updated after this date (ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ)\"\n" +
                "    }\n" +
                "  }\n" +
                "}");
    }
    
    @Override
    public ToolExecuteResult run(String toolInput) {
        log.info("Greenhouse list candidates tool input: {}", toolInput);
        
        try {
            Map<String, Object> inputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {});
            
            Integer page = (Integer) inputMap.getOrDefault("page", 1);
            Integer perPage = (Integer) inputMap.getOrDefault("perPage", 50);
            String createdBefore = (String) inputMap.get("createdBefore");
            String createdAfter = (String) inputMap.get("createdAfter");
            String updatedBefore = (String) inputMap.get("updatedBefore");
            String updatedAfter = (String) inputMap.get("updatedAfter");
            
            // Validate parameters
            if (page != null && page <= 0) {
                page = 1;
            }
            
            if (perPage != null && perPage > 500) {
                perPage = 500;
            }
            if (perPage != null && perPage <= 0) {
                perPage = 50;
            }
            
            List<GreenhouseCandidate> candidates = greenhouseClient.listCandidates(
                    page, perPage, createdBefore, createdAfter, updatedBefore, updatedAfter);
            
            if (candidates == null || candidates.isEmpty()) {
                return new ToolExecuteResult("No candidates found matching the criteria");
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Found %d candidates:\n\n", candidates.size()));
            
            for (GreenhouseCandidate candidate : candidates) {
                result.append(String.format("Candidate ID: %d\n", candidate.getId()));
                result.append(String.format("  Name: %s %s\n", 
                        StringUtils.defaultString(candidate.getFirstName()), 
                        StringUtils.defaultString(candidate.getLastName())));
                
                if (StringUtils.isNotBlank(candidate.getTitle())) {
                    result.append(String.format("  Title: %s\n", candidate.getTitle()));
                }
                
                if (StringUtils.isNotBlank(candidate.getCompany())) {
                    result.append(String.format("  Company: %s\n", candidate.getCompany()));
                }
                
                if (candidate.getEmailAddresses() != null && !candidate.getEmailAddresses().isEmpty()) {
                    String emails = candidate.getEmailAddresses().stream()
                            .map(GreenhouseEmailAddress::getValue)
                            .collect(Collectors.joining(", "));
                    result.append(String.format("  Email: %s\n", emails));
                }
                
                if (candidate.getPhoneNumbers() != null && !candidate.getPhoneNumbers().isEmpty()) {
                    String phones = candidate.getPhoneNumbers().stream()
                            .map(GreenhousePhoneNumber::getValue)
                            .collect(Collectors.joining(", "));
                    result.append(String.format("  Phone: %s\n", phones));
                }
                
                if (candidate.getRecruiter() != null) {
                    result.append(String.format("  Recruiter: %s\n", candidate.getRecruiter().getName()));
                }
                
                if (candidate.getApplicationIds() != null && !candidate.getApplicationIds().isEmpty()) {
                    result.append(String.format("  Applications: %d\n", candidate.getApplicationIds().size()));
                }
                
                if (StringUtils.isNotBlank(candidate.getLastActivity())) {
                    result.append(String.format("  Last Activity: %s\n", candidate.getLastActivity()));
                }
                
                result.append("\n");
            }
            
            return new ToolExecuteResult(result.toString());
            
        } catch (Exception e) {
            log.error("Error executing Greenhouse list candidates tool", e);
            return new ToolExecuteResult("Error: " + e.getMessage());
        }
    }
}
