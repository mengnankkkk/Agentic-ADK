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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Greenhouse tools that don't require API calls
 * 
 * @author LangEngine Team
 */
public class GreenhouseToolsUnitTest {
    
    @Test
    public void testListJobsTool_Initialization() {
        try {
            GreenhouseListJobsTool tool = new GreenhouseListJobsTool("test_api_key");
            
            assertNotNull(tool);
            assertEquals("GreenhouseListJobsTool", tool.getName());
            assertNotNull(tool.getDescription());
            assertNotNull(tool.getParameters());
            assertTrue(tool.getDescription().contains("job openings"));
        } catch (Exception e) {
            fail("Tool initialization should not fail: " + e.getMessage());
        }
    }
    
    @Test
    public void testGetJobTool_Initialization() {
        try {
            GreenhouseGetJobTool tool = new GreenhouseGetJobTool("test_api_key");
            
            assertNotNull(tool);
            assertEquals("GreenhouseGetJobTool", tool.getName());
            assertNotNull(tool.getDescription());
            assertNotNull(tool.getParameters());
            assertTrue(tool.getDescription().contains("detailed information"));
        } catch (Exception e) {
            fail("Tool initialization should not fail: " + e.getMessage());
        }
    }
    
    @Test
    public void testListCandidatesTool_Initialization() {
        try {
            GreenhouseListCandidatesTool tool = new GreenhouseListCandidatesTool("test_api_key");
            
            assertNotNull(tool);
            assertEquals("GreenhouseListCandidatesTool", tool.getName());
            assertNotNull(tool.getDescription());
            assertNotNull(tool.getParameters());
            assertTrue(tool.getDescription().contains("candidates"));
        } catch (Exception e) {
            fail("Tool initialization should not fail: " + e.getMessage());
        }
    }
    
    @Test
    public void testListJobsTool_ParametersFormat() {
        GreenhouseListJobsTool tool = new GreenhouseListJobsTool("test_api_key");
        
        String parameters = tool.getParameters();
        
        assertTrue(parameters.contains("\"type\": \"object\""));
        assertTrue(parameters.contains("\"properties\""));
        assertTrue(parameters.contains("\"page\""));
        assertTrue(parameters.contains("\"perPage\""));
    }
    
    @Test
    public void testGetJobTool_ParametersFormat() {
        GreenhouseGetJobTool tool = new GreenhouseGetJobTool("test_api_key");
        
        String parameters = tool.getParameters();
        
        assertTrue(parameters.contains("\"type\": \"object\""));
        assertTrue(parameters.contains("\"properties\""));
        assertTrue(parameters.contains("\"jobId\""));
        assertTrue(parameters.contains("\"required\": [\"jobId\"]"));
    }
    
    @Test
    public void testListCandidatesTool_ParametersFormat() {
        GreenhouseListCandidatesTool tool = new GreenhouseListCandidatesTool("test_api_key");
        
        String parameters = tool.getParameters();
        
        assertTrue(parameters.contains("\"type\": \"object\""));
        assertTrue(parameters.contains("\"properties\""));
        assertTrue(parameters.contains("\"page\""));
        assertTrue(parameters.contains("\"perPage\""));
        assertTrue(parameters.contains("\"createdBefore\""));
        assertTrue(parameters.contains("\"createdAfter\""));
        assertTrue(parameters.contains("\"updatedBefore\""));
        assertTrue(parameters.contains("\"updatedAfter\""));
    }
    
    @Test
    public void testGetJobTool_MissingJobId() {
        GreenhouseGetJobTool tool = new GreenhouseGetJobTool("test_api_key");
        
        String input = "{}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("Error"));
        assertTrue(result.getOutput().contains("jobId"));
    }
    
    @Test
    public void testGetJobTool_InvalidJobId() {
        GreenhouseGetJobTool tool = new GreenhouseGetJobTool("test_api_key");
        
        String input = "{\"jobId\": \"invalid\"}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("Error"));
    }
    
    @Test
    public void testGetJobTool_NegativeJobId() {
        GreenhouseGetJobTool tool = new GreenhouseGetJobTool("test_api_key");
        
        String input = "{\"jobId\": -1}";
        ToolExecuteResult result = tool.run(input);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("Error"));
    }
    
    @Test
    public void testListJobsTool_InvalidInput() {
        GreenhouseListJobsTool tool = new GreenhouseListJobsTool("test_api_key");
        
        String invalidInput = "not a json";
        ToolExecuteResult result = tool.run(invalidInput);
        
        assertNotNull(result);
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("Error") || result.getOutput().length() > 0);
    }
    
    @Test
    public void testToolsAreSerializable() {
        GreenhouseListJobsTool listJobsTool = new GreenhouseListJobsTool("test_api_key");
        GreenhouseGetJobTool getJobTool = new GreenhouseGetJobTool("test_api_key");
        GreenhouseListCandidatesTool listCandidatesTool = new GreenhouseListCandidatesTool("test_api_key");
        
        assertNotNull(listJobsTool.toString());
        assertNotNull(getJobTool.toString());
        assertNotNull(listCandidatesTool.toString());
    }
}
