# spring-a2ui-runtime Rebuild - Research Notes

> Created: 2026-05-19
> Purpose: Comprehensive reference for rebuilding spring-a2ui-runtime from scratch with pure A2UI direction

---

## 1. A2UI v0.8 Protocol - Complete Reference Summary

### 1.1 Core Protocol Concepts

A2UI is a **declarative, streaming UI protocol** where an AI agent sends component descriptions via JSONL (typically over SSE), and a client renders them natively. Key principles:

- **Flat component model** (adjacency list): Components are sent as a flat list with ID references, not nested JSON trees. This is LLM-friendly.
- **Data/structure separation**: UI structure via `surfaceUpdate`, dynamic data via `dataModelUpdate`. Bound via `path` references.
- **Progressive rendering**: Client buffers until `beginRendering`, then renders. Later messages update incrementally.
- **Catalog-driven**: Component types are defined in catalogs (standard + custom). The server selects a catalog the client supports.
- **Surface-scoped**: Multiple independent UI regions (surfaces) each with their own component buffer, data model, and root.

### 1.2 Server-to-Client Message Types (JSONL)

Each line is exactly one of:

| Message Type | Required Fields | Purpose |
|---|---|---|
| `surfaceUpdate` | `surfaceId`, `components[]` | Send component definitions (flat list with IDs) |
| `dataModelUpdate` | `surfaceId`, `contents[]` | Push data model changes (key-value entries) |
| `beginRendering` | `surfaceId`, `root`; optional `catalogId`, `styles` | Signal client to start rendering |
| `deleteSurface` | `surfaceId` | Remove a surface entirely |

### 1.3 Component Object Structure

```json
{
  "id": "unique-component-id",
  "weight": 1.0,  // optional, for Row/Column children (flex-grow)
  "component": {
    "ComponentTypeName": {
      // component-specific properties
    }
  }
}
```

The `component` object must contain exactly one key (the component type from the catalog).

### 1.4 Container Children: explicitList vs template

- `explicitList`: `["child_id_1", "child_id_2"]` - static known children
- `template`: `{ "dataBinding": "/path/to/list", "componentId": "template_id" }` - dynamic list rendering

Single-child containers use `"child": "component_id"` (not an array).

### 1.5 BoundValue System

Properties that can be data-bound accept a BoundValue object:

- `literalString`: static string value
- `literalNumber`: static numeric value
- `literalBoolean`: static boolean
- `literalArray`: static array value
- `path`: data model path reference (e.g., "/user/name")

If both `path` AND a `literal*` are present, it's an **initialization shorthand**: set the data model at path to the literal value, then bind to that path.

### 1.6 dataModelUpdate Structure

```json
{
  "dataModelUpdate": {
    "surfaceId": "main",
    "path": "user",  // optional, targets sub-path
    "contents": [
      {"key": "name", "valueString": "Bob"},
      {"key": "isVerified", "valueBoolean": true},
      {"key": "address", "valueMap": [
        {"key": "street", "valueString": "123 Main St"}
      ]}
    ]
  }
}
```

### 1.7 Client-to-Server Messages

Sent via A2A (not SSE). Must contain exactly one of `userAction` or `error`:

```json
{
  "userAction": {
    "name": "submit_form",
    "surfaceId": "main",
    "sourceComponentId": "submit_btn",
    "timestamp": "2025-09-19T17:05:00Z",
    "context": {
      "userInput": "resolved from data model",
      "formId": "f-123"
    }
  }
}
```

### 1.8 Catalog Negotiation (3-step)

1. **Server advertises** in Agent Card: `supportedCatalogIds[]`, `acceptsInlineCatalogs`
2. **Client declares** in every A2A message metadata: `supportedCatalogIds[]`, optional `inlineCatalogs[]`
3. **Server chooses** in `beginRendering`: `catalogId` field (defaults to standard catalog if omitted)

Standard catalog for v0.8: `https://a2ui.org/specification/v0_8/standard_catalog_definition.json`

### 1.9 Standard v0.8 Catalog Components

Text, Column, Row, Card, List, Tabs, Button, Image, Container, Divider, Modal, CheckBox, TextField, DateTimeInput, MultipleChoice, Slider, Icon, Video, AudioPlayer, Code, Chart, Table, Form, Confirmation, Accordion

### 1.10 A2A Extension for A2UI

