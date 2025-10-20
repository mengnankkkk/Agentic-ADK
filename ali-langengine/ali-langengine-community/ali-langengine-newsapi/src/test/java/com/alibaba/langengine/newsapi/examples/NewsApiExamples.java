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
package com.alibaba.langengine.newsapi.examples;

import com.alibaba.langengine.core.tool.ToolExecuteResult;
import com.alibaba.langengine.newsapi.tool.NewsApiSearchTool;
import com.alibaba.langengine.newsapi.tool.NewsApiTopHeadlinesTool;

/**
 * NewsAPI Integration Examples
 * 
 * This class demonstrates how to use NewsAPI tools in your application.
 * 
 * Before running these examples:
 * 1. Get a free API key from https://newsapi.org/
 * 2. Set the NEWS_API_KEY environment variable
 * 
 * @author LangEngine Team
 */
public class NewsApiExamples {
    
    /**
     * Example 1: Get top headlines from a specific country
     */
    public static void example1_TopHeadlinesByCountry() {
        System.out.println("=== Example 1: Top Headlines by Country ===\n");
        
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool();
        
        // Get top headlines from United States
        String input = "{\"country\": \"us\", \"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 2: Get top headlines by category
     */
    public static void example2_TopHeadlinesByCategory() {
        System.out.println("=== Example 2: Top Headlines by Category ===\n");
        
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool();
        
        // Get technology news from US
        String input = "{\"country\": \"us\", \"category\": \"technology\", \"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 3: Search headlines by keyword
     */
    public static void example3_SearchHeadlines() {
        System.out.println("=== Example 3: Search Headlines ===\n");
        
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool();
        
        // Search for AI-related headlines
        String input = "{\"query\": \"artificial intelligence\", \"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 4: Search all articles with basic query
     */
    public static void example4_SearchAllArticles() {
        System.out.println("=== Example 4: Search All Articles ===\n");
        
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Search for climate change articles
        String input = "{\"query\": \"climate change\", \"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 5: Search with date range
     */
    public static void example5_SearchWithDateRange() {
        System.out.println("=== Example 5: Search with Date Range ===\n");
        
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Search for recent articles about space exploration
        String input = "{\"query\": \"space exploration\", " +
                "\"from\": \"2024-01-01\", " +
                "\"to\": \"2024-12-31\", " +
                "\"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 6: Search with sorting
     */
    public static void example6_SearchWithSorting() {
        System.out.println("=== Example 6: Search with Sorting ===\n");
        
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Get most popular tech articles
        String input = "{\"query\": \"technology\", " +
                "\"sortBy\": \"popularity\", " +
                "\"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 7: Complex query with operators
     */
    public static void example7_ComplexQuery() {
        System.out.println("=== Example 7: Complex Query ===\n");
        
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Search using OR operator
        String input = "{\"query\": \"\\\"machine learning\\\" OR \\\"deep learning\\\"\", " +
                "\"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 8: Get latest news sorted by publication date
     */
    public static void example8_LatestNews() {
        System.out.println("=== Example 8: Latest News ===\n");
        
        NewsApiSearchTool tool = new NewsApiSearchTool();
        
        // Get latest sports news
        String input = "{\"query\": \"sports\", " +
                "\"sortBy\": \"publishedAt\", " +
                "\"pageSize\": 5}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Example 9: Multiple categories of news
     */
    public static void example9_MultipleCategories() {
        System.out.println("=== Example 9: Multiple Categories ===\n");
        
        NewsApiTopHeadlinesTool tool = new NewsApiTopHeadlinesTool();
        
        String[] categories = {"business", "technology", "health"};
        
        for (String category : categories) {
            System.out.println("--- " + category.toUpperCase() + " ---");
            String input = String.format("{\"country\": \"us\", \"category\": \"%s\", \"pageSize\": 2}", category);
            ToolExecuteResult result = tool.run(input);
            System.out.println(result.getOutput());
        }
        
        System.out.println("\n");
    }
    
    /**
     * Example 10: Custom API key usage
     */
    public static void example10_CustomApiKey() {
        System.out.println("=== Example 10: Custom API Key ===\n");
        
        // You can pass API key directly instead of using environment variable
        String customApiKey = System.getenv("NEWS_API_KEY"); // Replace with your key
        
        NewsApiSearchTool tool = new NewsApiSearchTool(customApiKey);
        
        String input = "{\"query\": \"innovation\", \"pageSize\": 3}";
        ToolExecuteResult result = tool.run(input);
        
        System.out.println(result.getOutput());
        System.out.println("\n");
    }
    
    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        // Check if API key is set
        String apiKey = System.getenv("NEWS_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("ERROR: NEWS_API_KEY environment variable is not set!");
            System.err.println("Please set it before running these examples:");
            System.err.println("  Windows PowerShell: $env:NEWS_API_KEY=\"your_key\"");
            System.err.println("  Windows CMD: set NEWS_API_KEY=your_key");
            System.err.println("  Linux/Mac: export NEWS_API_KEY=your_key");
            System.err.println("\nGet your free API key from https://newsapi.org/");
            return;
        }
        
        System.out.println("========================================");
        System.out.println("NewsAPI Integration Examples");
        System.out.println("========================================\n");
        
        try {
            // Run examples with delays to respect rate limits
            example1_TopHeadlinesByCountry();
            Thread.sleep(1000);
            
            example2_TopHeadlinesByCategory();
            Thread.sleep(1000);
            
            example3_SearchHeadlines();
            Thread.sleep(1000);
            
            example4_SearchAllArticles();
            Thread.sleep(1000);
            
            example5_SearchWithDateRange();
            Thread.sleep(1000);
            
            example6_SearchWithSorting();
            Thread.sleep(1000);
            
            example7_ComplexQuery();
            Thread.sleep(1000);
            
            example8_LatestNews();
            Thread.sleep(1000);
            
            example9_MultipleCategories();
            Thread.sleep(1000);
            
            example10_CustomApiKey();
            
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
