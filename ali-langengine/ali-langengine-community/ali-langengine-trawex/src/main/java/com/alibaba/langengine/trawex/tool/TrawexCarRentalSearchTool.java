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
 * Trawex 租车搜索工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexCarRentalSearchTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexCarRentalSearchTool() {
        this(new TrawexClient());
    }
    
    public TrawexCarRentalSearchTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.search_car_rentals");
        setHumanName("Trawex租车搜索");
        setDescription("Search for car rentals. Parameters: pickup_location (required), pickup_date (required, YYYY-MM-DD HH:mm), dropoff_date (required, YYYY-MM-DD HH:mm), dropoff_location (optional), car_type (optional, e.g., 'economy', 'suv', 'luxury')");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> pickupLocation = new HashMap<>();
        pickupLocation.put("type", "string");
        pickupLocation.put("description", "Pickup location (airport code or city)");
        properties.put("pickup_location", pickupLocation);
        
        Map<String, Object> pickupDate = new HashMap<>();
        pickupDate.put("type", "string");
        pickupDate.put("description", "Pickup date and time in YYYY-MM-DD HH:mm format");
        properties.put("pickup_date", pickupDate);
        
        Map<String, Object> dropoffDate = new HashMap<>();
        dropoffDate.put("type", "string");
        dropoffDate.put("description", "Drop-off date and time in YYYY-MM-DD HH:mm format");
        properties.put("dropoff_date", dropoffDate);
        
        Map<String, Object> dropoffLocation = new HashMap<>();
        dropoffLocation.put("type", "string");
        dropoffLocation.put("description", "Drop-off location (optional, defaults to pickup location)");
        properties.put("dropoff_location", dropoffLocation);
        
        Map<String, Object> carType = new HashMap<>();
        carType.put("type", "string");
        carType.put("description", "Car type: economy, compact, suv, luxury, etc.");
        properties.put("car_type", carType);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"pickup_location", "pickup_date", "dropoff_date"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexCarRentalSearchTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String pickupLocation = input.getString("pickup_location");
            String pickupDate = input.getString("pickup_date");
            String dropoffDate = input.getString("dropoff_date");
            String dropoffLocation = input.getString("dropoff_location");
            String carType = input.getString("car_type");
            
            if (pickupLocation == null || pickupLocation.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"pickup_location is required\"}", true);
            }
            if (pickupDate == null || pickupDate.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"pickup_date is required\"}", true);
            }
            if (dropoffDate == null || dropoffDate.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"dropoff_date is required\"}", true);
            }
            
            String response = client.searchCarRentals(pickupLocation, pickupDate, dropoffDate, dropoffLocation, carType);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexCarRentalSearchTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
