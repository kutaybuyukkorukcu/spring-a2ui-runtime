# Ops and diagnostics

How to operate a spring-a2ui host in production: fail-fast errors, metrics,
latency/cost, and what **not** to weaken.

Fail-fast policy is unchanged — invalid catalog output surfaces as SSE
`event: error` with diagnostics. There is **no** demo fallback surface and
**no** semantic repair of bad component props.

## Metrics (`a2ui.*`)

With Actuator + Micrometer (showcase enables `metrics` / `prometheus`):

| Meter | Meaning |
|-------|---------|
| `a2ui.dynamic.surface.generated` | Dynamic path produced a validated surface |
| `a2ui.dynamic.validation.failed` | Assembled messages failed catalog/schema validation |
| `a2ui.dynamic.validation.retry.success` | One bounded retry fixed validation |
| `a2ui.dynamic.validation.retry.failed` | Retry still invalid → fail-fast error |
| `a2ui.template.rendered` | Template rendered (`templateId` tag) |
| `a2ui.runtime.transform.success` / `.failure` | Stream transform outcomes |
| `a2ui.runtime.action.event` | Action ingress events |
| `a2ui.generation.context.chars` | Distribution of planner **static prefix** character length (digest + rules + examples + hard requirements) |

### Showcase example

```bash
# After a dynamic run (success or validation failure):
curl -s http://localhost:5001/actuator/metrics/a2ui.dynamic.validation.failed
curl -s http://localhost:5001/actuator/metrics/a2ui.dynamic.surface.generated
```

Alert on sustained `validation.failed` / `retry.failed` (prompt or catalog
mismatch), not on a single transient failure.

## Failure playbook (SSE `event: error`)

| Code | Typical cause | What to do |
|------|---------------|------------|
| `CONTENT_REQUIRED` | Empty `content` on stream | Send a non-blank prompt |
| `NO_COMPATIBLE_CATALOG` | Client `supportedCatalogIds` miss server catalogs | Align basic id or register host catalog ([registering catalogs](registering-catalogs.md)) |
| `A2UI_VALIDATION_FAILED` | Planner/tool output failed catalog validation after retry | Fix prompt/context; prefer host `assemble` for known trees, template mode second; check host catalog schemas |
| `TRANSFORM_FAILED` / `TRANSFORM_PARSE_FAILED` | Tool/orchestration or parse failure | Check logs (`com.kutaybuyukkorukcu.a2ui`), model tool-calling support |

Client rule: only pass A2UI JSON to your MessageProcessor; route utilization
events (`run*`, `assistantText`, `toolProgress`) to your own chrome
([utilization](native-sse-utilization.md)).

### Redaction

Do not log full prompts or data models with PII at INFO in production. Prefer
request ids (`X-A2UI-Request-Id`), error codes, and validation diagnostics.
DEBUG on `com.kutaybuyukkorukcu.a2ui` is fine for local showcase only.

## Latency, cost, and caching

Dynamic GenUI adds model latency (often hundreds of ms to seconds) and more
tokens than a text-only reply.

| Pattern | Guidance |
|---------|----------|
| Prefer **host `assemble`** when layout is known | No model call; deterministic fills |
| Prefer **dynamic** when this case’s tree is unknown | Budget for compose + one validation retry |
| Stream early | Clients should render progressive SSE envelopes |
| Cache | Cache **host domain results**, not invalid A2UI. Do not cache fail-open “repaired” surfaces |
| Semantic cache of surfaces | Optional at the host: same intent → reuse prior *validated* messages; never skip validation |

Do **not** reintroduce semantic repair to hide bad props — that fights fail-fast
and catalog safety.

## Deep trees vs flat adjacency

Nested component trees are where models drop required props. spring-a2ui uses
**flat adjacency lists** + catalog-derived tool constraints + strict validation.
Keep host catalogs shallow when possible; assemble known deep layouts in the
host ([authoring templates](authoring-templates.md) if you still use template mode).

## Next reading

* [Golden-path cookbook](golden-path-cookbook.md)  
* [Multi-provider Spring AI](multi-provider-spring-ai.md)  
* [REST API](../rest-api.md)  
* [Native SSE utilization](native-sse-utilization.md)  
