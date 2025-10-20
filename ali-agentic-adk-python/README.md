# Ali Agentic ADK (Python)

A Python-first toolkit delivering agentic building blocks that integrate tightly with Google ADK components. The package currently wraps core LLM, tool, and embedding providers and exposes examples for browser automation and application flows.

## Quick Start

```bash
git clone https://github.com/your-org/ali-agentic-adk-python.git
cd ali-agentic-adk-python
pip install -e .
```

The project targets Python 3.10+ and depends on `google-adk`, `dashscope`, and `openai`. Installing in a virtual environment is highly recommended.

## Configuration

Runtime configuration is centralised in `ali_agentic_adk_python.config`. `RuntimeSettings` loads environment variables (or values defined in a `.env`/`.env.local` file) using Pydantic models.

| Service | Environment keys (defaults) | Notes |
| ------- | --------------------------- | ----- |
| DashScope | `DASHSCOPE_API_KEY`, `DASHSCOPE_APP_ID`, `DASHSCOPE_BASE_URL`, `DASHSCOPE_DEFAULT_MODEL` (`qwen-plus`) | Used by `DashscopeLLM` and DashScope tools. |
| Bailian | `BAILIAN_API_KEY` (alias `AK`), `BAILIAN_APP_ID` | Enables `BailianAppTool`. |
| OpenAI compatible | `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_EMBEDDING_MODEL`, `OPENAI_CHAT_MODEL`, `OPENAI_USER` | Employed by `OpenAIEmbedding` and other OpenAI-compatible components. |

Example `.env` snippet:

```ini
DASHSCOPE_API_KEY=sk-demo
DASHSCOPE_APP_ID=app-123
BAILIAN_API_KEY=bl-demo
OPENAI_API_KEY=sk-openai
```

Load settings in code via:

```python
from ali_agentic_adk_python.config import get_runtime_settings

runtime_settings = get_runtime_settings()
dashscope_settings = runtime_settings.dashscope()
llm = DashscopeLLM.from_settings(dashscope_settings)
```

## Project Layout

```
├── README.md
├── docs/
│   └── module_overview.md      # Module responsibilities and dependencies
├── examples/
│   ├── app_call_demo/
│   └── browser_use_demo/
├── src/
│   └── ali_agentic_adk_python/
│       ├── config/             # Pydantic settings models
│       ├── core/               # Models, tools, runtime, embeddings, utilities
│       └── extension/          # Browser-use and other integrations
└── tests/                      # Unit and integration-oriented tests
```

## Runtime Execution

The `core.runtime` package introduces minimal execution primitives tailored for the Python runtime. Create a `SystemContext`, attach services (e.g. Browser Use), and delegate a request through the `SyncExecutor`:

```python
from ali_agentic_adk_python.core.runtime import Request, SyncExecutor, SystemContext
from ali_agentic_adk_python.extension.web.runtime import attach_browser_use_service, get_browser_use_service

def handler(ctx: SystemContext, request: Request):
    service = get_browser_use_service(ctx)
    request_id = request.request_id or "demo"
    service.call_and_wait(request_id, lambda: service.handle_callback(request_id, "ok"))
    return {"status": "ok"}

context = attach_browser_use_service(SystemContext())
executor = SyncExecutor("demo", handler)
request = Request.from_payload({"action": "open"}, request_id="demo")

async def run():
    async for result in executor.invoke(context, request):
        print(result.model_dump())
```

See `examples/browser_use_demo/runtime_example.py` for a runnable end-to-end snippet with structured runtime logs.

## Running Tests

```bash
python -m pytest
```

Tests rely on mocks and do not contact external services. Ensure required development dependencies are installed (`pip install -e .[dev]` if you maintain an extras definition).

## License

## MCP Extension

```python
import asyncio

from ali_agentic_adk_python.extension.mcp import (
    McpSessionManager,
    McpStdioServerConfig,
)


async def main() -> list:
    manager = McpSessionManager(
        servers=[
            McpStdioServerConfig(
                namespace="filesystem",
                command="uvx",
                args=["modelcontextprotocol/server-filesystem"],
            )
        ]
    )
    await manager.start()
    return manager.build_google_adk_tools()


mcp_tools = asyncio.run(main())
```

# License
Apache-2.0
Copyright (C) 2025 AIDC-AI All rights reserved.
