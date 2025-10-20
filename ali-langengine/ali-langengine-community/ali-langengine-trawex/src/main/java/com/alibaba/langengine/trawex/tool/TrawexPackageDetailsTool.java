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
 * Trawex 旅游套餐详情工具
 * 
 * @author AIDC-AI
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TrawexPackageDetailsTool extends BaseTool {
    
    private TrawexClient client;
    
    public TrawexPackageDetailsTool() {
        this(new TrawexClient());
    }
    
    public TrawexPackageDetailsTool(TrawexClient client) {
        this.client = client;
        setName("Trawex.get_package_details");
        setHumanName("Trawex套餐详情");
        setDescription("Get detailed information about a travel package. Parameters: package_id (required)");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> packageId = new HashMap<>();
        packageId.put("type", "string");
        packageId.put("description", "Package identifier from search results");
        properties.put("package_id", packageId);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"package_id"});
        
        setParameters(JSON.toJSONString(parameters));
    }
    
    @Override
    public ToolExecuteResult run(String toolInput, ExecutionContext executionContext) {
        try {
            log.info("TrawexPackageDetailsTool input: {}", toolInput);
            
            JSONObject input = JSON.parseObject(toolInput);
            String packageId = input.getString("package_id");
            
            if (packageId == null || packageId.trim().isEmpty()) {
                return new ToolExecuteResult("{\"error\": \"package_id is required\"}", true);
            }
            
            String response = client.getPackageDetails(packageId);
            
            return new ToolExecuteResult(response);
            
        } catch (Exception e) {
            log.error("TrawexPackageDetailsTool error", e);
            return new ToolExecuteResult("{\"error\": \"" + e.getMessage() + "\"}", true);
        }
    }
}
