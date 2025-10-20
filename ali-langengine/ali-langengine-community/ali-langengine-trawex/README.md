# Trawex Travel Technology Solutions Integration

Trawex 是一家领先的旅游技术解决方案提供商，提供全面的旅游预订系统和 API 服务。

## 功能特性

本集成提供以下 Trawex API 工具：

### 1. 酒店预订
- **TrawexHotelSearchTool**: 搜索酒店
- **TrawexHotelDetailsTool**: 获取酒店详情
- **TrawexHotelBookingTool**: 预订酒店

### 2. 航班预订
- **TrawexFlightSearchTool**: 搜索航班
- **TrawexFlightDetailsTool**: 获取航班详情
- **TrawexFlightBookingTool**: 预订航班

### 3. 旅游套餐
- **TrawexPackageSearchTool**: 搜索旅游套餐
- **TrawexPackageDetailsTool**: 获取套餐详情

### 4. 活动和景点
- **TrawexActivitySearchTool**: 搜索活动和景点
- **TrawexActivityBookingTool**: 预订活动

### 5. 汽车租赁
- **TrawexCarRentalSearchTool**: 搜索租车服务
- **TrawexCarRentalBookingTool**: 预订租车

## 配置

在 `application.properties` 或环境变量中配置以下参数：

```properties
# Trawex API 基础 URL
trawex.api.base.url=https://api.trawex.com/v1

# Trawex API 凭证
trawex.api.key=your_api_key_here
trawex.api.secret=your_api_secret_here

# 可选配置
trawex.request.timeout=30
trawex.default.language=en-US
trawex.default.currency=USD
trawex.use.sandbox=false
trawex.max.retries=3
trawex.retry.interval=1000
```

## 使用示例

### 基本用法

```java
// 创建配置
TrawexConfiguration config = new TrawexConfiguration();

// 创建工具工厂
TrawexToolFactory factory = new TrawexToolFactory(config);

// 获取所有工具
List<BaseTool> tools = factory.getAllTools();

// 或者获取特定工具
TrawexHotelSearchTool hotelSearch = factory.createHotelSearchTool();
TrawexFlightSearchTool flightSearch = factory.createFlightSearchTool();
```

### 搜索酒店

```java
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

ToolExecuteResult result = tool.run(input, null);
System.out.println(result.getOutput());
```

### 搜索航班

```java
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

ToolExecuteResult result = tool.run(input, null);
System.out.println(result.getOutput());
```

### 与 Agent 集成

```java
// 创建 LLM
ChatModelOpenAI model = new ChatModelOpenAI();
model.setModel(OpenAIModelConstants.GPT_4_TURBO);

// 创建 Trawex 工具
TrawexToolFactory factory = new TrawexToolFactory();
List<BaseTool> tools = factory.getAllTools();

// 初始化 Agent
AgentExecutor agentExecutor = ToolLoaders.initializeStructuredAgent(tools, model);

// 执行查询
String result = agentExecutor.run("找一下12月1号到5号在迪拜的五星级酒店");
System.out.println(result);
```

## API 支持

本集成支持以下 Trawex API 端点：

- Hotel Search API
- Hotel Details API
- Hotel Booking API
- Flight Search API
- Flight Details API
- Flight Booking API
- Package Search API
- Activity Search API
- Car Rental API

## 错误处理

所有工具都包含完善的错误处理机制：

- 参数验证
- API 错误响应处理
- 网络异常处理
- 自动重试机制

## 测试

运行测试：

```bash
mvn test
```

运行集成测试（需要真实 API 凭证）：

```bash
export TRAWEX_API_KEY=your_key
export TRAWEX_API_SECRET=your_secret
mvn test
```

## 许可证

Apache License 2.0

## 支持

如有问题，请访问 [Trawex 官方网站](https://www.trawex.com) 或联系技术支持。
