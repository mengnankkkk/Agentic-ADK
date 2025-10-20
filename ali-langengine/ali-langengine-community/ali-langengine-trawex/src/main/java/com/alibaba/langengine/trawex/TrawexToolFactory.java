package com.alibaba.langengine.trawex;

import com.alibaba.langengine.core.tool.BaseTool;
import com.alibaba.langengine.trawex.service.TrawexClient;
import com.alibaba.langengine.trawex.tool.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Trawex 工具工厂
 * 用于创建和管理 Trawex 相关工具
 * 
 * @author AIDC-AI
 */
public class TrawexToolFactory {
    
    private final TrawexClient client;
    
    public TrawexToolFactory() {
        this.client = new TrawexClient();
    }
    
    public TrawexToolFactory(String apiKey, String apiSecret, String baseUrl) {
        this.client = new TrawexClient(apiKey, apiSecret, baseUrl);
    }
    
    public TrawexToolFactory(TrawexClient client) {
        this.client = client;
    }
    
    /**
     * 创建酒店搜索工具
     */
    public TrawexHotelSearchTool createHotelSearchTool() {
        return new TrawexHotelSearchTool(client);
    }
    
    /**
     * 创建酒店详情工具
     */
    public TrawexHotelDetailsTool createHotelDetailsTool() {
        return new TrawexHotelDetailsTool(client);
    }
    
    /**
     * 创建航班搜索工具
     */
    public TrawexFlightSearchTool createFlightSearchTool() {
        return new TrawexFlightSearchTool(client);
    }
    
    /**
     * 创建航班详情工具
     */
    public TrawexFlightDetailsTool createFlightDetailsTool() {
        return new TrawexFlightDetailsTool(client);
    }
    
    /**
     * 创建酒店预订工具
     */
    public TrawexHotelBookingTool createHotelBookingTool() {
        return new TrawexHotelBookingTool(client);
    }
    
    /**
     * 创建航班预订工具
     */
    public TrawexFlightBookingTool createFlightBookingTool() {
        return new TrawexFlightBookingTool(client);
    }
    
    /**
     * 创建套餐搜索工具
     */
    public TrawexPackageSearchTool createPackageSearchTool() {
        return new TrawexPackageSearchTool(client);
    }
    
    /**
     * 创建套餐详情工具
     */
    public TrawexPackageDetailsTool createPackageDetailsTool() {
        return new TrawexPackageDetailsTool(client);
    }
    
    /**
     * 创建活动搜索工具
     */
    public TrawexActivitySearchTool createActivitySearchTool() {
        return new TrawexActivitySearchTool(client);
    }
    
    /**
     * 创建活动预订工具
     */
    public TrawexActivityBookingTool createActivityBookingTool() {
        return new TrawexActivityBookingTool(client);
    }
    
    /**
     * 创建租车搜索工具
     */
    public TrawexCarRentalSearchTool createCarRentalSearchTool() {
        return new TrawexCarRentalSearchTool(client);
    }
    
    /**
     * 创建租车预订工具
     */
    public TrawexCarRentalBookingTool createCarRentalBookingTool() {
        return new TrawexCarRentalBookingTool(client);
    }
    
    /**
     * 获取所有酒店相关工具
     */
    public List<BaseTool> getHotelTools() {
        List<BaseTool> tools = new ArrayList<>();
        tools.add(createHotelSearchTool());
        tools.add(createHotelDetailsTool());
        tools.add(createHotelBookingTool());
        return tools;
    }
    
    /**
     * 获取所有航班相关工具
     */
    public List<BaseTool> getFlightTools() {
        List<BaseTool> tools = new ArrayList<>();
        tools.add(createFlightSearchTool());
        tools.add(createFlightDetailsTool());
        tools.add(createFlightBookingTool());
        return tools;
    }
    
    /**
     * 获取所有套餐相关工具
     */
    public List<BaseTool> getPackageTools() {
        List<BaseTool> tools = new ArrayList<>();
        tools.add(createPackageSearchTool());
        tools.add(createPackageDetailsTool());
        return tools;
    }
    
    /**
     * 获取所有活动相关工具
     */
    public List<BaseTool> getActivityTools() {
        List<BaseTool> tools = new ArrayList<>();
        tools.add(createActivitySearchTool());
        tools.add(createActivityBookingTool());
        return tools;
    }
    
    /**
     * 获取所有租车相关工具
     */
    public List<BaseTool> getCarRentalTools() {
        List<BaseTool> tools = new ArrayList<>();
        tools.add(createCarRentalSearchTool());
        tools.add(createCarRentalBookingTool());
        return tools;
    }
    
    /**
     * 获取所有工具
     */
    public List<BaseTool> getAllTools() {
        List<BaseTool> tools = new ArrayList<>();
        // 酒店工具
        tools.add(createHotelSearchTool());
        tools.add(createHotelDetailsTool());
        tools.add(createHotelBookingTool());
        // 航班工具
        tools.add(createFlightSearchTool());
        tools.add(createFlightDetailsTool());
        tools.add(createFlightBookingTool());
        // 套餐工具
        tools.add(createPackageSearchTool());
        tools.add(createPackageDetailsTool());
        // 活动工具
        tools.add(createActivitySearchTool());
        tools.add(createActivityBookingTool());
        // 租车工具
        tools.add(createCarRentalSearchTool());
        tools.add(createCarRentalBookingTool());
        return tools;
    }
    
    /**
     * 根据名称获取工具
     */
    public BaseTool getToolByName(String toolName) {
        switch (toolName) {
            case "Trawex.search_hotels":
                return createHotelSearchTool();
            case "Trawex.get_hotel_details":
                return createHotelDetailsTool();
            case "Trawex.book_hotel":
                return createHotelBookingTool();
            case "Trawex.search_flights":
                return createFlightSearchTool();
            case "Trawex.get_flight_details":
                return createFlightDetailsTool();
            case "Trawex.book_flight":
                return createFlightBookingTool();
            case "Trawex.search_packages":
                return createPackageSearchTool();
            case "Trawex.get_package_details":
                return createPackageDetailsTool();
            case "Trawex.search_activities":
                return createActivitySearchTool();
            case "Trawex.book_activity":
                return createActivityBookingTool();
            case "Trawex.search_car_rentals":
                return createCarRentalSearchTool();
            case "Trawex.book_car_rental":
                return createCarRentalBookingTool();
            default:
                throw new IllegalArgumentException("Unknown tool name: " + toolName);
        }
    }
}