- Extension URI: `https://a2ui.org/a2a-extension/a2ui/v0.8`
- A2UI messages transmitted as A2A `DataPart` with `mimeType: application/json+a2ui`
- Client capabilities sent in message metadata under `a2uiClientCapabilities`

---

## 2. FogUI Architecture Analysis

### 2.1 Architecture (Custom Canonical Model)

FogUI builds around a **custom canonical response model** called `GenerativeUIResponse`:

```
GenerativeUIResponse {
  thinking: List<ThinkingItem>     // LLM chain-of-thought
  content: List<ContentBlock>     // recursive UI tree
  metadata: Map<String, Object>  // includes contractVersion
}

ContentBlock {
  type: "text" | "component"
  value: Object         // for text blocks
  componentType: String // for component blocks
  props: Object         // for component blocks
  children: List<ContentBlock>  // nesting
}
```

This is a **tree-structured, inline nesting** model - fundamentally different from A2UI's flat adjacency list model.

### 2.2 Module Structure

| Module | Purpose |
|--------|---------|
| `fogui-java-core` | Canonical model, validation, A2UI translation, stream reconciliation, parsing |
| `fogui-spring-boot-starter` | Auto-config, deterministic advisors, generation policy |
| `fogui-spring-web-starter` | HTTP/SSE controllers, prompt SPI, transform runtime |
| `apps/be-transform-showcase` | Reference backend host |
| `packages/react` | Frontend renderer adapter |

### 2.3 Key FogUI-Specific Concepts (NOT in A2UI)

1. **GenerativeUIResponse** with `thinking` + `content` blocks - this is the LLM-oriented "canonical model"
2. **ContentBlock tree model** (type: text/component with nested children) - NOT A2UI's flat adjacency list
3. **Contract versioning** (`fogui/1.0`) stamped in metadata
4. **A2UiUnsupportedNode fallback** - wraps unknown A2UI nodes instead of dropping them
5. **StreamPatchReconciler** - LWW merge strategy for streaming responses
6. **UIResponseParser** - bracket-closing heuristic for partial JSON from LLM streaming
7. **TransformService + TransformStreamProcessor** - orchestrate LLM call -> parse canonical model -> serve response
8. **DeterministicOptionsAdvisor** - sets temperature=0, top-p=1, json response format
9. **CanonicalValidationAdvisor** - validates LLM output against canonical contract
10. **FogUiCanonicalContract** / FogUiCanonicalValidator - validates the custom canonical format
11. **Prompt: asks LLM to produce FogUI canonical format (type: text/component)** - NOT A2UI messages

### 2.4 FogUI Transform Flow (Custom Response)

```
Client POST /fogui/transform {content, context}
  -> TransformService builds prompt from TRANSFORM_SYSTEM_PROMPT
  -> Spring AI ChatClient with DeterministicOptionsAdvisor
  -> LLM returns JSON in FogUI canonical format {thinking, content}
  -> UIResponseParser.tryParsePartial() (stream) or direct parse (sync)
  -> StreamPatchReconciler (stream)
  -> CanonicalValidationAdvisor validates against fogui/1.0 contract
  -> CanonicalOutboundMapper (identity in v1)
  -> Response: {success, result: GenerativeUIResponse, usage, requestId}
```

The response to the client is **FogUI's canonical model**, NOT A2UI messages.

### 2.5 FogUI A2UI Compatibility Layer

FogUI has a separate `/fogui/compat/a2ui/inbound` endpoint that translates A2UI payloads INTO FogUI's canonical model:

```
A2UI JSON payload -> A2UiInboundTranslator.translate() -> GenerativeUIResponse + errors
  -> FogUiCanonicalValidator.validate() -> validation errors
  -> Response: {success, result, translationErrors, validationErrors}
```

This is a **one-way bridge**: A2UI -> FogUI, but there's no FogUI -> A2UI outbound path in the original FogUI codebase.

---

## 3. spring-a2ui-runtime Architecture Analysis

### 3.1 Architecture (Hybrid A2UI + FogUI Canonical)

The A2UI runtime was extracted from FogUI and attempts to be A2UI-native, but retains significant FogUI DNA:

**A2UI-native parts:**
- `A2UiMessage` with proper message types (SurfaceUpdate, DataModelUpdate, BeginRendering, DeleteSurface)
- `A2UiMessageValidator` enforcing A2UI protocol rules (sequence, catalog, component types)
- `A2UiOutboundMapper` converting internal model to A2UI messages
- `A2UiCatalogRegistry` / `A2UiCatalogService` for catalog validation and serving
- `A2UiActionController` / `A2UiActionService` for userAction routing
- `A2UiRequestCatalogNegotiator` for catalog negotiation
- Controller routes under `/a2ui/` prefix

