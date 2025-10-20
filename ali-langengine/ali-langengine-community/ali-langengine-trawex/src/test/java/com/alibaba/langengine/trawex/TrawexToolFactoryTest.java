package com.alibaba.langengine.trawex;

import com.alibaba.langengine.core.tool.BaseTool;
import com.alibaba.langengine.trawex.service.TrawexClient;
import com.alibaba.langengine.trawex.tool.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Trawex 工具工厂测试类
 * 
 * @author AIDC-AI
 */
@DisplayName("Trawex工具工厂测试")
class TrawexToolFactoryTest {

    private TrawexToolFactory factory;
    private TrawexClient mockClient;

    @BeforeEach
    void setUp() {
        // 使用真实客户端或 mock 客户端
        factory = new TrawexToolFactory();
    }

    @Nested
    @DisplayName("工具创建测试")
    class ToolCreationTests {

        @Test
        @DisplayName("测试创建酒店搜索工具")
        void testCreateHotelSearchTool() {
            TrawexHotelSearchTool tool = factory.createHotelSearchTool();
            
            assertNotNull(tool);
            assertNotNull(tool.getName());
            assertEquals("Trawex.search_hotels", tool.getName());
            assertNotNull(tool.getDescription());
            assertNotNull(tool.getParameters());
        }

        @Test
        @DisplayName("测试创建酒店详情工具")
        void testCreateHotelDetailsTool() {
            TrawexHotelDetailsTool tool = factory.createHotelDetailsTool();
            
            assertNotNull(tool);
            assertEquals("Trawex.get_hotel_details", tool.getName());
            assertNotNull(tool.getDescription());
        }

        @Test
        @DisplayName("测试创建航班搜索工具")
        void testCreateFlightSearchTool() {
            TrawexFlightSearchTool tool = factory.createFlightSearchTool();
            
            assertNotNull(tool);
            assertEquals("Trawex.search_flights", tool.getName());
            assertNotNull(tool.getDescription());
        }

        @Test
        @DisplayName("测试每次创建的工具实例不同")
        void testToolInstancesAreDifferent() {
            TrawexHotelSearchTool tool1 = factory.createHotelSearchTool();
            TrawexHotelSearchTool tool2 = factory.createHotelSearchTool();
            
            assertNotSame(tool1, tool2);
        }
    }

    @Nested
    @DisplayName("工具列表测试")
    class ToolListTests {

        @Test
        @DisplayName("测试获取所有酒店工具")
        void testGetHotelTools() {
            List<BaseTool> tools = factory.getHotelTools();
            
            assertNotNull(tools);
            assertEquals(2, tools.size());
            
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.search_hotels")));
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.get_hotel_details")));
        }

        @Test
        @DisplayName("测试获取所有航班工具")
        void testGetFlightTools() {
            List<BaseTool> tools = factory.getFlightTools();
            
            assertNotNull(tools);
            assertEquals(1, tools.size());
            
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.search_flights")));
        }

        @Test
        @DisplayName("测试获取所有工具")
        void testGetAllTools() {
            List<BaseTool> tools = factory.getAllTools();
            
            assertNotNull(tools);
            assertTrue(tools.size() >= 3);
            
            // 验证包含所有主要工具
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.search_hotels")));
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.get_hotel_details")));
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.search_flights")));
        }

        @Test
        @DisplayName("测试工具列表中没有重复")
        void testNoDuplicateTools() {
            List<BaseTool> tools = factory.getAllTools();
            
            long uniqueNames = tools.stream()
                .map(BaseTool::getName)
                .distinct()
                .count();
            
            assertEquals(tools.size(), uniqueNames);
        }
    }

    @Nested
    @DisplayName("工具名称查询测试")
    class ToolNameQueryTests {

        @Test
        @DisplayName("测试根据名称获取酒店搜索工具")
        void testGetToolByNameHotelSearch() {
            BaseTool tool = factory.getToolByName("Trawex.search_hotels");
            
            assertNotNull(tool);
            assertTrue(tool instanceof TrawexHotelSearchTool);
            assertEquals("Trawex.search_hotels", tool.getName());
        }

        @Test
        @DisplayName("测试根据名称获取酒店详情工具")
        void testGetToolByNameHotelDetails() {
            BaseTool tool = factory.getToolByName("Trawex.get_hotel_details");
            
            assertNotNull(tool);
            assertTrue(tool instanceof TrawexHotelDetailsTool);
        }

