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
 * Trawex 旅游套餐搜索工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexPackageSearchTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexPackageSearchTool() {
        this(new TrawexClient());
    }
    
    public TrawexPackageSearchTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.search_packages");
        setHumanName("Trawex套餐搜索");
        setDescription("Search for travel packages. Parameters: destination (required), departure_date (required, YYYY-MM-DD), return_date (required, YYYY-MM-DD), adults (optional, default 2), package_type (optional, e.g., 'all-inclusive', 'beach', 'adventure')");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> destination = new HashMap<>();
        destination.put("type", "string");
        destination.put("description", "Destination for the package");
        properties.put("destination", destination);
        
        Map<String, Object> departureDate = new HashMap<>();
        departureDate.put("type", "string");
        departureDate.put("description", "Departure date in YYYY-MM-DD format");
        properties.put("departure_date", departureDate);
        
        Map<String, Object> returnDate = new HashMap<>();
        returnDate.put("type", "string");
        returnDate.put("description", "Return date in YYYY-MM-DD format");
        properties.put("return_date", returnDate);
        
        Map<String, Object> adults = new HashMap<>();
        adults.put("type", "integer");
        adults.put("description", "Number of adults (default: 2)");
        properties.put("adults", adults);
        
        Map<String, Object> packageType = new HashMap<>();
        packageType.put("type", "string");
        packageType.put("description", "Package type: all-inclusive, beach, adventure, cultural, etc.");
        properties.put("package_type", packageType);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"destination", "departure_date", "return_date"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexPackageSearchTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String destination = input.getString("destination");
            String departureDate = input.getString("departure_date");
            String returnDate = input.getString("return_date");
            Integer adults = input.getInteger("adults");
            String packageType = input.getString("package_type");
            
            if (destination == null || destination.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"destination is required\"}", true);
            }
            if (departureDate == null || departureDate.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"departure_date is required\"}", true);
            }
            if (returnDate == null || returnDate.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"return_date is required\"}", true);
            }
            
            String response = client.searchPackages(destination, departureDate, returnDate, adults, packageType);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexPackageSearchTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