**FogUI-inherited parts (THE PROBLEM):**
- `RuntimeResponse` with `thinking` + `content` - this is still the LLM-oriented canonical model
- `RuntimeContentBlock` with `type: "text" | "component"` - tree model, NOT flat A2UI
- `RuntimeThinkingItem` - LLM chain-of-thought concept
- `RuntimeResponseContract` / `RuntimeResponseValidator` - validates the FogUI-shaped internal model
- `RuntimeOutboundMapper` - currently identity (returns same object), placeholder for future mapping
- `A2UiInboundTranslator` - translates inbound A2UI into RuntimeResponse (FogUI-shaped model)
- `A2UiResponse` - convenience wrapper with `thinking` + `content` (FogUI-shaped)
- `A2UiStreamMessage` - LLM streaming envelope with `thinking` + `content` + `usage`
- `StreamPatchReconciler` - LWW merge for LLM streaming (FogUI concept)
- `UIResponseParser` - bracket-closing for partial JSON (LLM streaming concern)
- Prompt engineering: asks LLM to produce `{type: "text/component", componentType, props, children}` format
- Advisor chain validates against `RuntimeResponse` (the FogUI-shaped model)
- `TransformPrompts.RUNTIME_CANONICAL_RESPONSE_REMINDER` enforces the FogUI-shaped format

### 3.2 The Core Problem: Two Models, One Pipeline

The fundamental issue is that the runtime asks the LLM to produce a **FogUI-shaped response** (`RuntimeResponse` with `thinking` + `content` blocks), then maps it to A2UI messages via `A2UiOutboundMapper`. This creates several problems:

1. **The LLM prompt asks for `type: "text" | "component"` blocks** - this is NOT the A2UI wire format. The LLM doesn't know about surfaces, component IDs, adjacency lists, data model updates, or beginRendering.

2. **The OutboundMapper must infer an ID scheme** - it auto-generates IDs like `content-0`, `content-0-child-1` which are not meaningful or stable.

3. **The OutboundMapper has hardcoded component knowledge** - `EXPLICIT_CHILD_COMPONENTS = {Column, Container, List, Row, Tabs}` decides which A2UI children representation to use. This should come from the catalog, not be hardcoded.

4. **Text blocks become a special A2UI component** - `type: "text"` blocks are translated to `Text` components with `{text: {literalString: value}}`, but this mapping is a FogUI convention, not an A2UI-native approach.

5. **No data model support** - The LLM never generates `dataModelUpdate` messages because the canonical model doesn't have a concept of data binding. All data is inline in the component props.

6. **No multi-surface support** - The OutboundMapper always uses `surfaceId: "main"`. Multiple surfaces are not possible with the current internal model.

7. **No proper beginRendering lifecycle** - The OutboundMapper always appends a `beginRendering` message, but the LLM doesn't control when rendering should start.

8. **No userAction/context generation** - The LLM generates component props but never creates the `action` structure with bound context values that A2UI requires for interactivity.

### 3.3 Module Structure (Current)

| Module | Purpose |
|--------|---------|
| `a2ui-runtime-core` | Protocol models (A2UI messages + FogUI canonical), validation, translation, stream reconciliation |
| `a2ui-runtime-spring-starter` | Auto-config, deterministic advisors |
| `a2ui-runtime-spring-web-starter` | HTTP/SSE controllers, prompt SPI, action handling, catalog serving |
| `apps/be-transform-showcase` | Reference backend host |

### 3.4 Catalog Gap Analysis

The runtime's catalog at `META-INF/a2ui/catalogs/canonical-v0.8.json` defines 17 component types:
Text, Column, Row, Card, List, Tabs, Button, TextField, Image, Container, Table, Chart, Form, Confirmation, Accordion, Code

**Missing from standard v0.8 catalog:** Icon, Video, AudioPlayer, Divider, Modal, CheckBox, DateTimeInput, MultipleChoice, Slider

**Extra (NOT in standard v0.8 catalog):** Container (custom), Table (custom schema), Chart (custom schema), Form (custom schema), Confirmation (custom schema), Accordion (custom schema), Code (custom schema)

