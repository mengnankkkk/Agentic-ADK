package com.alibaba.langengine.trawex;

import com.alibaba.langengine.core.callback.ExecutionContext;
import com.alibaba.langengine.core.tool.BaseTool;
import com.alibaba.langengine.core.tool.ToolExecuteResult;
import com.alibaba.langengine.trawex.service.TrawexClient;
import com.alibaba.langengine.trawex.service.TrawexException;
import com.alibaba.langengine.trawex.tool.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Trawex 工具测试类
 * 包含单元测试和集成测试
 * 
 * @author AIDC-AI
 */
@DisplayName("Trawex工具测试")
class TrawexToolsTest {

    @Mock
    private TrawexClient mockClient;
    
    private ExecutionContext context;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        context = new ExecutionContext();
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Nested
    @DisplayName("酒店搜索工具测试")
    class HotelSearchToolTests {

        @Test
        @DisplayName("测试有效输入")
        void testValidInput() {
            when(mockClient.searchHotels(anyString(), anyString(), anyString(), 
                anyInt(), anyInt(), any(), any()))
                .thenReturn("{\"hotels\": [], \"total\": 0}");
            
            TrawexHotelSearchTool tool = new TrawexHotelSearchTool(mockClient);
            
            String input = """
                {
                  "destination": "Dubai",
                  "check_in": "2025-12-01",
                  "check_out": "2025-12-05",
                  "adults": 2,
                  "rooms": 1,
                  "star_rating": 5
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
            verify(mockClient, times(1)).searchHotels(
                eq("Dubai"), eq("2025-12-01"), eq("2025-12-05"), 
                eq(2), eq(1), eq(5), isNull());
        }

        @Test
        @DisplayName("测试缺少必需参数destination")
        void testMissingDestination() {
            TrawexHotelSearchTool tool = new TrawexHotelSearchTool(mockClient);
            
            String input = """
                {
                  "check_in": "2025-12-01",
                  "check_out": "2025-12-05"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("destination is required"));
        }

        @Test
        @DisplayName("测试缺少check_in参数")
        void testMissingCheckIn() {
            TrawexHotelSearchTool tool = new TrawexHotelSearchTool(mockClient);
            
            String input = """
                {
                  "destination": "Paris",
                  "check_out": "2025-12-05"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("check_in"));
        }

        @Test
        @DisplayName("测试缺少check_out参数")
        void testMissingCheckOut() {
            TrawexHotelSearchTool tool = new TrawexHotelSearchTool(mockClient);
            
            String input = """
                {
                  "destination": "London",
                  "check_in": "2025-12-01"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("check_out"));
        }

        @Test
        @DisplayName("测试可选参数")
        void testOptionalParameters() {
            when(mockClient.searchHotels(anyString(), anyString(), anyString(), 
                any(), any(), any(), any()))
                .thenReturn("{\"hotels\": [], \"total\": 0}");
            
            TrawexHotelSearchTool tool = new TrawexHotelSearchTool(mockClient);
            
            String input = """
                {
                  "destination": "Tokyo",
                  "check_in": "2025-12-01",
                  "check_out": "2025-12-05",
                  "max_price": 500.0
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertFalse(result.isError());
            verify(mockClient).searchHotels(
                eq("Tokyo"), eq("2025-12-01"), eq("2025-12-05"), 
                isNull(), isNull(), isNull(), eq(500.0));
        }

        @Test
        @DisplayName("测试API异常处理")
        void testApiException() {
            when(mockClient.searchHotels(anyString(), anyString(), anyString(), 
                any(), any(), any(), any()))
                .thenThrow(new TrawexException("API Error"));
            
            TrawexHotelSearchTool tool = new TrawexHotelSearchTool(mockClient);
            
            String input = """
                {
                  "destination": "Berlin",
                  "check_in": "2025-12-01",
                  "check_out": "2025-12-05"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("API Error"));
        }
    }

    @Nested
    @DisplayName("酒店详情工具测试")
    class HotelDetailsToolTests {

