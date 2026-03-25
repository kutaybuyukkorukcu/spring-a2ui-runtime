# FogUI OSS Roadmap

> Active backlog for OSS deterministic compatibility and rendering trust layer.

## Current OSS Scope

1. Canonical contract + validation (`fogui-java-core`).
2. Spring integration glue (`fogui-spring-starter`).
3. React adapter rendering SDK (`@fogui/react`).
4. Reference server + minimal demo for integration verification.

Commercial/cloud features are intentionally tracked separately in `docs/ROADMAP_CLOUD.md`.

---

## ✅ Completed Foundations

- [x] Canonical response model (`GenerativeUIResponse`, blocks, thinking, metadata).
- [x] Deterministic canonical validator.
- [x] A2UI inbound translation endpoint and translator.
- [x] Streaming parser + reconciliation helpers.
- [x] React SDK core primitives (`FogUIProvider`, `useFogUI`, `FogUIRenderer`, adapters).
- [x] Transform + stream reference APIs in `backend-java`.

---

## 🔥 Near-Term OSS Priorities

### Contract and Compatibility

- [ ] Add explicit version negotiation and compatibility checks in canonical contract tooling.
- [ ] Expand canonical validation errors with stable machine-readable error codes and docs.
- [ ] Add protocol conformance test fixtures for valid/invalid payload sets.

### Deterministic Streaming

- [ ] Strengthen patch reconciliation semantics for partial/late/out-of-order stream fragments.
- [ ] Add deterministic replay tests (same input stream -> same final snapshot).
- [ ] Add reference integration tests for SSE stream lifecycle (`result`, `usage`, `error`, `done`).

### Design-System Integration

- [ ] Complete shadcn adapter coverage (Table/List/Stack/Grid + tested mappings).
- [ ] Add adapter conformance checks for missing component mappings and prop transforms.
- [ ] Provide compact adapter templates for enterprise design systems.

### OSS DX and Packaging

- [ ] Publish Java artifacts for `fogui-java-core` and `fogui-spring-starter`.
- [ ] Add docs for consuming Java artifacts in external Spring Boot services.
- [ ] Add a minimal cross-stack quickstart showing protocol translation + renderer integration.

### Observability and Reliability

- [ ] Add reference metrics and tracing hooks (validation failures, stream reconciliation failures, latency).
- [ ] Add structured error envelopes for compatibility endpoints.
- [ ] Add runbook docs for common integration failures.

---

## 📦 Medium-Term OSS Priorities

- [ ] CLI scaffolding for adapter starter templates (`@fogui/cli`).
- [ ] Conformance test runner for contract + adapter compatibility checks.
- [ ] Community adapter gallery and validation harness.
- [ ] Additional protocol bridges beyond A2UI as ecosystem demand becomes concrete.

---

## 📊 OSS Success Indicators

- [ ] New contributors can identify core/reference/archived modules in under 5 minutes.
- [ ] Stable, versioned Java artifact releases are consumable externally.
- [ ] Deterministic stream/conformance tests are passing in CI.
- [ ] React SDK integrations can render canonical payloads with adapter coverage >= 80%.

---

**Last Updated:** March 25, 2026