The `Container` component type in this catalog with `child`, `children`, `layout`, `columns`, `gap` properties is NOT part of the A2UI standard v0.8 catalog. This is a FogUI concept that leaked in.

The standard catalog's component props often use `additionalProperties: true` or don't use `BoundValue` objects properly. The runtime's catalog should match the standard catalog exactly.

### 3.5 Endpoint Summary (Current)

| Endpoint | Method | Response Format |
|----------|--------|----------------|
| `/a2ui/transform` | POST | Returns A2UI messages array `[surfaceUpdate, beginRendering]` |
| `/a2ui/transform/stream` | POST (SSE) | Streams A2UI message events |
| `/a2ui/catalogs/canonical-v0.8` | GET | Returns catalog JSON |
| `/a2ui/actions` | POST | Returns A2UiActionResponse |
| `/a2ui/compat/inbound` | POST | Translates A2UI -> RuntimeResponse |

---

## 4. Key Differences: What Needs to Change for the Rebuild

### 4.1 CRITICAL: Eliminate the Intermediate Canonical Model

The biggest change needed is to **remove `RuntimeResponse`, `RuntimeContentBlock`, and `RuntimeThinkingItem`** from the pipeline. The LLM should be prompted to produce **A2UI messages directly**, not a custom canonical format that then needs translation.

**Current flow (broken):**
```
LLM -> RuntimeResponse (FogUI model) -> A2UiOutboundMapper -> A2UI messages
```

**Target flow:**
```
LLM -> A2UI messages (surfaceUpdate, dataModelUpdate, beginRendering) directly
```

This eliminates:
- The need for OutboundMapper (with its hardcoded heuristics)
- The need for RuntimeResponseValidator (validating wrong format)
- The need for StreamPatchReconciler with LWW strategy (A2UI messages are idempotent)
- The need for RuntimeResponseContract and its version stamping
- The need for UIResponseParser (A2UI JSONL messages are individually parseable)

### 4.2 CRITICAL: Prompt Engineering Must Target A2UI

The prompt must ask the LLM to produce **A2UI v0.8 JSONL messages** directly, with:
- Proper `surfaceUpdate` messages with flat component lists and ID references
- `dataModelUpdate` messages for data binding
- `beginRendering` signals at the right time
- Proper `BoundValue` usage (`literalString`, `path`, `literalNumber`, etc.)
- Component names from the A2UI standard catalog
- Action definitions with `name` and `context` arrays

### 4.3 CRITICAL: The Catalog Must Match Standard v0.8

The current catalog has custom component types (Container, Table, Chart, Form, Confirmation, Accordion, Code) that are not in the A2UI standard v0.8 catalog. It also misses standard components (Icon, Video, AudioPlayer, Divider, Modal, CheckBox, DateTimeInput, MultipleChoice, Slider).

For the rebuild:
- The default catalog should match the official A2UI v0.8 standard catalog **exactly**
- Custom catalogs should be supported via the catalog negotiation mechanism
- The `Container` component should be removed (A2UI uses Column/Row for layout)

### 4.4 IMPORTANT: Streaming Must Use A2UI JSONL Semantics

Currently, the streaming pipeline treats A2UI like an LLM streaming response - accumulating text, trying to parse incomplete JSON, and reconciling snapshots. A2UI is designed for **progressive rendering via incremental messages**:

Current (wrong): Accumulate LLM text -> tryParsePartial -> reconcile fogui snapshots -> map to A2UI messages

Target (right): Each LLM output chunk should ideally represent one or more A2UI JSONL messages. The runtime should parse individual JSONL lines as they come and forward them as SSE events.

### 4.5 IMPORTANT: Action Handling Must Generate A2UI Responses

When a user action comes in (`POST /a2ui/actions`), the handler should return **A2UI messages** (surfaceUpdate, dataModelUpdate, beginRendering, deleteSurface). Currently, handlers return `List<A2UiMessage>` which is correct, but the handler SPI and the response format need to be clean A2UI.

### 4.6 WHAT TO KEEP

These concepts/patterns from the existing codebases are valuable and should be preserved (with adaptation):