        @Test
        @DisplayName("测试有效输入")
        void testValidInput() {
            when(mockClient.getHotelDetails(anyString(), anyString(), anyString(), 
                any(), any()))
                .thenReturn("{\"hotel_id\": \"hotel-123\", \"name\": \"Test Hotel\"}");
            
            TrawexHotelDetailsTool tool = new TrawexHotelDetailsTool(mockClient);
            
            String input = """
                {
                  "hotel_id": "hotel-123",
                  "check_in": "2025-12-01",
                  "check_out": "2025-12-05",
                  "adults": 2,
                  "rooms": 1
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
            verify(mockClient).getHotelDetails("hotel-123", "2025-12-01", "2025-12-05", 2, 1);
        }

        @Test
        @DisplayName("测试缺少hotel_id")
        void testMissingHotelId() {
            TrawexHotelDetailsTool tool = new TrawexHotelDetailsTool(mockClient);
            
            String input = """
                {
                  "check_in": "2025-12-01",
                  "check_out": "2025-12-05"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("hotel_id is required"));
        }
    }

    @Nested
    @DisplayName("航班搜索工具测试")
    class FlightSearchToolTests {

        @Test
        @DisplayName("测试单程航班搜索")
        void testOneWayFlight() {
            when(mockClient.searchFlights(anyString(), anyString(), anyString(), 
                any(), any(), any()))
                .thenReturn("{\"flights\": [], \"total\": 0}");
            
            TrawexFlightSearchTool tool = new TrawexFlightSearchTool(mockClient);
            
            String input = """
                {
                  "origin": "JFK",
                  "destination": "LAX",
                  "departure_date": "2025-12-01",
                  "adults": 1,
                  "cabin_class": "economy"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
            verify(mockClient).searchFlights("JFK", "LAX", "2025-12-01", 
                null, 1, "economy");
        }

        @Test
        @DisplayName("测试往返航班搜索")
        void testRoundTripFlight() {
            when(mockClient.searchFlights(anyString(), anyString(), anyString(), 
                anyString(), any(), any()))
                .thenReturn("{\"flights\": [], \"total\": 0}");
            
            TrawexFlightSearchTool tool = new TrawexFlightSearchTool(mockClient);
            
            String input = """
                {
                  "origin": "DXB",
                  "destination": "LHR",
                  "departure_date": "2025-12-01",
                  "return_date": "2025-12-10",
                  "adults": 2,
                  "cabin_class": "business"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertFalse(result.isError());
            verify(mockClient).searchFlights("DXB", "LHR", "2025-12-01", 
                "2025-12-10", 2, "business");
        }

        @Test
        @DisplayName("测试缺少origin参数")
        void testMissingOrigin() {
            TrawexFlightSearchTool tool = new TrawexFlightSearchTool(mockClient);
            
            String input = """
                {
                  "destination": "LAX",
                  "departure_date": "2025-12-01"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("origin"));
        }

        @Test
        @DisplayName("测试缺少destination参数")
        void testMissingDestination() {
            TrawexFlightSearchTool tool = new TrawexFlightSearchTool(mockClient);
            
            String input = """
                {
                  "origin": "JFK",
                  "departure_date": "2025-12-01"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("destination"));
        }
    }

    @Nested
    @DisplayName("航班详情工具测试")
    class FlightDetailsToolTests {

