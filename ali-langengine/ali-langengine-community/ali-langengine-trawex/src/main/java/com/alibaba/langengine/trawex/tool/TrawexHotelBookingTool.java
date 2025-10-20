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
 * Trawex 酒店预订工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexHotelBookingTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexHotelBookingTool() {
        this(new TrawexClient());
    }
    
    public TrawexHotelBookingTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.book_hotel");
        setHumanName("Trawex酒店预订");
        setDescription("Book a hotel room. Parameters: hotel_id (required), check_in (required, YYYY-MM-DD), check_out (required, YYYY-MM-DD), guest_info (required, guest details), payment_info (required, payment details)");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> hotelId = new HashMap<>();
        hotelId.put("type", "string");
        hotelId.put("description", "Hotel identifier from search results");
        properties.put("hotel_id", hotelId);
        
        Map<String, Object> checkIn = new HashMap<>();
        checkIn.put("type", "string");
        checkIn.put("description", "Check-in date in YYYY-MM-DD format");
        properties.put("check_in", checkIn);
        
        Map<String, Object> checkOut = new HashMap<>();
        checkOut.put("type", "string");
        checkOut.put("description", "Check-out date in YYYY-MM-DD format");
        properties.put("check_out", checkOut);
        
        Map<String, Object> guestInfo = new HashMap<>();
        guestInfo.put("type", "object");
        guestInfo.put("description", "Guest information including name, email, phone");
        properties.put("guest_info", guestInfo);
        
        Map<String, Object> paymentInfo = new HashMap<>();
        paymentInfo.put("type", "object");
        paymentInfo.put("description", "Payment information");
        properties.put("payment_info", paymentInfo);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"hotel_id", "check_in", "check_out", "guest_info", "payment_info"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexHotelBookingTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String hotelId = input.getString("hotel_id");
            String checkIn = input.getString("check_in");
            String checkOut = input.getString("check_out");
            Map<String, Object> guestInfo = input.getObject("guest_info", Map.class);
            Map<String, Object> paymentInfo = input.getObject("payment_info", Map.class);
            
            if (hotelId == null || hotelId.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"hotel_id is required\"}", true);
            }
            if (checkIn == null || checkIn.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"check_in is required\"}", true);
            }
            if (checkOut == null || checkOut.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"check_out is required\"}", true);
            }
            if (guestInfo == null || guestInfo.isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"guest_info is required\"}", true);
            }
            if (paymentInfo == null || paymentInfo.isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"payment_info is required\"}", true);
            }
            
            String response = client.bookHotel(hotelId, checkIn, checkOut, guestInfo, paymentInfo);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexHotelBookingTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
