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
 * Trawex 航班详情工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexFlightDetailsTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexFlightDetailsTool() {
        this(new TrawexClient());
    }
    
    public TrawexFlightDetailsTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.get_flight_details");
        setHumanName("Trawex航班详情");
        setDescription("Get detailed information about a specific flight. Parameters: flight_id (required, flight identifier from search results)");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> flightId = new HashMap<>();
        flightId.put("type", "string");
        flightId.put("description", "Unique flight identifier from search results");
        properties.put("flight_id", flightId);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"flight_id"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexFlightDetailsTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String flightId = input.getString("flight_id");
            
            if (flightId == null || flightId.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"flight_id is required\"}", true);
            }
            
            String response = client.getFlightDetails(flightId);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexFlightDetailsTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