        @Test
        @DisplayName("测试根据名称获取航班搜索工具")
        void testGetToolByNameFlightSearch() {
            BaseTool tool = factory.getToolByName("Trawex.search_flights");
            
            assertNotNull(tool);
            assertTrue(tool instanceof TrawexFlightSearchTool);
        }

        @Test
        @DisplayName("测试获取不存在的工具名称")
        void testGetToolByNameNotExists() {
            assertThrows(IllegalArgumentException.class, () -> {
                factory.getToolByName("NonExistent.tool");
            });
        }

        @Test
        @DisplayName("测试获取空工具名称")
        void testGetToolByNameEmpty() {
            assertThrows(IllegalArgumentException.class, () -> {
                factory.getToolByName("");
            });
        }

        @Test
        @DisplayName("测试获取null工具名称")
        void testGetToolByNameNull() {
            assertThrows(Exception.class, () -> {
                factory.getToolByName(null);
            });
        }
    }

    @Nested
    @DisplayName("工具实例类型测试")
    class ToolInstanceTypeTests {

        @Test
        @DisplayName("测试所有工具都是BaseTool的实例")
        void testAllToolsAreBaseTools() {
            List<BaseTool> tools = factory.getAllTools();
            
            for (BaseTool tool : tools) {
                assertTrue(tool instanceof BaseTool);
            }
        }

        @Test
        @DisplayName("测试工具具有正确的类型")
        void testToolsHaveCorrectTypes() {
            TrawexHotelSearchTool hotelSearch = factory.createHotelSearchTool();
            TrawexHotelDetailsTool hotelDetails = factory.createHotelDetailsTool();
            TrawexFlightSearchTool flightSearch = factory.createFlightSearchTool();
            
            assertTrue(hotelSearch instanceof TrawexHotelSearchTool);
            assertTrue(hotelDetails instanceof TrawexHotelDetailsTool);
            assertTrue(flightSearch instanceof TrawexFlightSearchTool);
        }
    }

    @Nested
    @DisplayName("工具属性验证测试")
    class ToolAttributeValidationTests {

        @Test
        @DisplayName("测试所有工具都有名称")
        void testAllToolsHaveNames() {
            List<BaseTool> tools = factory.getAllTools();
            
            for (BaseTool tool : tools) {
                assertNotNull(tool.getName());
                assertFalse(tool.getName().isEmpty());
            }
        }

        @Test
        @DisplayName("测试所有工具都有描述")
        void testAllToolsHaveDescriptions() {
            List<BaseTool> tools = factory.getAllTools();
            
            for (BaseTool tool : tools) {
                assertNotNull(tool.getDescription());
                assertFalse(tool.getDescription().isEmpty());
            }
        }

        @Test
        @DisplayName("测试所有工具都有参数定义")
        void testAllToolsHaveParameters() {
            List<BaseTool> tools = factory.getAllTools();
            
            for (BaseTool tool : tools) {
                assertNotNull(tool.getParameters());
                assertFalse(tool.getParameters().isEmpty());
            }
        }

        @Test
        @DisplayName("测试工具名称符合命名规范")
        void testToolNamesFollowConvention() {
            List<BaseTool> tools = factory.getAllTools();
            
            for (BaseTool tool : tools) {
                assertTrue(tool.getName().startsWith("Trawex."),
                    "Tool name should start with 'Trawex.': " + tool.getName());
            }
        }
    }

    @Nested
    @DisplayName("工厂配置测试")
    class FactoryConfigurationTests {

        @Test
        @DisplayName("测试使用默认配置创建工厂")
        void testFactoryWithDefaultConfiguration() {
            TrawexToolFactory defaultFactory = new TrawexToolFactory();
            
            assertNotNull(defaultFactory);
            assertNotNull(defaultFactory.getAllTools());
        }

        @Test
        @DisplayName("测试使用自定义客户端创建工厂")
        void testFactoryWithCustomClient() {
            TrawexClient customClient = new TrawexClient("test-key", "test-secret", "https://test.api");
            TrawexToolFactory customFactory = new TrawexToolFactory(customClient);
            
            assertNotNull(customFactory);
            assertNotNull(customFactory.getAllTools());
        }

        @Test
        @DisplayName("测试使用API凭证创建工厂")
        void testFactoryWithApiCredentials() {
            TrawexToolFactory credFactory = new TrawexToolFactory(
                "test-api-key", "test-api-secret", "https://api.test.com");
            
            assertNotNull(credFactory);
            assertNotNull(credFactory.getAllTools());
        }
    }
}
