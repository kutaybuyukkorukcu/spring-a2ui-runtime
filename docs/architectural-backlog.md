# Architectural backlog (later)

Deferred **module** deepening for `packages/`. This is not the product execution order in [`BACKLOG.md`](../BACKLOG.md), not an in-flight [phase plan](plans/README.md), and not a new ADR.

**Status:** later — do not start while [A2UI v1.0 actions / functions](../BACKLOG.md) is Next, unless a later note reorders.  
**Scope:** `packages/` only. `apps/` stays out.  
**Snapshot:** `d7e8958` (2026-09-20). Two independent architecture reviews of the runtime; this file is the durable summary (the HTML reports were ephemeral).

Callers should get **leverage**; maintainers should get **locality**. Vocabulary here is the design glossary (**module**, **interface**, **depth**, **seam**, **adapter**) plus [CONTEXT.md](../CONTEXT.md) (**runtime**, **surface**, **compose**, **assemble**).

Does **not** reopen [ADR 001](adr/001-streaming-surface-generation.md), [ADR 002](adr/002-in-product-surfaces.md), or [ADR 003](adr/003-catalog-scoped-fail-fast.md): dual generation modes stay; template stays frozen; dynamic stays gravity; native SSE and fail-fast stay; fail-fast stays **this catalog**; one compose **module** and assembly-owns-validation stay. Several items below **implement** ADR 003’s “Not in this ADR” list rather than reversing it.

Product Later slices (parallel two-hop, host compose tools, dynamic tool forcing beyond OpenAI, provider prompt cache) stay in [`BACKLOG.md`](../BACKLOG.md). This file is **depth in `packages/`**, not those slices.

---

## Agreed friction

Both reviews named the same two leaked **seams**:

1. Host **assemble** and model **compose** share one Maven artifact (`a2ui-runtime-spring-web-starter`). Assemble-only builders transitively pull Spring AI (ChatClient, tools, prompts). Deleting compose also deletes the artifact that exposes assemble.
2. Fail-fast policy is copied: reject unknown actions, apply `A2UiSurfaceBuffer`, catalog-validate, reject hidden types. Host assemble, dynamic assemble, and (one review) the action path each own a copy. A new governance rule is shotgun surgery.

---

## Suggested order (when started)

The reviews agreed on the problems and disagreed on **first cut**. Use this order when the track is opened:

1. **Split host assemble from model compose** (Maven + auto-configuration **seam**). This is what OSS product builders see in the dependency graph. It realizes ADR 003 later work: catalog wiring out of the AI starter; Spring AI hop **adapter** behind the existing generation-mode **seam**.
2. **In the same change, or immediately after, deepen one assembly-policy module** so template assemble, dynamic assemble, and action responses share fail-fast ordering. Do not leave two policy pipelines after the artifact split.

Do not start the hop **adapter** by bumping Spring AI / Boot 4 first ([ADR 003](adr/003-catalog-scoped-fail-fast.md)).

---

## Items

Strength is from the reviews: **Strong** / **Worth exploring** / **Speculative**. “Both” means both reviews named the same deepening.

