# Implementation Plan: Phase X — Migrating to A2UI v0.9

**Status:** Stub / high-priority planning — **do not implement until after the v0.8 runtime release.**
**Prerequisite:** Phase 2.5 complete; spring-a2ui v0.8 released (template + dynamic GA without semantic repair).
**Backlog:** [`BACKLOG.md`](../../BACKLOG.md) — Phase X section

---

## Why this plan exists now

Frontier A2UI (Google) explicitly moved from **structured-output-first (v0.8)** to **prompt-first + validate + retry (v0.9+)**. We capture that direction here so the v0.8 release does not paint us into a permanent BoundValue/repair world — while still shipping v0.8 first.

---

## Frontier direction (must carry into Phase X)

Google’s reliability loop:

```
Prompt → Generate → Validate
  → if invalid: structured VALIDATION_FAILED (surfaceId, path, message) back to LLM
  → self-correct
```

| Mechanism | Frontier intent | Our takeaway |
|-----------|-----------------|--------------|
| Prompt-first schema in context | LLM reads catalog/rules from prompt | Keep/improve catalog prompt injection; less reliance on deep structured-output nesting |
| Catalog / tool-time validation before client send | e.g. `SendA2uiToClientTool` | Continue Phase 2.5a/b pattern on v0.9 catalogs |
| `payload_fixer` / stream parser “heal” | **Syntax only** (truncated JSON, trailing commas, close braces) | Allow syntax healers; **never** reintroduce semantic repair (Card wrap, Button synthesize, alias renames) |
| `VALIDATION_FAILED` error format | Agent self-correction | Map our diagnostics to v0.9 client↔server error shape |

This aligns with Phase 2.5’s end state (thin assemble + strict validate + retry) and becomes easier on v0.9 because the **wire format is flatter**.

---

## What changes v0.8 → v0.9 (summary)

| Topic | v0.8 (current) | v0.9 |
|-------|----------------|------|
| Philosophy | Structured output / function calling friendly | Prompt-first / in-context schema |
| Component shape | `{"component": {"Text": {...}}}` | `"component": "Text"` + props |
| Bound values | `{literalString}`, `{path}`, … | Native JSON + path objects (simpler for LLMs) |
| Lifecycle | `surfaceUpdate`, `dataModelUpdate`, `beginRendering` | `createSurface`, `updateComponents`, `updateDataModel`, … |
| Data model | Typed `contents[]` entries | Plain JSON objects |
| Validation | Schema + our Phase 2.5 rigor | Protocol-native `VALIDATION_FAILED` feedback loop |
| Assembler need | Thin BoundValue assembler **required** | Much of the assembler **goes away** |

Sources: [v0.9 evolution guide](https://a2ui.org/specification/v0.9-evolution-guide/), [v0.9.1 protocol](https://a2ui.org/specification/v0.9.1-a2ui/).

---

## Goals (when Phase X starts)

1. Speak **A2UI v0.9** on the wire (and negotiate version with clients).
2. Keep reliability stack: **sanitize (syntax) + validate (catalog) + retry** — no semantic repair.
3. Shrink or delete `A2UiDynamicComponentNormalizer` / assembler where v0.9 flat format removes BoundValue translation.
4. Preserve Phase 1 template path OR migrate templates to v0.9 ops (decide in kickoff).
5. Keep **A2UI-native SSE** unless a separate decision adds an optional chat/interaction bridge.

---

## Proposed workstreams (outline only)

### X.1 — Protocol + models

- Add v0.9 message types / catalog resources.
- Version negotiation (`supportedCatalogIds` / protocol version).
- Dual-run or hard cutover strategy (prefer: feature flag `a2ui.protocol-version=v0.8|v0.9` during transition).

### X.2 — Validation + retry

- Port catalog schema validation to v0.9 basic catalog.
- Emit / consume standard `VALIDATION_FAILED` (`code`, `surfaceId`, `path`, `message`).
- Reuse bounded planner retry; align diagnostic text with protocol paths.

### X.3 — Generation path

- Update `renderA2Ui` / assembly to emit v0.9 operations.
- Replace BoundValue-heavy assembler with light sanitize (+ optional syntax healer for streaming).
- **Explicit ban:** no Card/Button/CheckBox semantic invent-and-fix rules.

### X.4 — Client / demo

- Upgrade `fe-a2ui-demo` / `@a2ui/react` (or v0.9 renderer) to consume v0.9.
- E2E: open-ended prompt → validated surface without repair.

### X.5 — Docs + release

- Migration guide for app developers (v0.8 → v0.9).
- Deprecation timeline for v0.8 wire support.

---

## Non-goals (for this stub)

- Implementing v0.9 before the v0.8 release.
- Adopting a generic agent↔app chat protocol as primary transport (separate decision).
- Reintroducing FogUI-style intermediate canonical models.

---

## Kickoff checklist (after v0.8 ships)

- [ ] Confirm client library version and v0.9 renderer readiness
- [ ] Decide dual-support window vs hard cutover
- [ ] Inventory Phase 2.5 assembler rules that become unnecessary on v0.9
- [ ] Expand this stub into a full task/PR-slice plan (same depth as Phase 2.5)

---

## References

- [A2UI v0.9 evolution guide](https://a2ui.org/specification/v0.9-evolution-guide/)
- [A2UI v0.9.1 protocol](https://a2ui.org/specification/v0.9.1-a2ui/)
- [Google Developers: A2UI v0.9](https://developers.googleblog.com/a2ui-v0-9-generative-ui/)
- Phase 2.5 (v0.8 reliability foundation): [`phase-2.5-scalable-dynamic-runtime.md`](phase-2.5-scalable-dynamic-runtime.md)
- Internal: two-hop dynamic orchestration (`generateA2Ui` / `renderA2Ui`) already shipped in Phase 2