        @Test
        @DisplayName("测试有效输入")
        void testValidInput() {
            when(mockClient.getFlightDetails(anyString()))
                .thenReturn("{\"flight_id\": \"FL-123\", \"airline\": \"Emirates\"}");
            
            TrawexFlightDetailsTool tool = new TrawexFlightDetailsTool(mockClient);
            
            String input = """
                {
                  "flight_id": "FL-123"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
            verify(mockClient).getFlightDetails("FL-123");
        }

        @Test
        @DisplayName("测试缺少flight_id参数")
        void testMissingFlightId() {
            TrawexFlightDetailsTool tool = new TrawexFlightDetailsTool(mockClient);
            
            String input = "{}";
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("flight_id is required"));
        }
    }

    @Nested
    @DisplayName("酒店预订工具测试")
    class HotelBookingToolTests {

        @Test
        @DisplayName("测试有效预订")
        void testValidBooking() {
            when(mockClient.bookHotel(anyString(), anyString(), anyString(), 
                any(), any()))
                .thenReturn("{\"booking_id\": \"BK-123\", \"status\": \"confirmed\"}");
            
            TrawexHotelBookingTool tool = new TrawexHotelBookingTool(mockClient);
            
            String input = """
                {
                  "hotel_id": "hotel-123",
                  "check_in": "2025-12-01",
                  "check_out": "2025-12-05",
                  "guest_info": {
                    "name": "John Doe",
                    "email": "john@example.com",
                    "phone": "+1234567890"
                  },
                  "payment_info": {
                    "card_number": "****1234",
                    "cvv": "***"
                  }
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
        }

        @Test
        @DisplayName("测试缺少guest_info")
        void testMissingGuestInfo() {
            TrawexHotelBookingTool tool = new TrawexHotelBookingTool(mockClient);
            
            String input = """
                {
                  "hotel_id": "hotel-123",
                  "check_in": "2025-12-01",
                  "check_out": "2025-12-05",
                  "payment_info": {}
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("guest_info is required"));
        }
    }

    @Nested
    @DisplayName("航班预订工具测试")
    class FlightBookingToolTests {

        @Test
        @DisplayName("测试有效预订")
        void testValidBooking() {
            when(mockClient.bookFlight(anyString(), any(), any()))
                .thenReturn("{\"booking_id\": \"FB-123\", \"status\": \"confirmed\"}");
            
            TrawexFlightBookingTool tool = new TrawexFlightBookingTool(mockClient);
            
            String input = """
                {
                  "flight_id": "FL-123",
                  "passenger_info": {
                    "name": "Jane Smith",
                    "passport": "P12345678",
                    "dob": "1990-01-01"
                  },
                  "payment_info": {
                    "card_number": "****5678"
                  }
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
        }

        @Test
        @DisplayName("测试缺少passenger_info")
        void testMissingPassengerInfo() {
            TrawexFlightBookingTool tool = new TrawexFlightBookingTool(mockClient);
            
            String input = """
                {
                  "flight_id": "FL-123",
                  "payment_info": {}
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("passenger_info is required"));
        }
    }

    @Nested
    @DisplayName("套餐搜索工具测试")
    class PackageSearchToolTests {

        @Test
        @DisplayName("测试有效搜索")
        void testValidSearch() {
            when(mockClient.searchPackages(anyString(), anyString(), anyString(), 
                any(), any()))
                .thenReturn("{\"packages\": [], \"total\": 0}");
            
            TrawexPackageSearchTool tool = new TrawexPackageSearchTool(mockClient);
            
            String input = """
                {
                  "destination": "Maldives",
                  "departure_date": "2025-12-01",
                  "return_date": "2025-12-07",
                  "adults": 2,
                  "package_type": "all-inclusive"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
            verify(mockClient).searchPackages("Maldives", "2025-12-01", "2025-12-07", 
                2, "all-inclusive");
        }

        @Test
        @DisplayName("测试缺少destination")
        void testMissingDestination() {
            TrawexPackageSearchTool tool = new TrawexPackageSearchTool(mockClient);
            
            String input = """
                {
                  "departure_date": "2025-12-01",
                  "return_date": "2025-12-07"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("destination is required"));
        }
    }

    @Nested
    @DisplayName("套餐详情工具测试")
    class PackageDetailsToolTests {

        @Test
        @DisplayName("测试有效输入")
        void testValidInput() {
            when(mockClient.getPackageDetails(anyString()))
                .thenReturn("{\"package_id\": \"PKG-123\", \"name\": \"Luxury Package\"}");
            
            TrawexPackageDetailsTool tool = new TrawexPackageDetailsTool(mockClient);
            
            String input = """
                {
                  "package_id": "PKG-123"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
            verify(mockClient).getPackageDetails("PKG-123");
        }

        @Test
        @DisplayName("测试缺少package_id")
        void testMissingPackageId() {
            TrawexPackageDetailsTool tool = new TrawexPackageDetailsTool(mockClient);
            
            String input = "{}";
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("package_id is required"));
        }
    }

    @Nested
    @DisplayName("活动搜索工具测试")
    class ActivitySearchToolTests {

        @Test
        @DisplayName("测试有效搜索")
        void testValidSearch() {
            when(mockClient.searchActivities(anyString(), any(), any()))
                .thenReturn("{\"activities\": [], \"total\": 0}");
            
            TrawexActivitySearchTool tool = new TrawexActivitySearchTool(mockClient);
            
            String input = """
                {
                  "destination": "Paris",
                  "date": "2025-12-01",
                  "category": "cultural"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
            verify(mockClient).searchActivities("Paris", "2025-12-01", "cultural");
        }

        @Test
        @DisplayName("测试仅必需参数")
        void testOnlyRequiredParameters() {
            when(mockClient.searchActivities(anyString(), any(), any()))
                .thenReturn("{\"activities\": [], \"total\": 0}");
            
            TrawexActivitySearchTool tool = new TrawexActivitySearchTool(mockClient);
            
            String input = """
                {
                  "destination": "Rome"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertFalse(result.isError());
            verify(mockClient).searchActivities("Rome", null, null);
        }

        @Test
        @DisplayName("测试缺少destination")
        void testMissingDestination() {
            TrawexActivitySearchTool tool = new TrawexActivitySearchTool(mockClient);
            
            String input = """
                {
                  "date": "2025-12-01"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("destination is required"));
        }
    }

    @Nested
    @DisplayName("活动预订工具测试")
    class ActivityBookingToolTests {

        @Test
        @DisplayName("测试有效预订")
        void testValidBooking() {
            when(mockClient.bookActivity(anyString(), anyString(), anyInt(), 
                any(), any()))
                .thenReturn("{\"booking_id\": \"ACT-123\", \"status\": \"confirmed\"}");
            
            TrawexActivityBookingTool tool = new TrawexActivityBookingTool(mockClient);
            
            String input = """
                {
                  "activity_id": "ACT-456",
                  "date": "2025-12-01",
                  "participants": 4,
                  "participant_info": {
                    "lead_name": "Alice Brown",
                    "email": "alice@example.com"
                  },
                  "payment_info": {
                    "method": "credit_card"
                  }
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
        }

        @Test
        @DisplayName("测试无效参与人数")
        void testInvalidParticipants() {
            TrawexActivityBookingTool tool = new TrawexActivityBookingTool(mockClient);
            
            String input = """
                {
                  "activity_id": "ACT-456",
                  "date": "2025-12-01",
                  "participants": 0,
                  "participant_info": {},
                  "payment_info": {}
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("valid participants count is required"));
        }
    }

    @Nested
    @DisplayName("租车搜索工具测试")
    class CarRentalSearchToolTests {

        @Test
        @DisplayName("测试有效搜索")
        void testValidSearch() {
            when(mockClient.searchCarRentals(anyString(), anyString(), anyString(), 
                any(), any()))
                .thenReturn("{\"cars\": [], \"total\": 0}");
            
            TrawexCarRentalSearchTool tool = new TrawexCarRentalSearchTool(mockClient);
            
            String input = """
                {
                  "pickup_location": "JFK Airport",
                  "pickup_date": "2025-12-01 10:00",
                  "dropoff_date": "2025-12-05 10:00",
                  "dropoff_location": "LAX Airport",
                  "car_type": "suv"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
            verify(mockClient).searchCarRentals("JFK Airport", "2025-12-01 10:00", 
                "2025-12-05 10:00", "LAX Airport", "suv");
        }

        @Test
        @DisplayName("测试缺少pickup_location")
        void testMissingPickupLocation() {
            TrawexCarRentalSearchTool tool = new TrawexCarRentalSearchTool(mockClient);
            
            String input = """
                {
                  "pickup_date": "2025-12-01 10:00",
                  "dropoff_date": "2025-12-05 10:00"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("pickup_location is required"));
        }
    }

    @Nested
    @DisplayName("租车预订工具测试")
    class CarRentalBookingToolTests {

        @Test
        @DisplayName("测试有效预订")
        void testValidBooking() {
            when(mockClient.bookCarRental(anyString(), any(), any()))
                .thenReturn("{\"booking_id\": \"CAR-789\", \"status\": \"confirmed\"}");
            
            TrawexCarRentalBookingTool tool = new TrawexCarRentalBookingTool(mockClient);
            
            String input = """
                {
                  "car_id": "CAR-001",
                  "driver_info": {
                    "name": "Bob Wilson",
                    "license": "DL123456",
                    "phone": "+1234567890"
                  },
                  "payment_info": {
                    "card_number": "****9999"
                  }
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            assertFalse(result.isError());
        }

        @Test
        @DisplayName("测试缺少driver_info")
        void testMissingDriverInfo() {
            TrawexCarRentalBookingTool tool = new TrawexCarRentalBookingTool(mockClient);
            
            String input = """
                {
                  "car_id": "CAR-001",
                  "payment_info": {}
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertTrue(result.isError());
            assertTrue(result.getOutput().toString().contains("driver_info is required"));
        }
    }

    @Nested
    @DisplayName("工具工厂测试")
    class ToolFactoryTests {

        @Test
        @DisplayName("测试创建酒店搜索工具")
        void testCreateHotelSearchTool() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            TrawexHotelSearchTool tool = factory.createHotelSearchTool();
            
            assertNotNull(tool);
            assertEquals("Trawex.search_hotels", tool.getName());
        }

        @Test
        @DisplayName("测试创建酒店详情工具")
        void testCreateHotelDetailsTool() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            TrawexHotelDetailsTool tool = factory.createHotelDetailsTool();
            
            assertNotNull(tool);
            assertEquals("Trawex.get_hotel_details", tool.getName());
        }

        @Test
        @DisplayName("测试创建航班搜索工具")
        void testCreateFlightSearchTool() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            TrawexFlightSearchTool tool = factory.createFlightSearchTool();
            
            assertNotNull(tool);
            assertEquals("Trawex.search_flights", tool.getName());
        }

        @Test
        @DisplayName("测试获取所有酒店工具")
        void testGetHotelTools() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            List<BaseTool> tools = factory.getHotelTools();
            
            assertNotNull(tools);
            assertEquals(3, tools.size());
        }

        @Test
        @DisplayName("测试获取所有航班工具")
        void testGetFlightTools() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            List<BaseTool> tools = factory.getFlightTools();
            
            assertNotNull(tools);
            assertEquals(3, tools.size());
        }

        @Test
        @DisplayName("测试获取所有套餐工具")
        void testGetPackageTools() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            List<BaseTool> tools = factory.getPackageTools();
            
            assertNotNull(tools);
            assertEquals(2, tools.size());
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.search_packages")));
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.get_package_details")));
        }

        @Test
        @DisplayName("测试获取所有活动工具")
        void testGetActivityTools() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            List<BaseTool> tools = factory.getActivityTools();
            
            assertNotNull(tools);
            assertEquals(2, tools.size());
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.search_activities")));
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.book_activity")));
        }

        @Test
        @DisplayName("测试获取所有租车工具")
        void testGetCarRentalTools() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            List<BaseTool> tools = factory.getCarRentalTools();
            
            assertNotNull(tools);
            assertEquals(2, tools.size());
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.search_car_rentals")));
            assertTrue(tools.stream().anyMatch(t -> t.getName().equals("Trawex.book_car_rental")));
        }

        @Test
        @DisplayName("测试获取所有工具")
        void testGetAllTools() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            List<BaseTool> tools = factory.getAllTools();
            
            assertNotNull(tools);
            assertEquals(12, tools.size());
        }

        @Test
        @DisplayName("测试根据名称获取工具")
        void testGetToolByName() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            
            BaseTool hotelSearch = factory.getToolByName("Trawex.search_hotels");
            assertNotNull(hotelSearch);
            assertTrue(hotelSearch instanceof TrawexHotelSearchTool);
            
            BaseTool hotelDetails = factory.getToolByName("Trawex.get_hotel_details");
            assertNotNull(hotelDetails);
            assertTrue(hotelDetails instanceof TrawexHotelDetailsTool);
            
            BaseTool flightSearch = factory.getToolByName("Trawex.search_flights");
            assertNotNull(flightSearch);
            assertTrue(flightSearch instanceof TrawexFlightSearchTool);
            
            BaseTool packageSearch = factory.getToolByName("Trawex.search_packages");
            assertNotNull(packageSearch);
            assertTrue(packageSearch instanceof TrawexPackageSearchTool);
        }

        @Test
        @DisplayName("测试获取未知工具名称抛出异常")
        void testGetToolByNameThrowsException() {
            TrawexToolFactory factory = new TrawexToolFactory(mockClient);
            
            assertThrows(IllegalArgumentException.class, () -> {
                factory.getToolByName("Unknown.tool");
            });
        }
    }

    @Nested
    @DisplayName("工具参数验证测试")
    class ToolParameterTests {

        @Test
        @DisplayName("测试酒店搜索工具参数结构")
        void testHotelSearchToolParameters() {
            TrawexHotelSearchTool tool = new TrawexHotelSearchTool(mockClient);
            
            assertNotNull(tool.getParameters());
            assertEquals("Trawex.search_hotels", tool.getName());
            assertNotNull(tool.getDescription());
            assertTrue(tool.getDescription().contains("destination"));
            assertTrue(tool.getDescription().contains("check_in"));
            assertTrue(tool.getDescription().contains("check_out"));
        }

        @Test
        @DisplayName("测试航班搜索工具参数结构")
        void testFlightSearchToolParameters() {
            TrawexFlightSearchTool tool = new TrawexFlightSearchTool(mockClient);
            
            assertNotNull(tool.getParameters());
            assertEquals("Trawex.search_flights", tool.getName());
            assertNotNull(tool.getDescription());
            assertTrue(tool.getDescription().contains("origin"));
            assertTrue(tool.getDescription().contains("destination"));
        }
    }

    /**
     * 集成测试 - 需要真实的 API 凭证
     * 使用环境变量 TRAWEX_API_KEY 和 TRAWEX_API_SECRET 控制是否运行
     */
    @Nested
    @DisplayName("集成测试")
    class IntegrationTests {

        @Test
        @EnabledIfEnvironmentVariable(named = "TRAWEX_API_KEY", matches = ".+")
        @DisplayName("集成测试 - 搜索酒店")
        void integrationTestSearchHotels() {
            TrawexHotelSearchTool tool = new TrawexHotelSearchTool();
            
            String input = """
                {
                  "destination": "Dubai",
                  "check_in": "2025-12-01",
                  "check_out": "2025-12-05",
                  "adults": 2,
                  "rooms": 1,
                  "star_rating": 5
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            System.out.println("Hotel Search Result: " + result.getOutput());
        }

        @Test
        @EnabledIfEnvironmentVariable(named = "TRAWEX_API_KEY", matches = ".+")
        @DisplayName("集成测试 - 搜索航班")
        void integrationTestSearchFlights() {
            TrawexFlightSearchTool tool = new TrawexFlightSearchTool();
            
            String input = """
                {
                  "origin": "DXB",
                  "destination": "JFK",
                  "departure_date": "2025-12-01",
                  "return_date": "2025-12-10",
                  "adults": 1,
                  "cabin_class": "economy"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            System.out.println("Flight Search Result: " + result.getOutput());
        }

        @Test
        @EnabledIfEnvironmentVariable(named = "TRAWEX_API_KEY", matches = ".+")
        @DisplayName("集成测试 - 搜索套餐")
        void integrationTestSearchPackages() {
            TrawexPackageSearchTool tool = new TrawexPackageSearchTool();
            
            String input = """
                {
                  "destination": "Bali",
                  "departure_date": "2025-12-01",
                  "return_date": "2025-12-07",
                  "adults": 2,
                  "package_type": "honeymoon"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            System.out.println("Package Search Result: " + result.getOutput());
        }

        @Test
        @EnabledIfEnvironmentVariable(named = "TRAWEX_API_KEY", matches = ".+")
        @DisplayName("集成测试 - 搜索活动")
        void integrationTestSearchActivities() {
            TrawexActivitySearchTool tool = new TrawexActivitySearchTool();
            
            String input = """
                {
                  "destination": "Paris",
                  "date": "2025-12-01",
                  "category": "museums"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            System.out.println("Activity Search Result: " + result.getOutput());
        }

        @Test
        @EnabledIfEnvironmentVariable(named = "TRAWEX_API_KEY", matches = ".+")
        @DisplayName("集成测试 - 搜索租车")
        void integrationTestSearchCarRentals() {
            TrawexCarRentalSearchTool tool = new TrawexCarRentalSearchTool();
            
            String input = """
                {
                  "pickup_location": "LAX Airport",
                  "pickup_date": "2025-12-01 10:00",
                  "dropoff_date": "2025-12-05 10:00",
                  "car_type": "economy"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            System.out.println("Car Rental Search Result: " + result.getOutput());
        }

        @Test
        @EnabledIfEnvironmentVariable(named = "TRAWEX_API_KEY", matches = ".+")
        @DisplayName("集成测试 - 获取航班详情")
        void integrationTestGetFlightDetails() {
            TrawexFlightDetailsTool tool = new TrawexFlightDetailsTool();
            
            String input = """
                {
                  "flight_id": "FL-MOCK-123"
                }
                """;
            
            ToolExecuteResult result = tool.run(input, context);
            
            assertNotNull(result);
            System.out.println("Flight Details Result: " + result.getOutput());
        }
    }
}
