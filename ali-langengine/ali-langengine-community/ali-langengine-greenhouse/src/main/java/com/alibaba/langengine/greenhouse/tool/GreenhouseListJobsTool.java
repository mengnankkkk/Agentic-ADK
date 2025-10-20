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
 * Greenhouse List Jobs Tool
 * List all job openings from Greenhouse
 * 
 * @author LangEngine Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class GreenhouseListJobsTool extends DefaultTool {
    
    private GreenhouseClient greenhouseClient;
    
    public GreenhouseListJobsTool() {
        this.greenhouseClient = new GreenhouseClient();
        init();
    }
    
    public GreenhouseListJobsTool(String apiKey) {
        this.greenhouseClient = new GreenhouseClient(apiKey);
        init();
    }
    
    public GreenhouseListJobsTool(GreenhouseClient greenhouseClient) {
        this.greenhouseClient = greenhouseClient;
        init();
    }
    
    private void init() {
        setName("GreenhouseListJobsTool");
        setDescription("List all job openings from Greenhouse recruiting platform. " +
                "Input parameters: page (page number, default 1), perPage (results per page, max 500, default 50)");
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
                "    }\n" +
                "  }\n" +
                "}");
    }
    
    @Override
    public ToolExecuteResult run(String toolInput) {
        log.info("Greenhouse list jobs tool input: {}", toolInput);
        
        try {
            Map<String, Object> inputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {});
            
            Integer page = (Integer) inputMap.getOrDefault("page", 1);
            Integer perPage = (Integer) inputMap.getOrDefault("perPage", 50);
            
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
            
            List<GreenhouseJob> jobs = greenhouseClient.listJobs(page, perPage);
            
            if (jobs == null || jobs.isEmpty()) {
                return new ToolExecuteResult("No jobs found in Greenhouse");
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Found %d job openings:\n\n", jobs.size()));
            
            for (GreenhouseJob job : jobs) {
                result.append(String.format("Job ID: %d\n", job.getId()));
                result.append(String.format("  Name: %s\n", job.getName()));
                result.append(String.format("  Status: %s\n", job.getStatus()));
                
                if (job.getDepartments() != null && !job.getDepartments().isEmpty()) {
                    String departments = job.getDepartments().stream()
                            .map(GreenhouseDepartment::getName)
                            .collect(Collectors.joining(", "));
                    result.append(String.format("  Departments: %s\n", departments));
                }
                
                if (job.getOffices() != null && !job.getOffices().isEmpty()) {
                    String offices = job.getOffices().stream()
                            .map(office -> office.getName() + 
                                    (StringUtils.isNotBlank(office.getLocation()) ? " (" + office.getLocation() + ")" : ""))
                            .collect(Collectors.joining(", "));
                    result.append(String.format("  Offices: %s\n", offices));
                }
                
                if (job.getOpenings() != null && !job.getOpenings().isEmpty()) {
                    long openCount = job.getOpenings().stream()
                            .filter(o -> "open".equalsIgnoreCase(o.getStatus()))
                            .count();
                    result.append(String.format("  Open Positions: %d\n", openCount));
                }
                
                if (StringUtils.isNotBlank(job.getCreatedAt())) {
                    result.append(String.format("  Created: %s\n", job.getCreatedAt()));
                }
                
                result.append("\n");
            }
            
            return new ToolExecuteResult(result.toString());
            
        } catch (Exception e) {
            log.error("Error executing Greenhouse list jobs tool", e);
            return new ToolExecuteResult("Error: " + e.getMessage());
        }
    }
}
