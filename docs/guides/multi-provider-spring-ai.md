# Multi-provider Spring AI

OpenAI remains the **golden path** for getting started. spring-a2ui uses Spring
AI `ChatClient` + tools; other providers work when Spring AI supports them and
tool calling is reliable enough for template/dynamic modes.

Getting started stays OpenAI-default: [Getting started](getting-started.md).

## What the runtime already does

Provider-aware chat option customizers exist for OpenAI, Anthropic, and Vertex
AI Gemini (classpath-conditional in `a2ui-runtime-spring-starter`). Deterministic
temperature / top-p / etc. are applied via advisors when enabled.

Forced primary-tool choice on dynamic mode is **OpenAI-shaped** today. Other
providers may still run tools but without the same force-tool guarantee —
prefer **template mode** or verify tool calling before relying on dynamic
compose in production.

## Recipes

### OpenAI (default)

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4.1-nano
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

Smoke **template** mode first. Confirm tool calling works for your Spring AI
version before enabling dynamic in production.

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

Same caution: template first; validate tools for dynamic.

## Smoke checklist (alternate provider)

Use this before calling a non-OpenAI stack “production ready”:

1. [ ] Boot starts with the alternate starter / base-url on the classpath  
2. [ ] `POST /a2ui/surface/stream` with a **template** prompt returns `createSurface` / `updateComponents` / `done`  
3. [ ] With `lifecycle-events: true`, utilization events appear if expected  
4. [ ] `POST /a2ui/actions` with a known action (`approve` / `confirm`) returns ack messages  
5. [ ] (Optional) Dynamic profile: one successful surface; if tools flake, stay on template  
6. [ ] Actuator: `a2ui.template.rendered` or `a2ui.dynamic.surface.generated` increments ([ops](ops-and-diagnostics.md))

Showcase HITL path: [Action round-trip](action-round-trip.md).

## Honest limits

| Topic | Reality |
|-------|---------|
| Golden path | OpenAI (+ Groq-compatible OpenAI API) |
| Dynamic + force tool | Best on OpenAI-compatible options today |
| Schema / JSON mode | Provider-specific; do not assume `JSON_SCHEMA` everywhere |
| Cost / latency | Varies widely — budget in [ops](ops-and-diagnostics.md) |

## Next reading

* [Getting started](getting-started.md)  
* [Ops and diagnostics](ops-and-diagnostics.md)  
* [Golden-path cookbook](golden-path-cookbook.md)  
* [Dynamic generative UI](dynamic-generative-ui.md)  