1. **Spring Boot auto-configuration pattern** - `@ConditionalOnMissingBean`, property prefixes, layered modules
2. **Deterministic options advisor pattern** - Setting temperature=0, top-p=1, json response format via Spring AI advisor
3. **Provider-specific option customizers** - OpenAI, Anthropic, Gemini, Azure OpenAI mapping
4. **Request correlation** - `X-A2UI-Request-Id` header propagation
5. **Feature toggles** - Enable/disable endpoints via properties
6. **Error codes with categories** - Structured error reporting
7. **A2UiMessage structure** - The proper A2UI v0.8 message types are well-defined
8. **A2UiMessageValidator** - Outbound validation with sequence rules is correct
9. **A2UiCatalogRegistry** - Catalog loading and component type validation
10. **A2UiRequestCatalogNegotiator** - The negotiation logic is sound
11. **A2UiActionHandler SPI** - Strategy pattern for action routing
12. **Three-layer module structure** - core / spring-starter / spring-web-starter

---

## 5. Rebuild Direction: Architecture Proposal

### 5.1 Target Architecture

```
┌─────────────────────────────────────────────────┐
│              spring-a2ui-runtime                 │
├─────────────────────────────────────────────────┤
│                                                  │
│  a2ui-runtime-core                              │
│  ├── A2UI Protocol Models (v0.8)                │
│  │   ├── A2UiMessage (SurfaceUpdate, etc.)      │
│  │   ├── A2UiUserAction / A2UiClientEvent        │
│  │   ├── A2UiClientError                         │
│  │   ├── ComponentDefinition                     │
│  │   ├── DataEntry / BoundValue                  │
│  │   └── A2UiProtocol constants                  │
│  ├── A2UiMessageValidator                        │
│  ├── A2UiCatalogRegistry + catalog JSON          │
│  ├── A2UiMessageParser (JSONL line parser)       │
│  ├── A2UiSurfaceBuffer (component + data model)  │
│  └── Error models (validation errors, etc.)      │
│                                                  │
│  a2ui-runtime-spring-starter                    │
│  ├── DeterministicOptionsAdvisor                 │
│  ├── A2UiAdvisorOrder                            │
│  ├── Provider option customizers                 │
│  ├── Auto-configuration                          │
│  └── Deterministic policy (temperature, etc.)    │
│                                                  │
│  a2ui-runtime-spring-web-starter                │
│  ├── Controllers                                 │
│  │   ├── A2UiSurfaceController (POST /a2ui/surface│
│  │   ├── A2UiStreamController (SSE)              │
│  │   ├── A2UiActionController (POST /a2ui/actions│
│  │   └── A2UiCatalogController (GET)             │
│  ├── Services                                    │
│  │   ├── A2UiSurfaceService (orchestration)      │
│  │   ├── A2UiStreamService (SSE streaming)        │
│  │   ├── A2UiActionService (action routing)       │
│  │   ├── A2UiCatalogService                       │
│  │   └── A2UiCatalogNegotiator                    │
│  ├── A2UiMessagePromptProvider (SPI)              │
│  ├── A2UiTransformRuntime (SPI)                  │
│  ├── A2UiSurfaceBuffer (per-request state)       │
│  └── Auto-configuration                          │
│                                                  │
│  apps/showcase (thin demo host)                  │
│                                                  │
└─────────────────────────────────────────────────┘
```

### 5.2 Key Design Decisions for Rebuild

1. **NO intermediate canonical model.** The LLM produces A2UI JSONL messages directly. The runtime validates and forwards them.

2. **A2UI JSONL as the contract.** Every message the LLM outputs is parsed as a single A2UI message (surfaceUpdate, dataModelUpdate, beginRendering, or deleteSurface). No custom format in between.

3. **A2UiMessageParser instead of UIResponseParser.** Instead of trying to parse partial JSON and reconcile snapshots, parse individual complete JSON lines. A2UI is designed for this - each line is a complete, independent message.

4. **A2UiSurfaceBuffer for state tracking.** The runtime maintains a buffer of components and data model per surface. This enables:
   - Validation of ID references (component X references child Y which must exist)
   - beginRendering lifecycle (must follow surfaceUpdate for same surfaceId)
   - Incremental updates (new surfaceUpdate messages update the buffer)

5. **Prompt targets A2UI directly.** The system prompt includes:
   - The A2UI v0.8 message format specification
   - The catalog definition (component types and their properties)
   - Examples of correct A2UI JSONL output
   - Instruction to output one message per line (JSONL)

6. **Streaming uses SSE with individual message events.** Each parsed A2UI message is immediately sent as an SSE event. No accumulation needed - A2UI's progressive rendering design handles this.

7. **Action handlers return A2UI messages.** When a user action is handled, the response is `List<A2UiMessage>` containing surfaceUpdate, dataModelUpdate, or deleteSurface messages.

