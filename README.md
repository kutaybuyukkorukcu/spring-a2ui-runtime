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
- **Frontend**: http://localhost:5173 (React)

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

## FogUI React + CLI

FogUI consists of two core packages:
- **`@fogui/react`**: The core React provider and hooks for rendering generative UI.
- **`@fogui/cli`**: A command-line tool to bootstrap a component `Adapter` for your project.

### 1. Installation

```bash
npm install @fogui/react @fogui/cli
```

### 2. Create a Component Adapter

Run the CLI and follow the prompts to generate an adapter file tailored to your component library (e.g., Shadcn, Material UI). This file acts as a bridge between the FogUI schema and your actual UI components.

```bash
npx fogui create
```

This will create an `adapter.ts` (or a name you choose) in your specified directory.

### 3. Integrate the Provider

Wrap your application with the `FogUIProvider`, passing your newly created adapter and the API endpoint for the FogUI backend.

```tsx
import { FogUIProvider } from '@fogui/react';
import { myAdapter } from './path/to/my-adapter'; // Import your generated adapter

function App() {
  return (
    // The adapter tells FogUI how to render components
    // The apiEndpoint points to the backend that transforms text to UI schemas
    <FogUIProvider adapter={myAdapter} apiEndpoint="http://localhost:5001/v1/chat/completions">
      {/* Your application components can now use useFogUI() hook */}
    </FogUIProvider>
  );
}
```

## Schema & Adapter API

For developers wanting to create or customize an adapter manually, it's important to understand the core data structures. FogUI uses a canonical component schema that your adapter must map to.

### Canonical Component Schema

The backend returns a tree of components matching this schema. Your adapter's job is to translate these abstract definitions into concrete UI. The standard components are:

- **`Card`**: A container with a title, description, and children.
- **`Table`**: Displays tabular data with headers and rows.
- **`List`**: A simple ordered or unordered list of items.
- **`Form`**: A container for input fields and buttons.
- **`Input`**: A text, number, or password input field.
- **`Button`**: A button with a label and an associated action.
- **`Stack`**: A layout container for arranging items horizontally or vertically.
- **`Grid`**: A layout container for arranging items in a grid.
- **`Tabs` / `TabPane`**: A tabbed interface to switch between content panes.
- **`Badge`**: A small component to display a status or label.

### The Adapter Interface

The `Adapter` is a simple object that tells the `FogUIProvider` how to render and map props for each component in the schema.

```ts
export interface Adapter {
  // A map from schema componentType to your React component
  components: {
    Card: React.ComponentType<any>;
    Table: React.ComponentType<any>;
    // ... and so on for all schema components
  };

  // A function to transform props from the schema to your component's API
  mapProps: (
    componentType: FogUIComponent['componentType'],
    props: any,
  ) => any;
}
```

- **`components`**: This is a key-value map where the key is the string `componentType` from the schema (e.g., "Card") and the value is your actual React component (e.g., `<ShadcnCard />`).
- **`mapProps`**: This function receives the `componentType` and the `props` from the API response. It should return a new props object that matches the expected API of your component library. This is useful for renaming props (e.g., `title` -> `cardTitle`) or performing other transformations.

## Documentation

- [Product Vision](./PRODUCT_VISION_V2.md)
- [Backend README](./backend-java/README.md)