| Strength | Item | Files (indicative) | Problem | Later change | Why it waits |
|----------|------|--------------------|---------|--------------|--------------|
| Strong (both) | Separate host assemble from model compose | `a2ui-runtime-spring-web-starter/pom.xml` · `A2UiWebAutoConfiguration` · `A2UiSurfaceAssemblyService` · `SpringAiSurfaceRuntime` · generation **adapters** | The web-starter **interface** is shallow: known-tree, no-model assemble still accepts ChatClient, tools, and prompts. One auto-configuration class wires HTTP, assemble, actions, catalogs, and compose. | A Spring web/assemble **module** owns catalogs, validation, actions, host assemble, and transport. Model compose sits in a separate **adapter** **module** behind the generation-mode **seam**. | ADR 003 later work. Maven graph is the first thing builders inspect; not the current Next. |
| Strong (both) | Deepen one assembly-policy **module** | `A2UiSurfaceAssemblyService` · `A2UiDynamicAssemblyService` · `A2UiActionService` · `A2UiActionAllowList` · `A2UiComponentVisibility` | Two (three) orchestrators expose the same fail-fast sequence. **Interface** ≈ **implementation**; **locality** is gone. Deleting one path still leaves the policy in the other. | Path-specific normalization stays in each **adapter**. One internal **module** owns root integrity, action allow-list, catalog validation, visibility, diagnostics, and metrics. | Cheapest **locality** win; fold into the assemble/compose split so the new **modules** do not copy the leak. |
| Strong | Extract Spring AI hop **adapter** | `DynamicGenerationAdapter` · `A2UiDynamicTools` · `SpringAiSurfaceRuntime` · `A2UiWebAutoConfiguration` | Spring AI types (ChatClient, ToolCallback, forced tool-choice) leak across the generation-mode **seam**. Hop retry and tool-choice quirks have no single home. | One **adapter** hides the two-hop planner behind a narrow compose **interface**. Web-starter wires it; core stays framework-agnostic. A second **adapter** (in-memory test) would make the **seam** real. | Same ADR 003 later bullet as Boot 4. Blocked on hiding ChatClient, not on a BOM bump. |
| Worth exploring | Split catalog wiring from the AI starter | `A2UiCatalogRegistry` · `A2UiRequestCatalogNegotiator` · `A2UiCatalogService` · web-starter `pom.xml` | Catalog negotiation lives beside compose tooling. Assemble-only hosts still pull the full web-starter. | Registry + negotiation in a thin **module** between core and starters; web-starter keeps HTTP. | Usually the same change as “separate assemble from compose,” not a third artifact for its own sake. |
| Worth exploring | Move planner rendering to the compose **adapter** | `A2UiCatalogRegistry` · `CoreCatalogContributor` · `DynamicA2UiPromptProvider` | Core catalog registry mixes wire truth (load, merge, validate) with planner digest / prompt text. Deleting dynamic compose would leave `renderPlannerDigest` in core — the compose **seam** leaks inward. | Keep catalog facts and catalog-scoped validation in core (ADR 003 wire hygiene). Move digest formatting — and maybe prompt-only examples/rules — into the dynamic compose **adapter**. Confirm hosts do not need rules text on the assemble path first. | Do not move validation or `$ref` inlining out of core. |
| Worth exploring | Unify governance policy **modules** | `starter/policy/*` · `webstarter/policy/*` · `PolicyContributor` · `A2UiActionService` | Generation policy (ChatOptions) and surface/action policy are shallow **modules**; adding a rule touches several enforcement sites. | One governance **module** with three **seams**: generation options, surface visibility, action confirmation — each with one enforcement point. Do not extend `A2UiGenerationPolicy` for application rules ([platform.md](platform.md#generate-govern-execute)). | Orthogonal to the assemble/compose split; schedule only if a new rule still requires shotgun edits. |
| Worth exploring | Collapse the prompt contributor graph | `prompt/*` (eleven classes) · `DynamicA2UiPromptProvider` · `A2UiGenerationContextFactory` | Understanding what the dynamic planner sees means bouncing across many shallow **modules**. | Deepen `DynamicA2UiPromptProvider`: catalog, policy, and actions as internal sections. Keep a contributor **seam** only where hosts extend. | Compose-side cleanup; do not block the artifact split. |
| Speculative | Delete `A2UiSurfaceService` | `A2UiSurfaceService` · `A2UiStreamController` | Thin **module** between controller and runtime. | Fold lifecycle into the compose **interface**; controller calls compose directly. | **Deletion test failed:** the type owns content validation and RunStarted / RunFinished wrapping, not pure delegation. Revisit only if the stream façade grows. |

---

## Quality snapshot (that review)

`HEAD` matched `main` on `packages/` (`d7e8958`). There was no code-review **diff** to score. Baseline smells on current code (judgement, not a merge gate):

- **Duplicated code / shotgun surgery** — the fail-fast pipeline in the two assembly **modules** (and action path).
- **Middle man (weak)** — `A2UiSurfaceService` looks pass-through; deletion test says keep it.
- **Speculative generality** — constructor overload chains on the assembly **modules**.

Package tests were green on that snapshot. Counts from the two reviews were not reconciled (133 vs 277); rerun `mvn test` on `packages/` if you need a single number.

---

## When a later explorer reopens this

- Prefer deepening the two **Strong (both)** rows before inventing a new **seam**.
- If a candidate contradicts an ADR, reopen the ADR explicitly; do not silently reverse catalog-scoped fail-fast or one compose **module**.
- Concrete Java **interfaces** are not specified here on purpose. Design them when the track starts.