8. **Standard v0.8 catalog as default.** The catalog at `META-INF/a2ui/catalogs/standard-v0.8.json` matches the official A2UI specification exactly. Custom catalogs can be loaded from classpath or provided inline.

9. **Spring AI 2.0.0 compatibility.** Use the latest Spring AI APIs (ChatClient, Advisors, etc.) with the new 2.0.0 module structure.

10. **Action handling SPI preserved.** `A2UiActionHandler` with `supports()` + `handle()` pattern, auto-discovered via Spring beans.

### 5.3 Module Responsibilities

**a2ui-runtime-core:**
- Pure Java, no Spring dependencies
- A2UI v0.8 message models (records/immutables)
- A2UI message validator with sequence rules
- A2UI catalog registry and loader
- A2UI JSONL parser
- A2UI surface buffer (component map + data model per surface)
- Error models and codes
- NO LLM/parsing/streaming concerns

**a2ui-runtime-spring-starter:**
- Spring Boot auto-configuration
- Deterministic options advisor (temperature 0, json mode, etc.)
- Provider-specific chat options customizers
- Generation policy
- NO HTTP/web concerns

**a2ui-runtime-spring-web-starter:**
- HTTP/SSE controllers for A2UI endpoints
- Service layer: surface creation, streaming, action routing, catalog serving
- Prompt provider SPI (pluggable prompts)
- Transform runtime SPI (pluggable AI backend)
- Request correlation
- A2UI metrics

### 5.4 What Gets Removed

These classes/concepts from the current codebase are **gone** in the rebuild:

- `RuntimeResponse` - replaced by A2UI messages directly
- `RuntimeContentBlock` - A2UI has flat component definitions
- `RuntimeThinkingItem` - not part of A2UI protocol
- `RuntimeResponseContract` / `RuntimeResponseValidator` - validate A2UI messages instead
- `RuntimeOutboundMapper` - no mapping needed if LLM produces A2UI directly
- `A2UiOutboundMapper` - if LLM produces A2UI directly, no mapping needed
- `A2UiInboundTranslator` - becomes unnecessary
- `A2UiResponse` (thinking+content wrapper) - not an A2UI concept
- `A2UiStreamMessage` (thinking+content+usage) - replaced by SSE with individual A2UI messages
- `StreamPatchReconciler` - A2UI messages are individually complete
- `UIResponseParser` (bracket-closing heuristic) - replaced by JSONL line parser
- `A2UiCompatibilityService` / compatibility controller - no need to translate between formats
- `TransformPrompts.RUNTIME_CANONICAL_RESPONSE_REMINDER` - replaced by A2UI-format prompt
- `Container` component type in catalog - not in A2UI standard (use Column/Row)
- Custom component types not in A2UI standard v0.8 catalog

### 5.5 What Gets Added

- `A2UiMessageParser` - JSONL line-by-line parser for A2UI messages
- `A2UiSurfaceBuffer` - per-surface component buffer and data model tracker
- `A2UiMessagePromptProvider` - prompt SPI that targets A2UI v0.8 format
- `A2UiStandardCatalogLoader` - loads the official v0.8 standard catalog from spec JSON
- Proper BoundValue models (literalString, literalNumber, literalBoolean, literalArray, path)
- Proper DataEntry models (key + one of valueString/valueNumber/valueBoolean/valueMap)
- Proper action context models with BoundValue resolution
- Surface lifecycle management (create, update, delete)

### 5.6 Streaming Strategy

The current approach of accumulating LLM text and trying to parse incomplete JSON is fundamentally wrong for A2UI. A2UI is designed for progressive rendering - each message is a complete unit.

**New approach:**
1. LLM is instructed to output one A2UI JSONL message per output chunk
2. Each SSE data chunk is buffered until a complete JSON line is received
3. The complete line is parsed as an A2UiMessage
4. The message is validated against the catalog and protocol rules
5. Valid messages are immediately forwarded to the client as SSE events
6. Invalid messages trigger error handling

This leverages A2UI's design principle: "Each JSONL message is a self-contained unit of information."

### 5.7 Determinism Without Intermediate Model

The "determinism" goal means: **if a developer uses our runtime, they get valid A2UI output without worrying about prompt engineering or LLM quirks.** This is achieved through:

