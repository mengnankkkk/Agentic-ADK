# NewsAPI Integration for Ali-LangEngine

This module provides NewsAPI tool calling integration for Ali-LangEngine, allowing AI agents to fetch and search news articles from thousands of sources worldwide.

## Features

- **Top Headlines**: Get breaking news headlines by country, category, or search query
- **Everything Search**: Search through millions of articles with advanced filtering options
- **Tool Calling Support**: Full integration with Ali-LangEngine's tool calling framework
- **Flexible Filtering**: Filter by date range, sort order, country, and category
- **Rich Results**: Access article titles, descriptions, sources, authors, and URLs

## Prerequisites

1. **NewsAPI Key**: Get your free API key from [newsapi.org](https://newsapi.org/)
2. **Java 17+**: Required for building and running
3. **Maven**: For dependency management

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>ali-langengine-newsapi</artifactId>
    <version>1.2.6-202508111516</version>
</dependency>
```

## Configuration

Set your NewsAPI key as an environment variable:

**Windows (PowerShell):**
```powershell
$env:NEWS_API_KEY="your_api_key_here"
```

**Windows (CMD):**
```cmd
set NEWS_API_KEY=your_api_key_here
```

**Linux/Mac:**
```bash
export NEWS_API_KEY=your_api_key_here
```

Alternatively, pass the API key directly to the tool constructor:

```java
NewsApiSearchTool tool = new NewsApiSearchTool("your_api_key_here");
```

## Usage

### 1. Top Headlines Tool

Get breaking news headlines by country, category, or search query.

```java
import com.alibaba.langengine.newsapi.tool.NewsApiTopHeadlinesTool;
import com.alibaba.langengine.core.tool.ToolExecuteResult;

// Create the tool
NewsApiTopHeadlinesTool topHeadlinesTool = new NewsApiTopHeadlinesTool();

// Get top headlines from US
String input = "{\"country\": \"us\", \"pageSize\": 5}";
ToolExecuteResult result = topHeadlinesTool.run(input);
System.out.println(result.getOutput());

// Get technology news
String techInput = "{\"country\": \"us\", \"category\": \"technology\", \"pageSize\": 5}";
ToolExecuteResult techResult = topHeadlinesTool.run(techInput);
System.out.println(techResult.getOutput());

// Search headlines by keyword
String searchInput = "{\"query\": \"artificial intelligence\", \"pageSize\": 5}";
ToolExecuteResult searchResult = topHeadlinesTool.run(searchInput);
System.out.println(searchResult.getOutput());
```

**Supported Parameters:**
- `country` (optional): 2-letter ISO 3166-1 country code (e.g., "us", "cn", "jp", "gb")
- `category` (optional): business, entertainment, general, health, science, sports, technology
- `query` (optional): Keywords to search for in headlines
- `pageSize` (optional): Number of results (1-100, default 20)
- `page` (optional): Page number for pagination (default 1)

**Note:** At least one of `country`, `category`, or `query` must be provided.

### 2. Search Everything Tool

Search all articles with advanced filtering options.

```java
import com.alibaba.langengine.newsapi.tool.NewsApiSearchTool;
import com.alibaba.langengine.core.tool.ToolExecuteResult;

// Create the tool
NewsApiSearchTool searchTool = new NewsApiSearchTool();

// Basic search
String input = "{\"query\": \"climate change\", \"pageSize\": 5}";
ToolExecuteResult result = searchTool.run(input);
System.out.println(result.getOutput());

// Search with date range
String dateRangeInput = "{\"query\": \"AI\", \"from\": \"2024-01-01\", \"to\": \"2024-12-31\", \"pageSize\": 5}";
ToolExecuteResult dateResult = searchTool.run(dateRangeInput);
System.out.println(dateResult.getOutput());

// Search with sorting
String sortedInput = "{\"query\": \"technology\", \"sortBy\": \"popularity\", \"pageSize\": 5}";
ToolExecuteResult sortedResult = searchTool.run(sortedInput);
System.out.println(sortedResult.getOutput());

// Complex query with operators
String complexInput = "{\"query\": \"\\\"machine learning\\\" OR \\\"deep learning\\\"\", \"pageSize\": 5}";
ToolExecuteResult complexResult = searchTool.run(complexInput);
System.out.println(complexResult.getOutput());
```

**Supported Parameters:**
- `query` (required): Keywords or phrases to search for. Supports:
  - Phrase search: `"exact phrase"`
  - AND operator: `word1 AND word2`
  - OR operator: `word1 OR word2`
  - NOT operator: `word1 NOT word2`
- `from` (optional): Start date in `yyyy-MM-dd` format
- `to` (optional): End date in `yyyy-MM-dd` format
- `sortBy` (optional): relevancy (default), popularity, or publishedAt
- `pageSize` (optional): Number of results (1-100, default 20)
- `page` (optional): Page number for pagination (default 1)

## Testing

Run the tests with your NewsAPI key:

**Windows (PowerShell):**
```powershell
$env:NEWS_API_KEY="your_api_key_here"
mvn test
```

**Windows (CMD):**
```cmd
set NEWS_API_KEY=your_api_key_here
mvn test
```

**Linux/Mac:**
```bash
export NEWS_API_KEY=your_api_key_here
mvn test
```

## API Limits

The free NewsAPI tier has the following limits:
- 100 requests per day
- Articles from the last 30 days only
- Cannot use the `/everything` endpoint with dates older than 30 days

For higher limits and more features, check out [NewsAPI pricing plans](https://newsapi.org/pricing).

## Country Codes

Common country codes for the `country` parameter:
- `us` - United States
- `cn` - China
- `gb` - United Kingdom
- `jp` - Japan
- `de` - Germany
- `fr` - France
- `au` - Australia
- `ca` - Canada

See [ISO 3166-1 alpha-2](https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2) for all country codes.

## Categories

Available categories for the `category` parameter:
- `business` - Business news
- `entertainment` - Entertainment news
- `general` - General news
- `health` - Health news
- `science` - Science news
- `sports` - Sports news
- `technology` - Technology news

## Error Handling

The tools return descriptive error messages for common issues:
- Missing API key
- Invalid parameters
- API rate limits exceeded
- Network errors
- No results found

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.

## Support

For issues and questions:
- NewsAPI Documentation: https://newsapi.org/docs
- Ali-LangEngine Issues: https://github.com/alibaba/ali-langengine/issues
