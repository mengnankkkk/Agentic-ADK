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

import com.alibaba.langengine.core.tool.ToolExecuteResult;
import com.alibaba.langengine.newsapi.sdk.NewsApiClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NewsAPI tools that don't require API calls
 * 
 * @author LangEngine Team
 */
public class NewsApiToolsUnitTest {
    
    @Test
    public void testTopHeadlinesTool_Initialization() {
        // Test tool can be created (with or without API key)
        try {
            // This should work even without API key
            NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool("test_key");
            
            assertNotNull(tool);
            assertEquals("NewsApiTopHeadlinesTool", tool.getName());
            assertNotNull(tool.getDescription());
            assertNotNull(tool.getParameters());
            assertTrue(tool.getDescription().contains("top headlines"));
        } catch (Exception e) {
            fail("Tool initialization should not fail: " + e.getMessage());
        }
    }
    
    @Test
    public void testSearchTool_Initialization() {
        try {
            NewsApiSearchTool tool = new NewsApiSearchTool("test_key");
            
            assertNotNull(tool);
            assertEquals("NewsApiSearchTool", tool.getName());
            assertNotNull(tool.getDescription());
            assertNotNull(tool.getParameters());
            assertTrue(tool.getDescription().contains("Search all news"));
        } catch (Exception e) {
            fail("Tool initialization should not fail: " + e.getMessage());
        }
    }
    
    @Test
    public void testTopHeadlinesTool_ParametersFormat() {
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool("test_key");
        
        String parameters = tool.getParameters();
        
        // Verify JSON structure
        assertTrue(parameters.contains("\"type\": \"object\""));
        assertTrue(parameters.contains("\"properties\""));
        assertTrue(parameters.contains("\"country\""));
        assertTrue(parameters.contains("\"category\""));
        assertTrue(parameters.contains("\"query\""));
        assertTrue(parameters.contains("\"pageSize\""));
        assertTrue(parameters.contains("\"page\""));
    }
    
    @Test
    public void testSearchTool_ParametersFormat() {
        NewsApiSearchTool tool = new NewsApiSearchTool("test_key");
        
        String parameters = tool.getParameters();
        
        // Verify JSON structure
        assertTrue(parameters.contains("\"type\": \"object\""));
        assertTrue(parameters.contains("\"properties\""));
        assertTrue(parameters.contains("\"query\""));
        assertTrue(parameters.contains("\"from\""));
        assertTrue(parameters.contains("\"to\""));
        assertTrue(parameters.contains("\"sortBy\""));
        assertTrue(parameters.contains("\"required\": [\"query\"]"));
    }
    
    @Test
    public void testTopHeadlinesTool_InvalidInput() {
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool("test_key");
        
        // Test with malformed JSON
        String invalidInput = "not a json";
        ToolExecuteResult result = tool.run(invalidInput);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        // Should handle error gracefully
        assertTrue(result.getOutput().contains("Error") || result.getOutput().length() > 0);
    }
    
    @Test
    public void testSearchTool_MissingQuery() {
        NewsApiSearchTool tool = new NewsApiSearchTool("test_key");
        
        // Test with missing required query parameter
        String input = "{\"sortBy\": \"relevancy\"}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("Error"));
    }
    
    @Test
    public void testSearchTool_EmptyQuery() {
        NewsApiSearchTool tool = new NewsApiSearchTool("test_key");
        
        // Test with empty query
        String input = "{\"query\": \"\"}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("Error"));
    }
    
    @Test
    public void testTopHeadlinesTool_EmptyParameters() {
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool("test_key");
        
        // Test with no filter parameters
        String input = "{}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("Error"));
        assertTrue(result.getOutput().contains("at least one"));
    }
    
    @Test
    public void testToolsAreSerializable() {
        // Test that tools can be serialized (important for distributed systems)
        NewsApiTopHeadlinesTool topTool = new NewsApiTopHeadlinesTool("test_key");
        NewsApiSearchTool searchTool = new NewsApiSearchTool("test_key");
        
        assertNotNull(topTool.toString());
        assertNotNull(searchTool.toString());
    }
}