1. **System prompt** that includes the exact A2UI message format specification
2. **Catalog-aware validation** that rejects invalid component types
3. **Deterministic advisor** that sets temperature=0, json mode, etc.
4. **Message sequence validation** that ensures beginRendering follows surfaceUpdate
5. **Structured output** via Spring AI's structured output capabilities or JSON mode

The developer doesn't need to write prompts or worry about LLM output format - the runtime handles it.

---

## 6. A2UI v0.8 Standard Catalog (Complete)

This is the official standard catalog from the A2UI specification:

| Component | Required Props | Optional Props | Children |
|---|---|---|---|
| Text | text (BoundValue) | usageHint (enum) | Leaf |
| Image | url (BoundValue) | altText, fit, usageHint | Leaf |
| Icon | name (BoundValue) | - | Leaf |
| Video | url (BoundValue) | - | Leaf |
| AudioPlayer | url (BoundValue) | description | Leaf |
| Row | children (explicitList/template) | distribution, alignment | Container |
| Column | children (explicitList/template) | distribution, alignment | Container |
| List | children (explicitList/template) | direction, alignment | Container |
| Card | child (string ID) | - | Container (1) |
| Tabs | tabItems (array of {title, child}) | - | Container |
| Divider | - | axis (h/v) | Leaf |
| Modal | entryPointChild, contentChild | - | Container |
| Button | child (string ID) | primary (bool), action ({name, context}) | Container (1) |
| CheckBox | label (BoundValue), value (BoundBool) | - | Leaf |
| TextField | label (BoundValue) | text, textFieldType, validationRegexp | Leaf |
| DateTimeInput | value (BoundValue) | enableDate, enableTime | Leaf |
| MultipleChoice | selections (BoundValue), options (array) | maxAllowedSelections | Leaf |
| Slider | value (BoundValue) | label, minValue, maxValue | Leaf |

**BoundValue examples:**
- Static: `{"text": {"literalString": "Hello"}}`
- Dynamic: `{"text": {"path": "/user/name"}}`
- Init shorthand: `{"text": {"path": "/user/name", "literalString": "Guest"}}`

**Children examples:**
- Explicit: `{"children": {"explicitList": ["id1", "id2"]}}`
- Template: `{"children": {"template": {"dataBinding": "/items", "componentId": "item_template"}}}`

---

## 7. Existing Codebase File-by-File Inventory

### 7.1 spring-a2ui-runtime (current)

#### a2ui-runtime-core
- `contract/a2ui/A2UiProtocol.java` - Protocol version constant (v0.8)
- `contract/a2ui/A2UiCatalogIds.java` - Catalog URI constants
- `contract/a2ui/A2UiCatalogRegistry.java` - Singleton catalog loader and validator
- `contract/a2ui/A2UiMessage.java` - Core A2UI message types (SurfaceUpdate, DataModelUpdate, BeginRendering, DeleteSurface, ComponentDefinition, DataEntry)
- `contract/a2ui/A2UiUserAction.java` - Inbound user action model
- `contract/a2ui/A2UiClientEvent.java` - Client event discriminated union (userAction | error)
- `contract/a2ui/A2UiClientError.java` - Structured client error
- `contract/a2ui/A2UiActionResponse.java` - Server response to action
- `contract/a2ui/A2UiErrorResponse.java` - Error response envelope
- `contract/a2ui/A2UiStreamMessage.java` - **FOGUI**: LLM streaming envelope
- `contract/a2ui/A2UiResponse.java` - **FOGUI**: thinking+content wrapper
- `contract/a2ui/A2UiInboundTranslator.java` - **FOGUI**: translates A2UI -> RuntimeResponse
- `contract/a2ui/A2UiOutboundMapper.java` - **FOGUI**: maps RuntimeResponse -> A2UI messages (with heuristics)
- `contract/a2ui/A2UiMessageValidator.java` - A2UI protocol validator (sequence + catalog)
- `contract/a2ui/A2UiValidationContext.java` - Validation context
- `contract/a2ui/A2UiValidationErrorCode.java` - Error enum
- `contract/a2ui/A2UiValidationError.java` - Error data class
- `contract/a2ui/A2UiMessageValidationException.java` - Exception wrapper
- `contract/a2ui/A2UiTranslationError.java` - Translation error
- `contract/a2ui/A2UiTranslationResult.java` - Translation result with errors
- `contract/RuntimeResponseContract.java` - **FOGUI**: canonical contract version
- `contract/RuntimeResponseValidator.java` - **FOGUI**: validates RuntimeResponse
- `contract/RuntimeOutboundMapper.java` - **FOGUI**: identity mapper
- `contract/RuntimeErrorCategory.java` - Error category enum
- `contract/RuntimeErrorCode.java` - Error codes enum
- `contract/RuntimeValidationContext.java` - Validation context
- `contract/RuntimeValidationError.java` - Error data class
- `model/RuntimeResponse.java` - **FOGUI**: thinking + content model
- `model/RuntimeContentBlock.java` - **FOGUI**: text/component tree node
- `model/RuntimeThinkingItem.java` - **FOGUI**: LLM thinking
- `service/StreamPatchReconciler.java` - **FOGUI**: LWW streaming reconciliation
- `service/UIResponseParser.java` - **FOGUI**: bracket-closing partial JSON parser
- `resources/META-INF/a2ui/catalogs/canonical-v0.8.json` - **INCOMPLETE**: custom catalog (missing standard components, has extra)

