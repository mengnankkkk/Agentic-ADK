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
 * Trawex 航班预订工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexFlightBookingTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexFlightBookingTool() {
        this(new TrawexClient());
    }
    
    public TrawexFlightBookingTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.book_flight");
        setHumanName("Trawex航班预订");
        setDescription("Book a flight. Parameters: flight_id (required), passenger_info (required, passenger details), payment_info (required, payment details)");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> flightId = new HashMap<>();
        flightId.put("type", "string");
        flightId.put("description", "Flight identifier from search results");
        properties.put("flight_id", flightId);
        
        Map<String, Object> passengerInfo = new HashMap<>();
        passengerInfo.put("type", "object");
        passengerInfo.put("description", "Passenger information including name, passport, date of birth");
        properties.put("passenger_info", passengerInfo);
        
        Map<String, Object> paymentInfo = new HashMap<>();
        paymentInfo.put("type", "object");
        paymentInfo.put("description", "Payment information");
        properties.put("payment_info", paymentInfo);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"flight_id", "passenger_info", "payment_info"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexFlightBookingTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String flightId = input.getString("flight_id");
            Map<String, Object> passengerInfo = input.getObject("passenger_info", Map.class);
            Map<String, Object> paymentInfo = input.getObject("payment_info", Map.class);
            
            if (flightId == null || flightId.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"flight_id is required\"}", true);
            }
            if (passengerInfo == null || passengerInfo.isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"passenger_info is required\"}", true);
            }
            if (paymentInfo == null || paymentInfo.isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"payment_info is required\"}", true);
            }
            
            String response = client.bookFlight(flightId, passengerInfo, paymentInfo);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexFlightBookingTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
