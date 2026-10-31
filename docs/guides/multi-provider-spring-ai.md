# Multi-provider Spring AI

OpenAI remains the **golden path** for getting started. spring-a2ui uses Spring
AI `ChatClient` + tools; other providers work when Spring AI supports them and
tool calling is reliable enough for template/dynamic modes.

Getting started stays OpenAI-default: [Getting started](getting-started.md).

## What the runtime already does

Provider-aware chat option customizers exist for OpenAI, Anthropic, and Vertex
AI Gemini (classpath-conditional in `a2ui-runtime-spring-starter`). Deterministic
temperature / top-p / etc. are applied via advisors when enabled. That apply
keeps a forced tool choice already set on the request options.

Dynamic compose still forces one-shot `generateA2Ui` / `renderA2Ui`
(`returnDirect`). The active provider comes from the configured chat model
property (`A2UiProviderResolver`). When no model property is set, a single
provider `ChatOptions` type on the classpath is used. Several types and no
configured provider is ambiguous.

If the active provider cannot force the tool, the stream emits SSE
`event: error` with `TOOL_CHOICE_UNAVAILABLE`. The runtime does not continue
with empty `ChatOptions`. Getting started stays OpenAI.

Checked against Spring AI **`1.1.0-M2`** (the BOM in the root `pom.xml`).

## Forced tool choice

| Provider | Dynamic force | What goes on the wire |
|----------|---------------|------------------------|
| OpenAI | Forced | Named `tool_choice` function (`generateA2Ui` or `renderA2Ui`). Golden path. |
| OpenAI-compatible servers (Groq, Kimi / Moonshot, DeepSeek, Mistral, Ollama, vLLM, Azure OpenAI through the OpenAI client) | Forced, same OpenAI path | Same `tool_choice` body. The call works only when **that server honors `tool_choice`**. Open models such as Kimi do not get a separate client. |
| Mistral native (`spring-ai-mistral-ai`) | Forced | Spring AI 1.1.0-M2 exposes `tool_choice` as `auto` / `any` / `none`, not a function name. Each hop registers one tool, so the runtime sends `any`. |
| Anthropic (direct) | Fail-closed | `AnthropicChatOptions` / `ChatCompletionRequest` have tools and no `tool_choice`. |
| Vertex AI Gemini | Fail-closed | `VertexAiGeminiChatOptions` has no function-calling mode or allowed-function name. |
| Google GenAI | Fail-closed | `GoogleGenAiChatOptions` has no function-calling mode. |
| AWS Bedrock Converse | Fail-closed | `BedrockChatOptions` has no tool choice. The Converse `ToolConfiguration` is built from tool definitions only. |
| Azure OpenAI native starter (`spring-ai-azure-openai`) | Fail-closed | `AzureOpenAiChatOptions` has no tool choice. Use the OpenAI-compatible client (row above) when Azure should force. |
| Ollama native (`spring-ai-ollama`) | Fail-closed | `OllamaChatOptions` has no tool choice. Use Ollama's OpenAI-compatible endpoint on the OpenAI path when the server honors `tool_choice`. |

Template mode and host `assemble` do not force these tools. They stay available
when dynamic compose fail-closes.

Set the chat model property for the provider you run
(`spring.ai.openai.chat.options.model`, `spring.ai.anthropic.chat.options.model`,
`spring.ai.mistralai.chat.options.model`,
`spring.ai.vertex.ai.gemini.chat.options.model`,
`spring.ai.google.genai.chat.options.model`,
`spring.ai.bedrock.converse.chat.options.model`,
`spring.ai.azure.openai.chat.options.deployment-name`,
`spring.ai.ollama.chat.options.model`). The first configured property in that
resolver order wins. OpenAI is first, so a leftover OpenAI model property
selects the OpenAI path.

## Live smoke

Mocked HTTP tests assert the OpenAI and native Mistral request bodies that
Spring AI sends. They do not call a provider.

One live OpenAI chat completion (model `gpt-4.1-mini`, named `tool_choice`
function `generateA2Ui`) returned HTTP 200 with a tool call of that name.
That call used the OpenAI HTTP API directly. It was not a dynamic compose
stream through this runtime.

