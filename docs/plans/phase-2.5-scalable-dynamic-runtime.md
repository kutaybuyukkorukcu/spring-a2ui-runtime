# Implementation Plan: Phase 2.5 — Scalable Dynamic Runtime

**Prerequisite:** Phase 2 complete (dynamic two-hop tools + assembly).
**Backlog:** [`BACKLOG.md`](../../BACKLOG.md) — Phase 2.5 section
**Branch:** `feat/scalable-dynamic-runtime` (or continue current feature branch until cut).
**Release gate:** Dynamic mode must **not** GA for the v0.8 runtime release until 2.5a–2.5c are done. Template mode can ship without waiting on repair deletion.

---

## Problem statement

Phase 2 dynamic mode works end-to-end but has a critical reliability gap: the server historically validated component **type names** but not component **properties**. The LLM can emit `CheckBox` with `label` but missing required `value`, and a **repair normalizer** silently patches some LLM mistakes (`checked→value`, Button label→Text child, Card multi-child wrapping). Every new shorthand pattern needs a new Java rule. That does not scale and must not ship as GA.

**Example bug:** LLM emits `CheckBox.label` as `{path: "..."}` but omits `CheckBox.value`. Neither repair nor weak validation catches it → client (`@a2ui/react`) rejects.

---

## Goal

Production-grade dynamic mode:

1. **Strict server validation** against full v0.8 catalog schemas (same rigor as client).
2. **Upstream tool schema** constraints for `renderA2Ui`.
3. **Delete semantic repair** — keep only a thin v0.8 assembler (flat args → BoundValue adjacency).
4. **Bounded retry** on validation failure (already implemented) becomes the correction path.

Target reliability stack (frontier-aligned): **light sanitize + thin assemble + strict validate + retry**.

---

## Current implementation status (working tree)

| Slice | Status | Notes |
|-------|--------|-------|
| **2.5a** | ✅ | Catalog schema validator + assembly rejection tests (CheckBox/Button/Card). |
| **2.5b** | ✅ | Catalog prop shapes + BoundValue shorthand + `additionalProperties: false` + callback test. |
| **2.5c** | ✅ | Semantic repair deleted; thin assembler only. |
| **2.5d** | ✅ | Counters wired; `A2UiRuntimeMetricsTest` + showcase actuator `GET /actuator/metrics/a2ui.dynamic.validation.failed`. |

### Implementation note (deviation from original draft)

Prop validation uses a dedicated **`A2UiCatalogSchemaValidator`** (JSON Schema via `com.networknt:json-schema-validator`) instead of hand-written per-prop branches inside `A2UiMessageValidator`. Functionally correct; keep this approach.

---

## Architecture

### Current (Phase 2 + partial 2.5)

```
LLM renderA2Ui(components=[...])  ← partial tool schema
  → sanitize (drop bad entries)
  → A2UiDynamicComponentNormalizer
      → REPAIR (Card wrap, Button synthesize, CheckBox checked→value, …)  ← REMOVE
      → canonicalize (BoundValue, children shapes, …)                     ← KEEP
  → assemble messages + buffer
  → A2UiMessageValidator (+ catalog prop schema when context set)
  → on fail: bounded retry with diagnostics
  → SSE
```

### Target (Phase 2.5 complete)

```
LLM renderA2Ui(components=[...])  ← catalog-derived JSON Schema
  → sanitize (drop missing id/component; unstringify quirks)
  → thin assembler (flat → v0.8 BoundValue adjacency ONLY)
  → A2UiMessageValidator + A2UiCatalogSchemaValidator
  → on fail: bounded retry (diagnostics → planner) — never silent repair
  → SSE (client-safe envelopes only)
```

---

## Gap analysis

| Gap | Current | Target |
|-----|---------|--------|
| Prop validation | Catalog schema validator present | Hard fail before SSE; assembly tests prove it |
| Tool schema | Required props stub | Prop shapes + `additionalProperties: false` where providers allow |
| Normalizer | Repair + canonicalize mixed | **Repair deleted**; canonicalize/assembler only |
| Release | Dynamic demo-grade | Dynamic GA only after repair deletion |

---

## Tasks

### 2.5a — Catalog property validation (finish)

#### Done

- Enrich `A2UiCatalogRegistry` with `componentSchema` / `requiredProps` / `allowedProps` / …
- Error codes: `MISSING_REQUIRED_PROP`, `UNKNOWN_PROP`, `INVALID_BOUND_VALUE`, `INVALID_ENUM_VALUE`, `INVALID_CHILD_SHAPE`, `INVALID_ACTION_SHAPE`, `INVALID_PROP_TYPE`
- `A2UiCatalogSchemaValidator` + wire from `A2UiMessageValidator` when catalog context present
- Assembly services pass `A2UiValidationContext.forCatalog(catalogId)`
- CheckBox-missing-`value` unit test in `A2UiMessageValidatorTest`

#### Remaining

1. ~~`A2UiDynamicAssemblyServiceTest`: assemble CheckBox label-only → `A2UI_VALIDATION_FAILED`~~ ✅
2. Optional: dedicated `A2UiCatalogSchemaValidatorTest` for error-code mapping.
3. Optional: refine `mapErrorCode()` so child/action shapes emit `INVALID_CHILD_SHAPE` / `INVALID_ACTION_SHAPE` when appropriate.

**Acceptance:** No server-emitted envelope the client would reject for catalog prop violations.

---

### 2.5b — Strict `renderA2Ui` tool JSON Schema (finish)

#### Done

- `A2UiToolSchemaGenerator` + tests
- `A2UiDynamicTools.buildRenderA2UiToolCallback()` registers `inputSchema`