#### a2ui-runtime-spring-starter
- `A2UiRuntimeAutoConfiguration.java` - Auto-config
- `advisor/DeterministicOptionsAdvisor.java` - Sets deterministic LLM options
- `advisor/RuntimeValidationAdvisor.java` - **FOGUI**: validates RuntimeResponse
- `advisor/RuntimeAdvisorContextKeys.java` - Context key constants
- `advisor/RuntimeAdvisorOrder.java` - Advisor ordering constants
- `advisor/RuntimeAdvisorException.java` - Advisor exception
- `advisor/RuntimeAdvisorsProperties.java` - Advisor config properties
- `advisor/RuntimeAdvisorErrorCodes.java` - Error code constants
- `policy/*` - Provider-specific chat options customizers (keep)

#### a2ui-runtime-spring-web-starter
- `controller/A2UiTransformController.java` - Transform endpoints (keep, refactor)
- `controller/A2UiActionController.java` - Action endpoint (keep)
- `controller/A2UiCatalogController.java` - Catalog endpoint (keep)
- `controller/A2UiCompatibilityController.java` - **FOGUI**: compatibility bridge (remove)
- `model/transform/A2UiTransformRequest.java` - Request DTO (refactor)
- `model/transform/A2UiTransformResponse.java` - **FOGUI**: custom response envelope (remove)
- `runtime/A2UiTransformRuntime.java` - SPI interface (keep)
- `runtime/SpringAiTransformRuntime.java` - Default implementation (keep, refactor)
- `prompt/TransformPromptProvider.java` - SPI interface (keep, refactor)
- `prompt/TransformPromptContext.java` - Context record (keep, refactor)
- `prompt/DefaultTransformPromptProvider.java` - **FOGUI**: prompts ask for canonical format (replace)
- `service/TransformService.java` - Sync transform orchestrator (refactor)
- `service/TransformStreamProcessor.java` - **FOGUI**: stream with accumulation (replace with JSONL parsing)
- `service/TransformPrompts.java` - **FOGUI**: FogUI-format prompts (replace)
- `service/TransformExecutionException.java` - Keep
- `service/TransformErrorCodes.java` - Keep
- `service/A2UiActionService.java` - Action routing (keep)
- `service/A2UiActionHandler.java` - SPI interface (keep)
- `service/A2UiActionException.java` - Keep
- `service/A2UiActionErrorCodes.java` - Keep
- `service/A2UiCompatibilityService.java` - **FOGUI**: compatibility translation (remove)
- `service/A2UiCatalogService.java` - Catalog serving (keep)
- `service/A2UiRequestCatalogNegotiator.java` - Keep
- `service/A2UiRuntimeMetrics.java` - Keep
- `service/RequestCorrelationService.java` - Keep
- `autoconfigure/A2UiWebAutoConfiguration.java` - Keep
- `properties/A2UiWebProperties.java` - Keep
- `filter/RequestCorrelationMdcFilter.java` - Keep

---

## 8. Spring AI 2.0.0 Migration Notes

Current codebase uses Spring AI 1.1.0-M2. Key changes for 2.0.0:

- `ChatClient` API may have changed - need to verify
- `BaseAdvisor` replaced `CallAroundAdvisor` and `StreamAroundAdvisor` (already using BaseAdvisor in current code)
- `ChatClientRequest` / `ChatClientResponse` APIs may have changed
- Structured output support may be improved - can leverage for A2UI message generation
- The streaming API may use different Flux types

Need to check Spring AI 2.0.0 release notes for exact API changes.