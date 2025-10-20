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

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Greenhouse Get Job Tool
 * Get detailed information about a specific job
 * 
 * @author LangEngine Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class GreenhouseGetJobTool extends DefaultTool {
    
    private GreenhouseClient greenhouseClient;
    
    public GreenhouseGetJobTool() {
        this.greenhouseClient = new GreenhouseClient();
        init();
    }
    
    public GreenhouseGetJobTool(String apiKey) {
        this.greenhouseClient = new GreenhouseClient(apiKey);
        init();
    }
    
    public GreenhouseGetJobTool(GreenhouseClient greenhouseClient) {
        this.greenhouseClient = greenhouseClient;
        init();
    }
    
    private void init() {
        setName("GreenhouseGetJobTool");
        setDescription("Get detailed information about a specific job opening from Greenhouse. " +
                "Input parameters: jobId (required, the ID of the job to retrieve)");
        setParameters("{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"jobId\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"description\": \"The ID of the job to retrieve\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\"jobId\"]\n" +
                "}");
    }
    
    @Override
    public ToolExecuteResult run(String toolInput) {
        log.info("Greenhouse get job tool input: {}", toolInput);
        
        try {
            Map<String, Object> inputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {});
            
            Object jobIdObj = inputMap.get("jobId");
            if (jobIdObj == null) {
                return new ToolExecuteResult("Error: jobId parameter is required");
            }
            
            Long jobId = null;
            if (jobIdObj instanceof Integer) {
                jobId = ((Integer) jobIdObj).longValue();
            } else if (jobIdObj instanceof Long) {
                jobId = (Long) jobIdObj;
            } else {
                try {
                    jobId = Long.parseLong(jobIdObj.toString());
                } catch (NumberFormatException e) {
                    return new ToolExecuteResult("Error: jobId must be a valid number");
                }
            }
            
            if (jobId <= 0) {
                return new ToolExecuteResult("Error: jobId must be a positive number");
            }
            
            GreenhouseJob job = greenhouseClient.getJob(jobId);
            
            if (job == null) {
                return new ToolExecuteResult("Job not found with ID: " + jobId);
            }
            
            StringBuilder result = new StringBuilder();
            result.append("=== Job Details ===\n\n");
            result.append(String.format("Job ID: %d\n", job.getId()));
            result.append(String.format("Name: %s\n", job.getName()));
            result.append(String.format("Status: %s\n", job.getStatus()));
            result.append(String.format("Confidential: %s\n", job.getConfidential() != null ? job.getConfidential() : "No"));
            
            if (StringUtils.isNotBlank(job.getRequisitionId())) {
                result.append(String.format("Requisition ID: %s\n", job.getRequisitionId()));
            }
            
            if (job.getDepartments() != null && !job.getDepartments().isEmpty()) {
                result.append("\nDepartments:\n");
                for (GreenhouseDepartment dept : job.getDepartments()) {
                    result.append(String.format("  - %s (ID: %d)\n", dept.getName(), dept.getId()));
                }
            }
            
            if (job.getOffices() != null && !job.getOffices().isEmpty()) {
                result.append("\nOffices:\n");
                for (GreenhouseOffice office : job.getOffices()) {
                    result.append(String.format("  - %s", office.getName()));
                    if (StringUtils.isNotBlank(office.getLocation())) {
                        result.append(String.format(" (%s)", office.getLocation()));
                    }
                    result.append(String.format(" [ID: %d]\n", office.getId()));
                }
            }
            
            if (job.getHiringTeam() != null) {
                result.append("\n=== Hiring Team ===\n");
                
                if (job.getHiringTeam().getHiringManagers() != null && !job.getHiringTeam().getHiringManagers().isEmpty()) {
                    result.append("\nHiring Managers:\n");
                    for (GreenhouseUser user : job.getHiringTeam().getHiringManagers()) {
                        result.append(String.format("  - %s (ID: %d)\n", user.getName(), user.getId()));
                    }
                }
                
                if (job.getHiringTeam().getRecruiters() != null && !job.getHiringTeam().getRecruiters().isEmpty()) {
                    result.append("\nRecruiters:\n");
                    for (GreenhouseUser user : job.getHiringTeam().getRecruiters()) {
                        result.append(String.format("  - %s (ID: %d)\n", user.getName(), user.getId()));
                    }
                }
                
                if (job.getHiringTeam().getCoordinators() != null && !job.getHiringTeam().getCoordinators().isEmpty()) {
                    result.append("\nCoordinators:\n");
                    for (GreenhouseUser user : job.getHiringTeam().getCoordinators()) {
                        result.append(String.format("  - %s (ID: %d)\n", user.getName(), user.getId()));
                    }
                }
            }
            
            if (job.getOpenings() != null && !job.getOpenings().isEmpty()) {
                result.append("\n=== Job Openings ===\n");
                long openCount = job.getOpenings().stream()
                        .filter(o -> "open".equalsIgnoreCase(o.getStatus()))
                        .count();
                long closedCount = job.getOpenings().stream()
                        .filter(o -> "closed".equalsIgnoreCase(o.getStatus()))
                        .count();
                result.append(String.format("Total Openings: %d (Open: %d, Closed: %d)\n", 
                        job.getOpenings().size(), openCount, closedCount));
            }
            
            if (StringUtils.isNotBlank(job.getNotes())) {
                result.append("\n=== Notes ===\n");
                result.append(job.getNotes()).append("\n");
            }
            
            result.append("\n=== Timestamps ===\n");
            if (StringUtils.isNotBlank(job.getCreatedAt())) {
                result.append(String.format("Created: %s\n", job.getCreatedAt()));
            }
            if (StringUtils.isNotBlank(job.getUpdatedAt())) {
                result.append(String.format("Updated: %s\n", job.getUpdatedAt()));
            }
            
            return new ToolExecuteResult(result.toString());
            
        } catch (Exception e) {
            log.error("Error executing Greenhouse get job tool", e);
            return new ToolExecuteResult("Error: " + e.getMessage());
        }
    }
}
