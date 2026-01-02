# GenUI - Generative UI Platform

> Transform LLM responses into beautiful, interactive UI components.

## Quick Start

### 1. Set your API key

Create a `.env` file in the project root:

```bash
# For OpenAI
GENUI_PROVIDER=openai
OPENAI_API_KEY=sk-your-openai-key-here

# OR for Azure OpenAI
GENUI_PROVIDER=azureopenai
AZURE_OPENAI_API_KEY=your-azure-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
AZURE_OPENAI_DEPLOYMENT_NAME=gpt-4o-mini
```

### 2. Run with Docker Compose

```bash
docker-compose up --build
```

This starts:
- **Backend**: http://localhost:5001 (Spring Boot + Spring AI)
- **Frontend**: http://localhost:3001 (React)

### 3. Test the API

```bash
curl -X POST http://localhost:5001/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "stream": true,
    "messages": [{"role": "user", "content": "What is the weather in Tokyo?"}]
  }'
```

## API Keys

There are **two ways** to provide API keys:

### Option A: Environment Variables (Recommended for development)

Set in `.env` file or export:
```bash
export OPENAI_API_KEY=sk-...
```

### Option B: Request Headers (BYOK - Bring Your Own Key)

Pass per-request via headers:
```bash
curl -X POST http://localhost:5001/v1/chat/completions \
  -H "X-LLM-API-Key: sk-your-key" \
  -H "X-LLM-Provider: openai" \
  ...
```

| Header | Description |
|--------|-------------|
| `X-LLM-API-Key` | Your LLM provider API key |
| `X-LLM-Provider` | `openai` or `azure` |
| `X-Azure-Endpoint` | Azure endpoint URL (if Azure) |
| `X-Azure-Deployment` | Azure deployment name (if Azure) |

## Project Structure

```
genui-poc/
├── backend-java/        # Spring Boot backend
│   ├── src/main/java/   # Java source files
│   ├── pom.xml          # Maven config
│   └── Dockerfile       # Docker build
├── client/              # React frontend
├── docker-compose.yml   # Production compose
└── .env                 # API keys (create this)
```

## Documentation

- [Product Vision](./PRODUCT_VISION_V2.md)
- [Backend README](./backend-java/README.md)
