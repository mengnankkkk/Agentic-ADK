package com.alibaba.langengine.trawex;

import com.alibaba.langengine.core.callback.ExecutionContext;
import com.alibaba.langengine.core.tool.ToolExecuteResult;
import com.alibaba.langengine.trawex.tool.*;

/**
 * Trawex 工具使用示例
 * 展示如何使用 Trawex API 工具
 * 
 * @author AIDC-AI
 */
public class TrawexExamples {

    public static void main(String[] args) {
        // 示例1：搜索酒店
        searchHotelsExample();
        
        // 示例2：获取酒店详情
        // getHotelDetailsExample();
        
        // 示例3：搜索航班
        // searchFlightsExample();
        
        // 示例4：使用工厂创建工具
        // factoryExample();
    }

    /**
     * 示例1：搜索酒店
     */
    public static void searchHotelsExample() {
        System.out.println("=== 搜索酒店示例 ===");
        
        TrawexHotelSearchTool tool = new TrawexHotelSearchTool();
        
        String input = """
            {
              "destination": "Dubai",
              "check_in": "2025-12-01",
              "check_out": "2025-12-05",
              "adults": 2,
              "rooms": 1,
              "star_rating": 5,
              "max_price": 500
            }
            """;
        
        ExecutionContext context = new ExecutionContext();
        ToolExecuteResult result = tool.run(input, context);
        
        if (result.isError()) {
            System.out.println("错误: " + result.getOutput());
        } else {
            System.out.println("结果: " + result.getOutput());
        }
    }

    /**
     * 示例2：获取酒店详情
     */
    public static void getHotelDetailsExample() {
        System.out.println("\n=== 获取酒店详情示例 ===");
        
        TrawexHotelDetailsTool tool = new TrawexHotelDetailsTool();
        
        String input = """
            {
              "hotel_id": "hotel-12345",
              "check_in": "2025-12-01",
              "check_out": "2025-12-05",
              "adults": 2,
              "rooms": 1
            }
            """;
        
        ExecutionContext context = new ExecutionContext();
        ToolExecuteResult result = tool.run(input, context);
        
        if (result.isError()) {
            System.out.println("错误: " + result.getOutput());
        } else {
            System.out.println("结果: " + result.getOutput());
        }
    }

    /**
     * 示例3：搜索航班
     */
    public static void searchFlightsExample() {
        System.out.println("\n=== 搜索航班示例 ===");
        
        TrawexFlightSearchTool tool = new TrawexFlightSearchTool();
        
        // 单程航班
        String oneWayInput = """
            {
              "origin": "DXB",
              "destination": "JFK",
              "departure_date": "2025-12-01",
              "adults": 1,
              "cabin_class": "economy"
            }
            """;
        
        System.out.println("单程航班搜索:");
        ExecutionContext context = new ExecutionContext();
        ToolExecuteResult result = tool.run(oneWayInput, context);
        System.out.println("结果: " + result.getOutput());
        
        // 往返航班
        String roundTripInput = """
            {
              "origin": "DXB",
              "destination": "JFK",
              "departure_date": "2025-12-01",
              "return_date": "2025-12-10",
              "adults": 2,
              "cabin_class": "business"
            }
            """;
        
        System.out.println("\n往返航班搜索:");
        result = tool.run(roundTripInput, context);
        System.out.println("结果: " + result.getOutput());
    }

    /**
     * 示例4：使用工厂创建工具
     */
    public static void factoryExample() {
        System.out.println("\n=== 工厂模式示例 ===");
        
        // 创建工厂
        TrawexToolFactory factory = new TrawexToolFactory();
        
        // 方式1：获取所有工具
        System.out.println("所有工具数量: " + factory.getAllTools().size());
        
        // 方式2：获取特定类型的工具
        System.out.println("酒店相关工具数量: " + factory.getHotelTools().size());
        System.out.println("航班相关工具数量: " + factory.getFlightTools().size());
        
        // 方式3：根据名称获取工具
        var hotelSearch = factory.getToolByName("Trawex.search_hotels");
        System.out.println("通过名称获取的工具: " + hotelSearch.getName());
        
        // 方式4：直接创建特定工具
        TrawexHotelSearchTool hotelTool = factory.createHotelSearchTool();
        TrawexFlightSearchTool flightTool = factory.createFlightSearchTool();
        
        System.out.println("创建的工具:");
        System.out.println("  - " + hotelTool.getName());
        System.out.println("  - " + flightTool.getName());
    }

    /**
     * 示例5：与 Agent 集成（需要 LLM）
     */
    public static void agentIntegrationExample() {
        System.out.println("\n=== Agent 集成示例 ===");
        
        // 创建 Trawex 工具
        TrawexToolFactory factory = new TrawexToolFactory();
        
        // 获取所有工具
        var tools = factory.getAllTools();
        
        System.out.println("可用的工具:");
        tools.forEach(tool -> {
            System.out.println("  - " + tool.getName() + ": " + tool.getHumanName());
        });
        
        // 在实际使用中，这些工具会传递给 Agent
        // AgentExecutor agent = ToolLoaders.initializeStructuredAgent(tools, llm);
        // String result = agent.run("帮我找一下12月1号到5号在迪拜的五星级酒店");
    }

    /**
     * 示例6：错误处理
     */
    public static void errorHandlingExample() {
        System.out.println("\n=== 错误处理示例 ===");
        
        TrawexHotelSearchTool tool = new TrawexHotelSearchTool();
        
        // 缺少必需参数
        String invalidInput = """
            {
              "check_in": "2025-12-01",
              "check_out": "2025-12-05"
            }
            """;
        
        ExecutionContext context = new ExecutionContext();
        ToolExecuteResult result = tool.run(invalidInput, context);
        
        if (result.isError()) {
            System.out.println("预期的错误: " + result.getOutput());
        }
        
        // 无效的日期格式
        String invalidDateInput = """
            {
              "destination": "Dubai",
              "check_in": "invalid-date",
              "check_out": "2025-12-05"
            }
            """;
        
        result = tool.run(invalidDateInput, context);
        
        if (result.isError()) {
            System.out.println("日期格式错误: " + result.getOutput());
        }
    }
}
