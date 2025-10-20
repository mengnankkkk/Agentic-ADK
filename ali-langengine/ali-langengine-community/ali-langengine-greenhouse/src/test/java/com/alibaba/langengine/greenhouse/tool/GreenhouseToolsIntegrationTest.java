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

import com.alibaba.langengine.core.tool.ToolExecuteResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Greenhouse tools
 * 
 * Note: These tests require a valid GREENHOUSE_API_KEY environment variable.
 * Get your API key from your Greenhouse account settings.
 * 
 * Set the environment variable before running tests:
 * Windows: set GREENHOUSE_API_KEY=your_api_key_here
 * Linux/Mac: export GREENHOUSE_API_KEY=your_api_key_here
 * 
 * @author LangEngine Team
 */
@EnabledIfEnvironmentVariable(named = "GREENHOUSE_API_KEY", matches = ".+")
public class GreenhouseToolsIntegrationTest {
    
    @BeforeAll
    public static void setUp() {
        String apiKey = System.getenv("GREENHOUSE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("WARNING: GREENHOUSE_API_KEY environment variable is not set. Tests will be skipped.");
            System.out.println("Get your API key from your Greenhouse account settings.");
        }
    }
    
    @Test
    public void testListJobs_Basic() {
        GreenhouseListJobsTool tool = new GreenhouseListJobsTool();
        
        String input = "{\"page\": 1, \"perPage\": 10}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertFalse(result.getOutput().contains("Error"));
        
        System.out.println("=== List Jobs ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testListJobs_DefaultParameters() {
        GreenhouseListJobsTool tool = new GreenhouseListJobsTool();
        
        String input = "{}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        
        System.out.println("=== List Jobs (Default Parameters) ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testListJobs_SmallPageSize() {
        GreenhouseListJobsTool tool = new GreenhouseListJobsTool();
        
        String input = "{\"perPage\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        
        System.out.println("=== List Jobs (Small Page Size) ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testListCandidates_Basic() {
        GreenhouseListCandidatesTool tool = new GreenhouseListCandidatesTool();
        
        String input = "{\"page\": 1, \"perPage\": 10}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        
        System.out.println("=== List Candidates ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testListCandidates_WithDateFilter() {
        GreenhouseListCandidatesTool tool = new GreenhouseListCandidatesTool();
        
        // Get candidates created after a specific date
        String input = "{\"createdAfter\": \"2024-01-01T00:00:00Z\", \"perPage\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        
        System.out.println("=== List Candidates (Date Filter) ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testGetJob_WithValidId() {
        // First, get a job ID from the list
        GreenhouseListJobsTool listTool = new GreenhouseListJobsTool();
        String listInput = "{\"perPage\": 1}";
        ToolExecuteResult listResult = listTool.run(listInput);
        
        // Extract job ID from the result (assuming there is at least one job)
        if (listResult.getOutput().contains("Job ID:")) {
            String output = listResult.getOutput();
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.startsWith("Job ID:")) {
                    String jobIdStr = line.substring("Job ID:".length()).trim();
                    
                    GreenhouseGetJobTool getTool = new GreenhouseGetJobTool();
                    String input = "{\"jobId\": " + jobIdStr + "}";
                    ToolExecuteResult result = getTool.run(input);
                    
                    assertNotNull(result);
                    assertNotNull(result.getOutput());
                    assertFalse(result.getOutput().contains("Error"));
                    assertTrue(result.getOutput().contains("Job Details"));
                    
                    System.out.println("=== Get Job Details ===");
                    System.out.println(result.getOutput());
                    break;
                }
            }
        }
    }
    
    @Test
    public void testGetJob_InvalidId() {
        GreenhouseGetJobTool tool = new GreenhouseGetJobTool();
        
        String input = "{\"jobId\": 999999999}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        // May return error or "not found"
        
        System.out.println("=== Get Job (Invalid ID) ===");
        System.out.println(result.getOutput());
    }
}
