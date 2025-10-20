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
 * Trawex 活动预订工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexActivityBookingTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexActivityBookingTool() {
        this(new TrawexClient());
    }
    
    public TrawexActivityBookingTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.book_activity");
        setHumanName("Trawex活动预订");
        setDescription("Book an activity or tour. Parameters: activity_id (required), date (required, YYYY-MM-DD), participants (required, number), participant_info (required), payment_info (required)");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> activityId = new HashMap<>();
        activityId.put("type", "string");
        activityId.put("description", "Activity identifier from search results");
        properties.put("activity_id", activityId);
        
        Map<String, Object> date = new HashMap<>();
        date.put("type", "string");
        date.put("description", "Date for the activity in YYYY-MM-DD format");
        properties.put("date", date);
        
        Map<String, Object> participants = new HashMap<>();
        participants.put("type", "integer");
        participants.put("description", "Number of participants");
        properties.put("participants", participants);
        
        Map<String, Object> participantInfo = new HashMap<>();
        participantInfo.put("type", "object");
        participantInfo.put("description", "Participant information");
        properties.put("participant_info", participantInfo);
        
        Map<String, Object> paymentInfo = new HashMap<>();
        paymentInfo.put("type", "object");
        paymentInfo.put("description", "Payment information");
        properties.put("payment_info", paymentInfo);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"activity_id", "date", "participants", "participant_info", "payment_info"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexActivityBookingTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String activityId = input.getString("activity_id");
            String date = input.getString("date");
            Integer participants = input.getInteger("participants");
            Map<String, Object> participantInfo = input.getObject("participant_info", Map.class);
            Map<String, Object> paymentInfo = input.getObject("payment_info", Map.class);
            
            if (activityId == null || activityId.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"activity_id is required\"}", true);
            }
            if (date == null || date.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"date is required\"}", true);
            }
            if (participants == null || participants < 1) {
                return new ToolExecuteResult("{\"error\": \"valid participants count is required\"}", true);
            }
            if (participantInfo == null || participantInfo.isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"participant_info is required\"}", true);
            }
            if (paymentInfo == null || paymentInfo.isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"payment_info is required\"}", true);
            }
            
            String response = client.bookActivity(activityId, date, participants, participantInfo, paymentInfo);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexActivityBookingTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
