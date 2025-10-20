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
package com.alibaba.langengine.greenhouse.examples;

import com.alibaba.langengine.core.tool.ToolExecuteResult;
import com.alibaba.langengine.greenhouse.tool.GreenhouseGetJobTool;
import com.alibaba.langengine.greenhouse.tool.GreenhouseListCandidatesTool;
import com.alibaba.langengine.greenhouse.tool.GreenhouseListJobsTool;

/**
 * Greenhouse Integration Examples
 * 
 * This class demonstrates how to use Greenhouse tools in your application.
 * 
 * Before running these examples:
 * 1. Get your Harvest API key from Greenhouse settings
 * 2. Set the GREENHOUSE_API_KEY environment variable
 * 
 * @author LangEngine Team
 */
public class GreenhouseExamples {
    
    /**
     * Example 1: List all jobs
     */
    public static void example1_ListJobs() {
        System.out.println("=== Example 1: List All Jobs ===\n");
        
        GreenhouseListJobsTool tool = new GreenhouseListJobsTool();
        
        String input = "{\"page\": 1, \"perPage\": 10}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 2: List jobs with default parameters
     */
    public static void example2_ListJobsDefault() {
        System.out.println("=== Example 2: List Jobs (Default Parameters) ===\n");
        
        GreenhouseListJobsTool tool = new GreenhouseListJobsTool();
        
        String input = "{}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 3: Get specific job details
     */
    public static void example3_GetJobDetails() {
        System.out.println("=== Example 3: Get Job Details ===\n");
        
        // First, get a job ID from the list
        GreenhouseListJobsTool listTool = new GreenhouseListJobsTool();
        String listInput = "{\"perPage\": 1}";
        ToolExecuteResult listResult = listTool.run(listInput);
        
        // Extract job ID from the result
        if (listResult.getOutput().contains("Job ID:")) {
            String output = listResult.getOutput();
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.startsWith("Job ID:")) {
                    String jobIdStr = line.substring("Job ID:".length()).trim();
                    
                    GreenhouseGetJobTool getTool = new GreenhouseGetJobTool();
                    String input = "{\"jobId\": " + jobIdStr + "}";
                    ToolExecuteResult result = getTool.run(input);
                    
                    System.out.println(result.getOutput());
                    break;
                }
            }
        } else {
            System.out.println("No jobs found to display details");
        }
        
        System.out.println("\n");
    }
    
    /**
     * Example 4: List all candidates
     */
    public static void example4_ListCandidates() {
        System.out.println("=== Example 4: List All Candidates ===\n");
        
        GreenhouseListCandidatesTool tool = new GreenhouseListCandidatesTool();
        
        String input = "{\"page\": 1, \"perPage\": 10}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 5: List candidates created after a specific date
     */
    public static void example5_ListCandidatesDateFilter() {
        System.out.println("=== Example 5: List Candidates (Date Filter) ===\n");
        
        GreenhouseListCandidatesTool tool = new GreenhouseListCandidatesTool();
        
        String input = "{\"createdAfter\": \"2024-01-01T00:00:00Z\", \"perPage\": 10}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 6: List recently updated candidates
     */
    public static void example6_ListRecentlyUpdatedCandidates() {
        System.out.println("=== Example 6: List Recently Updated Candidates ===\n");
        
        GreenhouseListCandidatesTool tool = new GreenhouseListCandidatesTool();
        
        String input = "{\"updatedAfter\": \"2024-06-01T00:00:00Z\", \"perPage\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 7: Pagination through jobs
     */
    public static void example7_PaginationExample() {
        System.out.println("=== Example 7: Pagination Example ===\n");
        
        GreenhouseListJobsTool tool = new GreenhouseListJobsTool();
        
        // Get first page
        System.out.println("--- Page 1 ---");
        String input1 = "{\"page\": 1, \"perPage\": 5}";
        ToolExecuteResult result1 = tool.run(input1);
        System.out.println(result1.getOutput());
        
        // Get second page
        System.out.println("--- Page 2 ---");
        String input2 = "{\"page\": 2, \"perPage\": 5}";
        ToolExecuteResult result2 = tool.run(input2);
        System.out.println(result2.getOutput());
        
        System.out.println("\n");
    }
    
    /**
     * Example 8: Custom API key usage
     */
    public static void example8_CustomApiKey() {
        System.out.println("=== Example 8: Custom API Key ===\n");
        
        String customApiKey = System.getenv("GREENHOUSE_API_KEY");
        
        GreenhouseListJobsTool tool = new GreenhouseListJobsTool(customApiKey);
        
        String input = "{\"perPage\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        // Check if API key is set
        String apiKey = System.getenv("GREENHOUSE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("ERROR: GREENHOUSE_API_KEY environment variable is not set!");
            System.err.println("Please set it before running these examples:");
            System.err.println("  Windows PowerShell: $env:GREENHOUSE_API_KEY=\"your_key\"");
            System.err.println("  Windows CMD: set GREENHOUSE_API_KEY=your_key");
            System.err.println("  Linux/Mac: export GREENHOUSE_API_KEY=your_key");
            System.err.println("\nGet your API key from Greenhouse Settings > API Credential Management");
            return;
        }
        
        System.out.println("========================================");
        System.out.println("Greenhouse Integration Examples");
        System.out.println("========================================\n");
        
        try {
            example1_ListJobs();
            Thread.sleep(1000);
            
            example2_ListJobsDefault();
            Thread.sleep(1000);
            
            example3_GetJobDetails();
            Thread.sleep(1000);
            
            example4_ListCandidates();
            Thread.sleep(1000);
            
            example5_ListCandidatesDateFilter();
            Thread.sleep(1000);
            
            example6_ListRecentlyUpdatedCandidates();
            Thread.sleep(1000);
            
            example7_PaginationExample();
            Thread.sleep(1000);
            
            example8_CustomApiKey();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Examples interrupted");
        } catch (Exception e) {
            System.err.println("Error running examples: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("========================================");
        System.out.println("All examples completed!");
        System.out.println("========================================");
    }
}
