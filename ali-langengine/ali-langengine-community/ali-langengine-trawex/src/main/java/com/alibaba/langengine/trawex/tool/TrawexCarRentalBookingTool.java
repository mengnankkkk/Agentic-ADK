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
 * Trawex 租车预订工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexCarRentalBookingTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexCarRentalBookingTool() {
        this(new TrawexClient());
    }
    
    public TrawexCarRentalBookingTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.book_car_rental");
        setHumanName("Trawex租车预订");
        setDescription("Book a car rental. Parameters: car_id (required), driver_info (required, driver details including license), payment_info (required)");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> carId = new HashMap<>();
        carId.put("type", "string");
        carId.put("description", "Car identifier from search results");
        properties.put("car_id", carId);
        
        Map<String, Object> driverInfo = new HashMap<>();
        driverInfo.put("type", "object");
        driverInfo.put("description", "Driver information including name, license number, contact");
        properties.put("driver_info", driverInfo);
        
        Map<String, Object> paymentInfo = new HashMap<>();
        paymentInfo.put("type", "object");
        paymentInfo.put("description", "Payment information");
        properties.put("payment_info", paymentInfo);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"car_id", "driver_info", "payment_info"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexCarRentalBookingTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String carId = input.getString("car_id");
            Map<String, Object> driverInfo = input.getObject("driver_info", Map.class);
            Map<String, Object> paymentInfo = input.getObject("payment_info", Map.class);
            
            if (carId == null || carId.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"car_id is required\"}", true);
            }
            if (driverInfo == null || driverInfo.isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"driver_info is required\"}", true);
            }
            if (paymentInfo == null || paymentInfo.isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"payment_info is required\"}", true);
            }
            
            String response = client.bookCarRental(carId, driverInfo, paymentInfo);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexCarRentalBookingTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
