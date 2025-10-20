package com.alibaba.langengine.trawex.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.langengine.core.callback.ExecutionContext;
import com.alibaba.langengine.core.tool.BaseTool;
import com.alibaba.langengine.core.tool.ToolExecuteResult;
import com.alibaba.langengine.trawex.service.TrawexClient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Trawex 活动搜索工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexActivitySearchTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexActivitySearchTool() {
        this(new TrawexClient());
    }
    
    public TrawexActivitySearchTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.search_activities");
        setHumanName("Trawex活动搜索");
        setDescription("Search for activities and tours. Parameters: destination (required), date (optional, YYYY-MM-DD), category (optional, e.g., 'tours', 'adventure', 'cultural', 'food')");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> destination = new HashMap<>();
        destination.put("type", "string");
        destination.put("description", "Destination city or location for activities");
        properties.put("destination", destination);
        
        Map<String, Object> date = new HashMap<>();
        date.put("type", "string");
        date.put("description", "Date for the activity in YYYY-MM-DD format (optional)");
        properties.put("date", date);
        
        Map<String, Object> category = new HashMap<>();
        category.put("type", "string");
        category.put("description", "Activity category: tours, adventure, cultural, food, etc.");
        properties.put("category", category);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"destination"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexActivitySearchTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String destination = input.getString("destination");
            String date = input.getString("date");
            String category = input.getString("category");
            
            if (destination == null || destination.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"destination is required\"}", true);
            }
            
            String response = client.searchActivities(destination, date, category);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexActivitySearchTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
