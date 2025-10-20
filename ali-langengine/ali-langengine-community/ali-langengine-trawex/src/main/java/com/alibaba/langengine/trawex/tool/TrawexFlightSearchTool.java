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
 * Trawex 航班搜索工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexFlightSearchTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexFlightSearchTool() {
        this(new TrawexClient());
    }
    
    public TrawexFlightSearchTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.search_flights");
        setHumanName("Trawex航班搜索");
        setDescription("Search for flights using Trawex API. Parameters: origin (required, airport code), destination (required, airport code), departure_date (required, YYYY-MM-DD), return_date (optional, for round trips), adults (optional, default 1), cabin_class (optional, economy/business/first)");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> origin = new HashMap<>();
        origin.put("type", "string");
        origin.put("description", "Origin airport code (e.g., 'JFK', 'LAX', 'DXB')");
        properties.put("origin", origin);
        
        Map<String, Object> destination = new HashMap<>();
        destination.put("type", "string");
        destination.put("description", "Destination airport code");
        properties.put("destination", destination);
        
        Map<String, Object> departureDate = new HashMap<>();
        departureDate.put("type", "string");
        departureDate.put("description", "Departure date in YYYY-MM-DD format");
        properties.put("departure_date", departureDate);
        
        Map<String, Object> returnDate = new HashMap<>();
        returnDate.put("type", "string");
        returnDate.put("description", "Return date for round trip in YYYY-MM-DD format (optional)");
        properties.put("return_date", returnDate);
        
        Map<String, Object> adults = new HashMap<>();
        adults.put("type", "integer");
        adults.put("description", "Number of adults (default: 1)");
        adults.put("minimum", 1);
        properties.put("adults", adults);
        
        Map<String, Object> cabinClass = new HashMap<>();
        cabinClass.put("type", "string");
        cabinClass.put("description", "Cabin class: economy, business, or first (default: economy)");
        cabinClass.put("enum", new String[]{"economy", "business", "first"});
        properties.put("cabin_class", cabinClass);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"origin", "destination", "departure_date"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexFlightSearchTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String origin = input.getString("origin");
            String destination = input.getString("destination");
            String departureDate = input.getString("departure_date");
            String returnDate = input.getString("return_date");
            Integer adults = input.getInteger("adults");
            String cabinClass = input.getString("cabin_class");
            
            if (origin == null || origin.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"origin airport code is required\"}", true);
            }
            if (destination == null || destination.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"destination airport code is required\"}", true);
            }
            if (departureDate == null || departureDate.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"departure_date is required\"}", true);
            }
            
            String response = client.searchFlights(origin, destination, departureDate, 
                returnDate, adults, cabinClass);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexFlightSearchTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
