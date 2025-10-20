# Greenhouse Integration for Ali-LangEngine

This module provides Greenhouse Recruiting tool calling integration for Ali-LangEngine, allowing AI agents to interact with the Greenhouse ATS (Applicant Tracking System) API.

## Features

- **List Jobs**: Retrieve all job openings with pagination support
- **Get Job Details**: Get comprehensive information about a specific job
- **List Candidates**: Browse candidates with flexible filtering options
- **Tool Calling Support**: Full integration with Ali-LangEngine's tool calling framework
- **Date Filtering**: Filter candidates by creation and update timestamps
- **Rich Results**: Access job details, hiring teams, departments, offices, and candidate information

## Prerequisites

1. **Greenhouse API Key**: Obtain your API key from Greenhouse account settings
   - Log in to your Greenhouse account
   - Go to Settings → API Credential Management
   - Create a new Harvest API key with appropriate permissions
2. **Java 17+**: Required for building and running
3. **Maven**: For dependency management

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>ali-langengine-greenhouse</artifactId>
    <version>1.2.6-202508111516</version>
</dependency>
```

## Configuration

Set your Greenhouse API key as an environment variable:

**Windows (PowerShell):**
```powershell
$env:GREENHOUSE_API_KEY="your_api_key_here"
```

**Windows (CMD):**
```cmd
set GREENHOUSE_API_KEY=your_api_key_here
```

**Linux/Mac:**
```bash
export GREENHOUSE_API_KEY=your_api_key_here
```

Alternatively, pass the API key directly to the tool constructor:

```java
GreenhouseListJobsTool tool = new GreenhouseListJobsTool("your_api_key_here");
```

## Usage

### 1. List Jobs Tool

Retrieve all job openings from Greenhouse.

```java
import com.alibaba.langengine.greenhouse.tool.GreenhouseListJobsTool;
import com.alibaba.langengine.core.tool.ToolExecuteResult;

// Create the tool
GreenhouseListJobsTool listJobsTool = new GreenhouseListJobsTool();

// List first page of jobs (default 50 per page)
String input = "{\"page\": 1, \"perPage\": 10}";
ToolExecuteResult result = listJobsTool.run(input);
System.out.println(result.getOutput());

// Use default parameters
String defaultInput = "{}";
ToolExecuteResult defaultResult = listJobsTool.run(defaultInput);
System.out.println(defaultResult.getOutput());
```

**Supported Parameters:**
- `page` (optional): Page number for pagination (default: 1)
- `perPage` (optional): Number of results per page, max 500 (default: 50)

### 2. Get Job Details Tool

Get detailed information about a specific job.

```java
import com.alibaba.langengine.greenhouse.tool.GreenhouseGetJobTool;
import com.alibaba.langengine.core.tool.ToolExecuteResult;

// Create the tool
GreenhouseGetJobTool getJobTool = new GreenhouseGetJobTool();

// Get job by ID
String input = "{\"jobId\": 12345}";
ToolExecuteResult result = getJobTool.run(input);
System.out.println(result.getOutput());
```

**Supported Parameters:**
- `jobId` (required): The ID of the job to retrieve

**Returns:**
- Job name, status, and requisition ID
- Departments and offices
- Hiring team (managers, recruiters, coordinators)
- Job openings (open/closed count)
- Notes and timestamps

### 3. List Candidates Tool

Browse candidates with flexible filtering options.

```java
import com.alibaba.langengine.greenhouse.tool.GreenhouseListCandidatesTool;
import com.alibaba.langengine.core.tool.ToolExecuteResult;

// Create the tool
GreenhouseListCandidatesTool listCandidatesTool = new GreenhouseListCandidatesTool();

// List candidates with basic pagination
String input = "{\"page\": 1, \"perPage\": 20}";
ToolExecuteResult result = listCandidatesTool.run(input);
System.out.println(result.getOutput());

// Filter by creation date
String dateInput = "{\"createdAfter\": \"2024-01-01T00:00:00Z\", \"perPage\": 10}";
ToolExecuteResult dateResult = listCandidatesTool.run(dateInput);
System.out.println(dateResult.getOutput());

// Filter by update date
String updateInput = "{\"updatedAfter\": \"2024-06-01T00:00:00Z\", \"perPage\": 10}";
ToolExecuteResult updateResult = listCandidatesTool.run(updateInput);
System.out.println(updateResult.getOutput());
```

**Supported Parameters:**
- `page` (optional): Page number for pagination (default: 1)
- `perPage` (optional): Number of results per page, max 500 (default: 50)
- `createdBefore` (optional): Filter candidates created before this date (ISO 8601 format)
- `createdAfter` (optional): Filter candidates created after this date (ISO 8601 format)
- `updatedBefore` (optional): Filter candidates updated before this date (ISO 8601 format)
- `updatedAfter` (optional): Filter candidates updated after this date (ISO 8601 format)

**Date Format:** Use ISO 8601 format: `YYYY-MM-DDTHH:MM:SSZ` (e.g., `2024-01-01T00:00:00Z`)

## Testing

Run the unit tests (no API key required):

```bash
mvn test -Dtest=GreenhouseToolsUnitTest
```

Run the integration tests (requires API key):

**Windows (PowerShell):**
```powershell
$env:GREENHOUSE_API_KEY="your_api_key_here"
mvn test -Dtest=GreenhouseToolsIntegrationTest
```

**Linux/Mac:**
```bash
export GREENHOUSE_API_KEY=your_api_key_here
mvn test -Dtest=GreenhouseToolsIntegrationTest
```

## API Permissions

Your Greenhouse API key needs the following permissions:
- **Harvest API access**: Read access to jobs, candidates, departments, and offices
- **Minimum permissions**: 
  - Jobs: Read
  - Candidates: Read
  - Departments: Read
  - Offices: Read

## Rate Limits

Greenhouse API has rate limits based on your account plan:
- **Basic**: 50 requests per 10 seconds
- **Premium**: Higher limits available

The tools automatically handle pagination to work within these limits.

## Error Handling

The tools return descriptive error messages for common issues:
- Missing or invalid API key
- Invalid parameters
- Job or candidate not found
- API rate limits exceeded
- Network errors

## Data Privacy

⚠️ **Important**: Greenhouse contains sensitive candidate and employee data. Ensure:
- API keys are stored securely
- Access is restricted to authorized personnel
- Compliance with data privacy regulations (GDPR, CCPA, etc.)
- Audit logs are maintained

## Examples

See the `examples` package for comprehensive usage examples:
- `GreenhouseExamples.java`: Complete examples of all tool operations

Run examples:

```bash
export GREENHOUSE_API_KEY=your_api_key_here
mvn exec:java -Dexec.mainClass="com.alibaba.langengine.greenhouse.examples.GreenhouseExamples"
```

## Greenhouse API Documentation

For more information about the Greenhouse API:
- [Greenhouse Harvest API Documentation](https://developers.greenhouse.io/harvest.html)
- [Authentication](https://developers.greenhouse.io/harvest.html#authentication)
- [Rate Limiting](https://developers.greenhouse.io/harvest.html#throttling)

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.

## Support

For issues and questions:
- Greenhouse API Support: https://support.greenhouse.io/
- Ali-LangEngine Issues: https://github.com/alibaba/ali-langengine/issues