#### Remaining

1. Embed catalog `properties` per component; `additionalProperties: false` on component props.
2. Constrain enums; allow LLM-friendly BoundValue **shorthand unions** (string \| `{literalString}` \| `{path}`) so thin assembler still has room to canonicalize.
3. Prefer blocking aliases like `checked` / `variant` at schema level (or let validation reject them post-normalize).
4. Test that tool callback embeds generated schema JSON.

**Acceptance:** Fewer invalid tool args in practice; `checked`-only CheckBox either blocked upstream or fails validation → retry (never repaired).

---

### 2.5c — Delete semantic repair; keep thin assembler ⚠️ release-critical

**This is not “freeze growth.” Delete repair code.**

#### Delete (semantic repair)

| Method / behavior | Why delete |
|-------------------|------------|
| `fixCardComponent` | Invents Column wrappers |
| `fixButtonComponent` | Invents Text child + action |
| `fixCheckBoxComponent` | Renames `checked` → `value` |
| `fixTextComponent` (`variant`→`usageHint`) | Alias repair; prefer schema + fail |
| Inline items hoisting as structural invent | Schema/retry instead |

Remove `enforceCatalogConstraints` switch that calls the above, or delete the methods and leave canonicalize-only path.

#### Keep (thin v0.8 assembler)

- Flat string type → `{Type: {...}}`
- BoundValue shorthand coercion
- Bare `children` list → `{explicitList}`
- Action string → `{name}`
- Child id string coercion
- Tab/option/context BoundValue normalization
- Path `/` prefix normalization
- List `data` → `template.dataBinding` **only if** treated as documented canonicalization of a valid equivalent form (if ambiguous, prefer fail + retry)
- Drop entries missing `id`/`component`
- Duplicate ID / DAG validation (**fail**, do not invent nodes)

Optional rename: `A2UiDynamicComponentNormalizer` → `A2UiDynamicComponentAssembler` once repairs are gone.

#### Tests (hard acceptance)

Rewrite / add:

1. Invalid CheckBox (no `value`) → assembly or validator failure → retry path; **not** patched via `checked`.
2. Button with only `label` (no `child`/`action`) → failure, not synthesized Text.
3. Card with multi `children` and no `child` → failure, not Column wrapper.
4. Remove or invert normalizer tests that assert repair behavior (`shouldMapCheckBoxCheckedToValueBoundValue`, Button label remodeling, Card wrap).
5. Happy-path assembly fixtures must supply **catalog-valid** args.

**Acceptance checklist:**

- [ ] Repair methods gone from production path
- [ ] Invalid LLM output fails / retries, never silently repaired
- [ ] Thin assembler still produces valid v0.8 wire from shorthand BoundValues
- [ ] Phase 1 template tests green (unaffected)
- [ ] Dynamic path green for valid fixtures

---

### 2.5d — Metrics verification ✅

#### Done

- Micrometer counters + `A2UiDynamicTools` call sites
- `A2UiRuntimeMetricsTest` (`SimpleMeterRegistry` counter assertions)
- Showcase E2E: record `validation.failed` → `GET /actuator/metrics/a2ui.dynamic.validation.failed` ≥ 1

---

## Suggested implementation order (continue from here)

```
Finish 2.5a remaining tests
  → Tighten 2.5b schema depth
    → 2.5c DELETE repairs + rewrite tests   ← do before dynamic GA
      → 2.5d actuator verification
        → Update BACKLOG checkboxes / cut v0.8 release notes
```

**PR slice A:** finish 2.5a tests + tighten 2.5b.  
**PR slice B (release-critical):** 2.5c repair deletion + test rewrites.  
**PR slice C:** 2.5d actuator verification + docs.

---

## Non-goals

- **A2UI v0.9 migration** — tracked separately in [`phase-x-migrating-to-v0.9.md`](phase-x-migrating-to-v0.9.md); start **after** v0.8 release.
- **Alternate agent chat/event transports** — out of scope for v0.8 (A2UI-native SSE remains).
- **Client-side Zod catalog generation** — server validates from the same catalog JSON the client uses.
- **Semantic repair feature flags** — prefer hard delete over dual-path maintenance.

---

## Definition of done

- [x] Catalog registry exposes component prop schemas
- [x] Prop-level catalog validation exists (with catalog context)
- [x] New validation error codes defined
- [x] CheckBox missing `value` fails validator unit test
- [x] Tool schema generator + programmatic registration exist (depth still to tighten)
- [x] Dynamic validation metrics wired
- [x] Tool schema constrains prop shapes / unknown props sufficiently for GA
- [x] **Semantic repair methods deleted**
- [x] Tests prove invalid shapes fail/retry (not repair)
- [x] Assembly rejection test for invalid CheckBox
- [x] Actuator metrics verified
- [ ] All Phase 1 + Phase 2 regression tests green
- [x] BACKLOG Phase 2.5 marked complete; dynamic GA allowed

---

## References

- [`BACKLOG.md`](../../BACKLOG.md)
- [`docs/plans/phase-2-dynamic-generative-ui.md`](phase-2-dynamic-generative-ui.md)
- [`docs/guides/dynamic-generative-ui.md`](../guides/dynamic-generative-ui.md)
- [`docs/plans/phase-x-migrating-to-v0.9.md`](phase-x-migrating-to-v0.9.md)
- [A2UI v0.8](https://a2ui.org/) · [standard-v0.8.json](../../packages/a2ui-runtime-core/src/main/resources/META-INF/a2ui/catalogs/standard-v0.8.json)
- Related direction: A2UI v0.9 prompt→validate→retry (syntax heal only) — see Phase X
