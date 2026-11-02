# Agent notes

Guidance for automated agents working in this repository.

## Learned rules

| Rule | Enforcement |
|------|-------------|
| Every path that claims a dedupe slot must complete or abandon it; covered by `runEvent` / `runFunction`. | `A2UiIngressDeduper.runEvent` and `runFunction` (work lambda, release in finally); `A2UiIngressDeduperReleaseTest`. |
