# ADR 005: Verification framework and merge gate

| Status | Proposed |
|--------|----------|
| Date | 2026-11-02 |
| Deciders | spring-a2ui maintainers |
| Complements | [ADR 001](001-streaming-surface-generation.md) (fail-fast on the stream) · [ADR 002](002-in-product-surfaces.md) (host owns writes and design system) · [ADR 004](004-action-and-function-identity.md) (ingress identity, idempotency, test strategy) |

This ADR decides how a change proves it is safe to merge, and what the owner reads instead of the diff. It does not change runtime behavior, the wire, or any public API. The [phased plan](../plans/verification-framework.md) schedules the work.

## Context

The owner reviews behavior summaries, not code line by line, and pays for model usage on a $20 Cursor plan. A merge must be low risk without a line-by-line read. Today the evidence for "safe" is mostly `mvn verify` plus whatever the authoring agent writes in the PR body.

### What `mvn verify` already covers

`mvn verify -B -ntp` on `cursor/ingress-dedupe-slot-guardrail-45a1` (PR #40) runs 387 tests with 1 skipped, in 35 to 50 seconds on a cloud agent.

| Module | Tests | Kinds of test |
|--------|------:|---------------|
| `a2ui-runtime-core` | 150 | Unit tests (JUnit 5, AssertJ). `A2UiV1ConformanceTest` validates the official v1.0 fixtures at pin `825beac` against the vendored schemas, plus hand-built `CallRendererFunction`, `AgentFunctionResponse`, and `CreateSurface` / `UpdateComponents` messages. |
| `a2ui-runtime-spring-starter` | 16 | Auto-configuration and policy unit tests. |
| `a2ui-runtime-spring-web-starter` | 187 | Spring integration tests over MockMvc with a Mockito `ChatModel` / `ChatClient` (`A2UiTestConfiguration`). `ForcedToolChoiceWireTest` captures the provider request body with a JDK `HttpServer`. `A2UiIngressIdempotencyTest` covers single-flight and the 30-second waiter timeout. `A2UiIngressDeduperReleaseTest` is a reflective check that no non-private deduper method can claim a slot without a work callback. |
| `a2ui-showcase-host` | 34 | `RuntimeSurfaceE2ETest` drives the showcase over MockMvc. `ArchitectureRulesTest` has three ArchUnit rules, all scoped to `com.kutaybuyukkorukcu.a2ui.showcase`. `V1WebRendererE2ETest` is disabled until a web v1.0 renderer exists (ADR 004). |

### What it missed

A large-model review of PR #39 found problems after `mvn verify` was green:

| Finding | Kind | Check that would have caught it |
|---------|------|---------------------------------|
| A thrown agent function left its dedupe slot in flight, so a retry waited on `join()` | Blocker | A concurrency test over every work outcome (return, throw), or a model of the slot states. PR #40 now makes the bad state hard to write (`runEvent` / `runFunction`). |
| The same idempotency key with a different event fingerprint; `functionCallId` dedupe across surfaces; a `rendererOnly` `@call` with no `catalogId` | Idempotency and function resolution | Tests derived one-to-one from the ADR 004 rules. The ADR listed the rules, and nothing checked that each rule had a test. |
| A host function args schema that `$ref`s `common_types.json#/$defs/DynamicString` did not resolve | Schema | A check that every `$ref` in vendored and contributed schemas resolves. |
| BACKLOG, README, migration guide, and upstream-issue wording that no longer matched the code or the upstream state | Docs accuracy | A check that names, codes, and versions in docs exist in code, and that upstream claims carry a date and a link. |

The repository has other gaps of the same kind:

- `.github/workflows/ci.yml` runs only for pull requests whose base is `main`. Stacked PRs #37, #38, #39, and #40 have no CI run. Only the owner's local `mvn verify` or the agent's own run covered them.
- CI runs `mvn test` and then `mvn verify`. There is no Failsafe split, so the second step runs every test again.
- `CONTRIBUTING.md` says "Formatting is enforced with Spotless." The Spotless plugin has no execution bound to a phase and CI does not call it. `mvn spotless:check` fails today on all 50 Java files in `a2ui-runtime-core`.
- `apps/fe-a2ui-demo` has an `eslint` script and no tests. CI does not build it.
- JaCoCo writes a report and enforces no threshold.
- There is no PR template. Each PR body has its own shape.

## Decision

### Verification gates merges; evaluation does not

**Verification** is a deterministic check with a pass or fail answer: the same input gives the same result, and a failure names the broken promise. Schema conformance, an HTTP status, a replayed stored result, and a dependency rule are verification.

**Evaluation** scores the quality of model output against a rubric. Examples are "did the composed surface fit the intent" and "did the planner pick sensible components". It needs a judge (a person or a model), and the same run can score differently twice.

The runtime's promise is verification-shaped. ADR 001 and ADR 002 promise catalog-bounded, validated, streamed, fail-fast surfaces in a product the host owns. Whether a surface is a good surface for that product depends on the host's catalog, prompt context, and model. The host owns that judgment.

Decisions:

1. Only verification gates a merge. No model-graded score blocks or allows a PR.
2. The runtime does not ship an evaluation layer. We do not add an LLM-as-judge, a quality rubric, or a scored benchmark.
3. Two places sit near evaluation, and both stay deterministic:
   - **Stored model responses.** Real provider tool-call arguments, recorded once and replayed in CI through the real `renderA2Ui` and assemble path. The model output is fixed, so the check is verification. PR #32 (open, based on `main`, v0.9.1 line) has a first corpus of ten fixtures.
   - **Live prompt smoke.** The runtime does own prompt text (`DynamicA2UiPromptProvider`) and the `renderA2Ui` tool schema. Changes to those can move the share of model output that fails validation. When a PR changes them, the owner may run a fixed set of intents against a live provider and report counts from the existing meters (`a2ui.dynamic.validation.failed`, `a2ui.dynamic.validation.retry.success`, `a2ui.dynamic.validation.retry.failed`). These are counts, not scores. The run is manual, uses the owner's API key, and never gates CI.
4. We revisit the second decision only if the runtime starts to own content that decides surface quality beyond the planner prompt and the tool schema.

### Verification layers

Each layer has one question it answers. A layer is listed even when it does not exist yet, so a PR can say which layers it touched.

| Layer | Question it answers | Today | Target |
|-------|---------------------|-------|--------|
| Unit | Does this class keep its contract for concrete inputs? | 150 core tests and most starter tests | Unchanged. New tests follow pstack `principle-test-behavior-not-implementation`. |
| Contract and schema conformance | Is every envelope we emit, and every envelope we accept, valid against the vendored A2UI spec at its pinned commit? | Official fixtures and hand-built messages in `A2UiV1ConformanceTest` | Every envelope that an integration test receives from `/a2ui/surface/stream` or `POST /a2ui/actions` is validated against `agent_to_renderer.json`. Every `$ref` in vendored and contributed catalog schemas resolves. |
| Decision traceability | Does each rule an ADR states have a test that would fail if the rule broke? | Implicit; ADR 004 lists rules and tests separately | New ADRs that state behavior rules list the test that covers each rule. The docs check confirms each named test exists. |
| Integration with mocked providers | Does the Spring wiring produce the right HTTP and SSE behavior when the model returns a known response? | MockMvc with a Mockito `ChatModel`; JDK `HttpServer` for provider wire bodies | Add the stored-response corpus on the `3.0.0` line. No live provider in CI. |
| Concurrency and properties | Does shared state stay correct under every outcome and interleaving we can generate? | Hand-written single-flight and timeout tests | Generated sequences of run, throw, join, timeout, and eviction against `A2UiIngressDeduper` and `A2UiPendingRendererCalls`. This lands before parallel two-hop generation adds a second thread to compose. |
| Formal verification (optional) | Can any order of steps break the dedupe promise, within stated bounds? | None | One small model of the dedupe slot states, generated with pstack `create-formal-verification-skill`. Run when its covered files change, not on every PR. |
| Guardrail lints | Does the build reject a pattern that caused a past bug? | Reflective deduper check; ArchUnit on showcase packages only | ArchUnit rules over `com.kutaybuyukkorukcu.a2ui.runtime..` from the showcase test module, which already has ArchUnit and depends on every library module. Each rule cites the bug or decision it encodes (per `.cursor/rules/guardrails.mdc`). |
| Docs accuracy | Do names, error codes, defaults, and versions in docs match the code? | Reviewer reading | A JUnit test that reads Markdown and fails on a backticked `A2Ui*` type, or an error code, that does not exist in code. Upstream claims carry a date and a link. |
| Real browser end-to-end | Does a real renderer in a real browser render our surface and post an action the backend accepts? | `V1WebRendererE2ETest` disabled | Stays gated on a web v1.0 renderer (ADR 004, confirmation 5). Until then, a cloud agent can record a browser proof only when the owner names `mvp-demo-host` / `mvp-demo-fe` (`.cursor/rules/work-lanes.mdc`). |

### What runs where, and what it costs

Cost has two parts. Compute is CPU time on a laptop, a cloud agent VM, or GitHub Actions. Standard GitHub-hosted runners are free for public repositories, and this repository is public. Model usage is what the $20 plan pays for. It grows with the size of the model and the amount of code the model reads.

| Layer | Owner laptop | Cloud agent | CI on every PR | Model usage |
|-------|--------------|-------------|----------------|-------------|
| Unit, contract, decision traceability, integration, guardrail lints, docs accuracy | `mvn verify` | `mvn verify` before every push | Yes, one `mvn verify` | None to run. Writing them is small-model work. |
| Concurrency and properties | Inside `mvn verify` | Inside `mvn verify` | Yes. Generated cases are capped so `mvn verify` stays near today's time. | None to run. Writing them is medium-model work. |
| Formal verification | Optional | When a PR touches covered files | Not at first. A later CI step may only flag that covered files changed. | None to run. Building and updating the model is medium-to-large model work. |
| Stored model responses | Inside `mvn verify` | Inside `mvn verify` | Yes | None in CI. Recording costs provider tokens once, on the owner's key. |
| Live prompt smoke | Owner's choice | Only with an owner-provided key | Never | Provider tokens on the owner's key. No Cursor model usage beyond starting the run. |
| Real browser end-to-end | Optional | Only when the owner names the demo apps | Later, path-filtered to FE and wire changes, once a v1.0 web renderer exists | Small model for a scripted run; screen recording review costs more. |
| Model review of the PR | Never required | See the review policy below | Never | The largest line item. Rationed by risk tier. |

### Merge gate checklist

A PR is ready for the owner when every line holds. The PR template and the merge-gate rule (see "What to create") carry this list.

1. **`mvn verify` is green on the PR head in CI**, not only on the author's machine. The PR body pastes `BUILD SUCCESS` and the test and skip counts per module, and explains any change in those counts.
2. **The PR body has a review guide** in the format below.
3. **Each bug fix names its guardrail**, per `.cursor/rules/guardrails.mdc`: the type or design change, the build-enforced check, or the `AGENTS.md` learned-rules line, in that order of preference.
4. **Each new test was seen failing before the fix.** The PR body pastes the failing output from the test run against the unfixed code, or from a probe that reintroduces the bug, as PR #40 did. A test that was never seen failing is not evidence.
5. **Docs match the code.** The docs-accuracy check passes. Any new claim about an upstream project (a release, an issue state, a renderer status) has a date and a link. A claim the author could not verify says so.
6. **The review that the risk tier calls for is done**, and each proven finding is fixed or deferred by the owner by name.
7. **Each decision listed for the owner has an answer.** An unanswered decision blocks the merge, even when CI is green.

### Review guide (required in every PR body)

The review guide is the part of the PR the owner reads. It replaces a line-by-line read for tier 0 and tier 1, and points a line-by-line read at the right files for tiers 2 and 3. It uses these headings, in this order:

```markdown
## Review guide

### Behavior changes
What a host, a renderer, or an operator can observe that is different after this PR.
One bullet per change, each written as "Before: ... After: ...". Say "None" for a docs-only or refactor PR.

### Decisions for the owner
Numbered questions with a recommended answer each. Only questions that change product
behavior, public API, wire format, dependencies, CI cost, or the docs' promises.
Say "None" when there are none.

### Read these files
Two or three files, each with one sentence on why it carries the risk.

### Evidence
- `mvn verify` result with per-module counts, and the count delta explained.
- The failing-before output for each new test.
- The guardrail for each fix.
- Anything run by hand (curl, browser recording) with the command or artifact.

### Not covered
What this PR does not prove, and which layer would.

### Risk tier
Tier 0 to 3, the reason, and the review that was done.
```

### Risk-tiered review policy

A large-model review is worth its cost when the change touches state that tests reach poorly, or a promise that a host cannot easily work around. The PR #39 blocker was concurrency in the ingress path. The idempotency and schema findings were contract rules. None of them were in leaf logic.

| Tier | Changes in it | Review |
|------|---------------|--------|
| 0 | Docs, plans, comments, CI wording, formatting only | No model review. The docs-accuracy check and the review guide are enough. |
| 1 | Logic inside one class or module, behind existing tests, with no change to the wire, public API, schemas, or shared state | The authoring agent runs pstack `blast-radius` on a small model and puts the one safety fact in the review guide. No separate reviewer. |
| 2 | Public API, wire shape, error codes, HTTP status, vendored schemas, catalog validation, auto-configuration defaults, or docs that state those contracts | One review pass by a medium model, scoped to the files in "Read these files" and the ADR the PR implements. Findings count only when a run proves them. |
| 3 | Shared mutable state, concurrency, idempotency, retries, timeouts, caches (`A2UiIngressDeduper`, `A2UiPendingRendererCalls`, `A2UiActionService`, and compose threading once parallel two-hop lands), security-relevant input handling, or a release cut | One large-model pass with pstack `interrogate` or `blast-radius`, scoped to the diff and its blast radius. Only findings proven by a run survive. When a fix round follows, the next review reads only the new delta. |

The tier is the highest tier any changed file falls into. The authoring agent proposes it in the review guide, and the owner can raise it.

To stay inside the $20 plan:

- Spend at most one large-model review per stack of PRs. Run it on the PR that carries the tier 3 change, not on every PR in the stack.
- Never re-review a whole stack after a fix. Review the fix commit.
- Do not run multi-model `arena` reviews on ordinary PRs. Keep them for a release cut, and only if the owner asks.
- Prefer cheap checks that remove a whole class of finding over reviews that find one instance. The docs-accuracy test and the schema `$ref` test are examples.

### How the framework stays current

| What | When | How |
|------|------|-----|
| Repo-local verification skill and its feature map | At each Maven Central release cut, and when a PR adds or removes an HTTP route or SSE event name | pstack `maintain-verification-skill`. Outcome is clean, changed, or blocked. A changed run ships one PR of corrections. |
| Formal model of the dedupe slots | When a PR touches a file in the model's coverage record, and at each release cut | pstack `maintain-formal-verification-skill`. The skill records a sync commit, so a later run can see which commits it has not checked. |
| Guardrail table in `AGENTS.md` | Whenever a bug is fixed or a reviewer finds one | pstack `correct`, per `.cursor/rules/guardrails.mdc`. Drop a rule once its mistake can no longer compile or pass the build. |
| This ADR's layer table | When a layer ships or is dropped | Edit the "Today" column in the same PR that changes it. |
| Performance claims (parallel two-hop is next) | Any PR that reports a speedup or a regression | pstack `benchmark-checklist` before the number goes in the PR body. |

### What to create

Repository-specific:

| Artifact | Path | Purpose |
|----------|------|---------|
| Merge-gate rule | `.cursor/rules/merge-gate.mdc` (`alwaysApply`) | Tells every agent the checklist, the review-guide headings, and how to pick a risk tier. |
| PR template | `.github/pull_request_template.md` | The review-guide headings, so a human-opened PR has the same shape. |
| CI trigger and steps | `.github/workflows/ci.yml` | Run on every pull request, whatever its base. Run one `mvn verify`. Later, a check that the PR body has the review-guide headings, and a path-filtered browser job. |
| Emitted-envelope conformance | Web starter test sources | A shared assertion that validates every envelope a test receives against the vendored `agent_to_renderer.json`, and a test that every schema `$ref` resolves. |
| Docs-accuracy test | Showcase test sources | Reads `README.md`, `docs/guides/`, and `docs/rest-api.md`. Fails on an unknown `A2Ui*` type or error code. Files that document removed names (migration guides, `CHANGELOG.md`, ADRs) are excluded or allow-listed by name. |
| Library ArchUnit rules | Showcase test sources, beside `ArchitectureRulesTest` | Rules over the runtime packages. Candidates to confirm against the code first: core has no Spring dependency; the web starter does not depend on the showcase. |
| Concurrency and property tests | Web starter test sources | Generated outcome sequences for the dedupe and pending-call maps. |
| Repo-local verification skill | `.cursor/skills/verify-spring-a2ui/` | Generated with pstack `create-verification-skill`. Drives the showcase over HTTP and SSE and records evidence. |
| Formal verification skill | `.cursor/skills/verify-ingress-dedupe/` | Generated with pstack `create-formal-verification-skill`. Model of `A2UiIngressDeduper` slot states. |

Portable to other repositories as a user-level skill, once three PRs in this repository have used it:

- The verification-versus-evaluation rule.
- The merge gate checklist and the review-guide headings.
- The risk-tier table, with the tier 3 file list replaced by a per-repository list.
- The docs-accuracy method (backticked names must resolve; upstream claims need a date and a link). The scanner itself is repository-specific.

The repo-local skills, the ArchUnit rules, the conformance tests, and the CI file stay in this repository. Their content depends on this runtime's types, routes, and pinned spec.

## Alternatives rejected

**An LLM-as-judge evaluation suite in CI.** It costs model usage on every run, it can flip between runs on the same commit, and it scores something the host owns. A flaky gate trains everyone to rerun until green.

**A large-model review on every PR.** It does not fit the plan, and most PRs are tier 0 or tier 1. Cheap checks that remove a class of finding pay back on every later PR.

**Rely on `mvn verify` alone.** PR #39 had 372 green tests when the review found the blocker, four contract findings, and five docs findings.

**Mutation testing (for example PIT) as a gate.** It answers "would a test notice this change" well, but each mutant reruns tests, which is costly with Spring contexts, and it needs a new plugin. The failing-before rule in the merge gate answers the same question for the tests a PR adds, at no compute cost. We may revisit mutation testing for `a2ui-runtime-core` only.

**A coverage threshold.** Line coverage says a line ran, not that an assertion would fail when it breaks. JaCoCo stays a report.

**Formal verification on every PR.** The model is useful when its covered files change. Running it otherwise spends time on a fixed answer.

**Browser end-to-end on the v0.9 demo against the v1.0 backend.** ADR 004 already rejects `@a2ui/react/v0_9` against v1.0 envelopes as a passing end-to-end.

## Consequences

- Every PR body gets longer by one section. The owner reads that section instead of the diff for tiers 0 and 1.
- Stacked PRs get CI. A stack's base branch must build on its own.
- Dropping the extra `mvn test` step removes the second full test run in CI. No job depends on the `test` step alone today.
- New dependencies need owner approval: a property-testing library (for example jqwik), a model checker (for example TLA+ tools), and a browser driver (for example Playwright for Java). Each is test-scoped or a local tool, and none ships in a published module.
- The Spotless claim in `CONTRIBUTING.md` is false today. Either the claim changes, or one formatting PR reformats the code and binds `spotless:check` to `verify`. A mass reformat conflicts with every open PR, so it waits until the open stack merges.
- Prompt or tool-schema changes can still lower model output quality without failing any gate. The live prompt smoke is the owner's optional check for that.

## Confirmation

The owner should confirm these before the first implementation PR merges:

1. Only deterministic verification gates merges. The runtime ships no evaluation layer. The live prompt smoke is manual, optional, and reports meter counts only.
2. CI runs on every pull request, whatever its base, and runs `mvn verify` once.
3. The review guide is required in every PR body, and a CI check enforces its headings after the template has been used for a while.
4. The risk tiers and the review budget: no model review for tier 0, self-review for tier 1, one medium pass for tier 2, and at most one large-model pass per stack for tier 3.
5. The failing-before rule: each new test shows its failing output before the fix, in the PR body.
6. Spotless: fix the `CONTRIBUTING.md` claim now, and reformat plus enforce after the open stack merges. The other option is to drop Spotless.
7. New test-scoped dependencies or local tools, each approved in its own PR: jqwik for property tests, TLA+ tools for the dedupe model, and Playwright for Java once a v1.0 web renderer exists.
8. Stored model responses on the `3.0.0` line: rebase PR #32 or record a new corpus against v1.0. Recording needs the owner's provider key.
9. The showcase gains a keyless profile with a scripted model, so the repo-local verification skill can drive it without an API key. This touches `apps/be-transform-showcase`, not a library module.

## References

- [ADR 001](001-streaming-surface-generation.md), [ADR 002](002-in-product-surfaces.md), [ADR 004](004-action-and-function-identity.md)
- [Phased plan](../plans/verification-framework.md)
- [BACKLOG: Verification framework](../../BACKLOG.md#next--verification-framework)
- `.cursor/rules/guardrails.mdc`, `.cursor/rules/work-lanes.mdc`, `AGENTS.md` (learned rules)
- pstack skills: `create-verification-skill`, `maintain-verification-skill`, `create-formal-verification-skill`, `maintain-formal-verification-skill`, `correct`, `blast-radius`, `interrogate`, `principle-prove-it-works`, `principle-test-behavior-not-implementation`, `benchmark-checklist`
