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
 * Trawex 酒店详情工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexHotelDetailsTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexHotelDetailsTool() {
        this(new TrawexClient());
    }
    
    public TrawexHotelDetailsTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.get_hotel_details");
        setHumanName("Trawex酒店详情");
        setDescription("Get detailed information about a specific hotel. Parameters: hotel_id (required), check_in (required, YYYY-MM-DD), check_out (required, YYYY-MM-DD), adults (optional, default 2), rooms (optional, default 1)");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> hotelId = new HashMap<>();
        hotelId.put("type", "string");
        hotelId.put("description", "Unique hotel identifier from search results");
        properties.put("hotel_id", hotelId);
        
        Map<String, Object> checkIn = new HashMap<>();
        checkIn.put("type", "string");
        checkIn.put("description", "Check-in date in YYYY-MM-DD format");
        properties.put("check_in", checkIn);
        
        Map<String, Object> checkOut = new HashMap<>();
        checkOut.put("type", "string");
        checkOut.put("description", "Check-out date in YYYY-MM-DD format");
        properties.put("check_out", checkOut);
        
        Map<String, Object> adults = new HashMap<>();
        adults.put("type", "integer");
        adults.put("description", "Number of adults (default: 2)");
        properties.put("adults", adults);
        
        Map<String, Object> rooms = new HashMap<>();
        rooms.put("type", "integer");
        rooms.put("description", "Number of rooms (default: 1)");
        properties.put("rooms", rooms);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"hotel_id", "check_in", "check_out"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexHotelDetailsTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String hotelId = input.getString("hotel_id");
            String checkIn = input.getString("check_in");
            String checkOut = input.getString("check_out");
            Integer adults = input.getInteger("adults");
            Integer rooms = input.getInteger("rooms");
            
            if (hotelId == null || hotelId.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"hotel_id is required\"}", true);
            }
            if (checkIn == null || checkIn.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"check_in date is required\"}", true);
            }
            if (checkOut == null || checkOut.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"check_out date is required\"}", true);
            }
            
            String response = client.getHotelDetails(hotelId, checkIn, checkOut, adults, rooms);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexHotelDetailsTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