Live dynamic compose was not run for Anthropic, Vertex, Google GenAI, Bedrock,
native Azure OpenAI, native Mistral, Groq, or Ollama: this environment had no
API keys for those providers. On Spring AI 1.1.0-M2 those direct clients
(except native Mistral) fail closed before a request, so a live call would
surface `TOOL_CHOICE_UNAVAILABLE` rather than a rendered surface. Native
Mistral would need a key to show `tool_choice: any` accepted by the API.

## Recipes

### OpenAI (default)

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4.1
          temperature: 0.0
```

Dependency: `spring-ai-openai-spring-boot-starter`.

### Groq (OpenAI-compatible base URL)

Showcase already accepts `GROQ_API_KEY` / `OPENAI_BASE_URL` overrides:

```bash
export GROQ_API_KEY=...
export OPENAI_BASE_URL=https://api.groq.com/openai
export OPENAI_MODEL=llama-3.3-70b-versatile
mvn -pl apps/be-transform-showcase spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=template"
```

The template profile is optional frozen-capability smoke; the showcase default is dynamic.

```yaml
spring:
  ai:
    openai:
      api-key: ${GROQ_API_KEY}
      base-url: https://api.groq.com/openai
      chat:
        options:
          model: llama-3.3-70b-versatile
          temperature: 0.0
```

Still uses the OpenAI Spring AI starter against Groq’s compatible API.

### Anthropic

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-sonnet-4-20250514
          temperature: 0.0
```

Smoke **template** mode or host `assemble` here. Dynamic compose fail-closes
on this Spring AI version (`TOOL_CHOICE_UNAVAILABLE`) because the Anthropic
client cannot force `generateA2Ui` / `renderA2Ui`.

### Vertex AI Gemini

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-vertex-ai-gemini-spring-boot-starter</artifactId>
</dependency>
```

Configure project/location/credentials per Spring AI Vertex docs, then:

```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          project-id: ${GCP_PROJECT}
          location: ${GCP_LOCATION:us-central1}
          chat:
            options:
              model: gemini-2.0-flash
              temperature: 0.0
```

Same limit as Anthropic: dynamic compose fail-closes
(`TOOL_CHOICE_UNAVAILABLE`). Vertex and Google GenAI ChatOptions in Spring AI
1.1.0-M2 do not expose a function-calling mode. Template mode or host
`assemble` still apply.

## Smoke checklist (alternate provider)

Use this before calling a non-OpenAI stack “production ready”:

1. [ ] Boot starts with the alternate starter / base-url on the classpath  
2. [ ] `POST /a2ui/surface/stream` with a **template** prompt returns `createSurface` / `updateComponents` / `done`  
3. [ ] With `lifecycle-events: true`, utilization events appear if expected  
4. [ ] `POST /a2ui/actions` with a known action (`approve` / `confirm`) returns ack messages  
5. [ ] Dynamic profile: one successful surface on a **forced** row of the matrix above. `TOOL_CHOICE_UNAVAILABLE` means this provider cannot force; stay on template or host `assemble`  
6. [ ] Actuator: `a2ui.template.rendered` or `a2ui.dynamic.surface.generated` increments ([ops](ops-and-diagnostics.md))

Showcase write-gate path: [Action round-trip](action-round-trip.md).

## Honest limits

| Topic | Reality |
|-------|---------|
| Golden path | OpenAI. OpenAI-compatible base URLs (Groq and the others in the matrix) stay on that path |
| Dynamic + force tool | Named OpenAI `tool_choice`, native Mistral `any`, fail-closed `TOOL_CHOICE_UNAVAILABLE` everywhere else in the matrix. A server on the OpenAI path still has to honor `tool_choice` (Kimi and other open models included) |
| Schema / JSON mode | `NONE` or `JSON_OBJECT` only. Dynamic mode resolves to `NONE`. There is no `JSON_SCHEMA` mode. |
| Cost / latency | Varies widely — budget in [ops](ops-and-diagnostics.md) |

## Next reading

* [Getting started](getting-started.md)  
* [Ops and diagnostics](ops-and-diagnostics.md)  
* [Golden-path cookbook](golden-path-cookbook.md)  
* [Dynamic generative UI](dynamic-generative-ui.md)  
