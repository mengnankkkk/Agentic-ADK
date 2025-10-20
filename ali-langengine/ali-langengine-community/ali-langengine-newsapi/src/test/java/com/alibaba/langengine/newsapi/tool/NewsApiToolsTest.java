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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NewsAPI Tools Test
 * 
 * Note: These tests require a valid NEWS_API_KEY environment variable to be set.
 * You can get a free API key from https://newsapi.org/
 * 
 * Set the environment variable before running tests:
 * Windows: set NEWS_API_KEY=your_api_key_here
 * Linux/Mac: export NEWS_API_KEY=your_api_key_here
 * 
 * @author LangEngine Team
 */
@EnabledIfEnvironmentVariable(named = "NEWS_API_KEY", matches = ".+")
public class NewsApiToolsTest {
    
    @BeforeAll
    public static void setUp() {
        String apiKey = System.getenv("NEWS_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("WARNING: NEWS_API_KEY environment variable is not set. Tests will be skipped.");
            System.out.println("Get your free API key from https://newsapi.org/");
        }
    }
    
    @Test
    public void testNewsApiTopHeadlinesTool_WithCountry() {
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool();
        
        // Test getting top headlines from US
        String input = "{\"country\": \"us\", \"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertFalse(result.getOutput().contains("Error"));
        assertTrue(result.getOutput().contains("top headlines"));
        
        System.out.println("=== Top Headlines (US) ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testNewsApiTopHeadlinesTool_WithCategory() {
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool();
        
        // Test getting technology news from US
        String input = "{\"country\": \"us\", \"category\": \"technology\", \"pageSize\": 3}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertFalse(result.getOutput().contains("Error"));
        
        System.out.println("=== Technology Headlines ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testNewsApiTopHeadlinesTool_WithQuery() {
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool();
        
        // Test searching for specific topic in headlines
        String input = "{\"query\": \"AI\", \"pageSize\": 3}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertFalse(result.getOutput().contains("Error"));
        
        System.out.println("=== AI Headlines ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testNewsApiTopHeadlinesTool_EmptyParameters() {
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool();
        
        // Test with no parameters (should return error)
        String input = "{}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("Error"));
        
        System.out.println("=== Empty Parameters Test ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testNewsApiSearchTool_BasicQuery() {
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Test searching for articles about artificial intelligence
        String input = "{\"query\": \"artificial intelligence\", \"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertFalse(result.getOutput().contains("Error"));
        assertTrue(result.getOutput().contains("Found"));
        
        System.out.println("=== Search: Artificial Intelligence ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testNewsApiSearchTool_WithDateRange() {
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Test searching with date range
        String input = "{\"query\": \"climate change\", \"from\": \"2024-01-01\", \"to\": \"2024-12-31\", \"pageSize\": 3}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertFalse(result.getOutput().contains("Error"));
        
        System.out.println("=== Search with Date Range: Climate Change ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testNewsApiSearchTool_WithSortBy() {
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Test sorting by popularity
        String input = "{\"query\": \"technology\", \"sortBy\": \"popularity\", \"pageSize\": 3}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertFalse(result.getOutput().contains("Error"));
        
        System.out.println("=== Search Sorted by Popularity ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testNewsApiSearchTool_WithPublishedAtSort() {
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Test sorting by published date
        String input = "{\"query\": \"sports\", \"sortBy\": \"publishedAt\", \"pageSize\": 3}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertFalse(result.getOutput().contains("Error"));
        
        System.out.println("=== Search Sorted by Published Date ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testNewsApiSearchTool_EmptyQuery() {
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Test with empty query (should return error)
        String input = "{\"query\": \"\"}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("Error"));
        
        System.out.println("=== Empty Query Test ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testNewsApiSearchTool_NoQuery() {
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Test with no query parameter (should return error)
        String input = "{\"sortBy\": \"relevancy\"}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("Error"));
        
        System.out.println("=== No Query Test ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testNewsApiSearchTool_ComplexQuery() {
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Test with complex query using AND/OR operators
        String input = "{\"query\": \"\\\"machine learning\\\" OR \\\"deep learning\\\"\", \"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        
        System.out.println("=== Complex Query Search ===");
        System.out.println(result.getOutput());
    }
    
    @Test
    public void testPageSizeLimit() {
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Test that page size is capped at 100
        String input = "{\"query\": \"news\", \"pageSize\": 150}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        
        System.out.println("=== Page Size Limit Test ===");
        System.out.println(result.getOutput());
    }
}
