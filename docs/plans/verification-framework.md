# Verification framework

**Status:** proposed, not started (design in [ADR 005](../adr/005-verification-framework.md))
**Branch:** design on `cursor/verification-framework-adr-8c69` (stacked on PR #40, `cursor/ingress-dedupe-slot-guardrail-45a1`). Each step below is its own PR.
**Backlog:** [Verification framework](../../BACKLOG.md#next--verification-framework)

## In

- The layers, merge gate, review guide, and risk tiers in ADR 005, built as small PRs.
- Each PR changes at most about 400 lines, excluding recorded fixture data.
- Each PR's body uses the ADR 005 review guide, starting with this one.

## Out

- An evaluation layer, an LLM-as-judge, or scored benchmarks (ADR 005 decision 1).
- Runtime behavior, wire, or public API changes. A layer that finds a product bug reports it, and the fix is a separate PR with its own guardrail.
- Mutation testing and coverage thresholds.
- Browser end-to-end before a web v1.0 renderer exists (ADR 004, confirmation 5).

## Constraints

- No new dependency without owner approval in that PR's "Decisions for the owner".
- `mvn verify` stays near its current time (35 to 50 seconds on a cloud agent). A layer that adds more than 30 seconds says so in its review guide.
- `mvp-demo-host` and `mvp-demo-fe` run only when the owner names them (`.cursor/rules/work-lanes.mdc`).
- Model sizes follow `.cursor/rules/work-lanes.mdc`. **Small** means mechanical work for `composer-2.5`. **Medium** means the implementer is still `composer-2.5`, but what to assert is settled on the design lane first.
- Cost risk is about model usage on the $20 plan. CI minutes are free on this public repository.

## Order

Steps are ordered by risk removed per unit of cost. Step 1 bases on `main`, so it can merge without the open stack. Steps 2 to 12 stack on this design PR, because they test code that exists only on the `3.0.0` line.

### 1. Run CI on every pull request

- **Goal:** stacked PRs get a CI run. Today `ci.yml` filters `pull_request` to base `main`, and PRs #37 to #40 never ran CI.
- **Files:** `.github/workflows/ci.yml`. Remove the `branches` filter under `pull_request`. Remove the `mvn test` step, because `mvn verify` runs the same tests again.
- **Acceptance:** the PR shows one CI run with one test pass of 387 tests (or the count on its base). A PR based on another feature branch shows a CI check. GitHub may read the trigger from the base branch. If no run appears on the PR itself, the check passes on the first stacked PR after merge.
- **Model:** small. **Cost risk:** low.

### 2. Add the merge-gate rule and the PR template

- **Goal:** every agent and every human PR uses the ADR 005 checklist and review guide.
- **Files:** `.cursor/rules/merge-gate.mdc` (`alwaysApply`: checklist, review-guide headings, how to pick a tier), `.github/pull_request_template.md`, `CONTRIBUTING.md` (point the PR section at the template, and correct "Formatting is enforced with Spotless", which is false today).
- **Acceptance:** the next agent PR has the review guide without being asked. `mvn spotless:check` behavior and the `CONTRIBUTING.md` sentence agree.
- **Model:** small. **Cost risk:** low.

### 3. Validate every emitted envelope against the vendored schema

- **Goal:** catch wire drift in what the runtime actually sends, not only in hand-built messages. Catch unresolvable `$ref`s like the PR #39 `DynamicString` finding.
- **Files:** a shared test assertion in the web starter test sources that parses SSE frames and `POST /a2ui/actions` bodies and validates each envelope against `META-INF/a2ui/v1/agent_to_renderer.json`. Adopt it in the stream, action, and function ingress integration tests. A core test that loads every vendored schema and the showcase catalog contribution and resolves each `$ref`.
- **Acceptance:** a probe that renames one serialized field fails the integration tests. A probe that adds a `$ref` to a missing `$defs` entry fails the `$ref` test. Both probes are reverted, and their failing output is in the PR body.
- **Model:** medium. **Cost risk:** low. It may find real wire bugs, which become separate PRs.

### 4. Check docs against the code

- **Goal:** catch names, error codes, and versions in docs that do not exist in code. Five of the PR #39 findings were of this kind.
- **Files:** a JUnit test in the showcase test sources (it can load every library class). It reads `README.md`, `docs/guides/*.md`, and `docs/rest-api.md`. It fails on a backticked `A2Ui*` type that does not load, or on an error code missing from `A2UiActionErrorCodes` and the other code holders. Migration guides, `CHANGELOG.md`, and ADRs document removed names, so they are excluded or allow-listed by file and name. Relative Markdown links must point at files that exist.
- **Acceptance:** a probe that adds `A2UiUserAction` to `docs/guides/hosting-actions.md` fails the test. Drift the test finds on the current tree is fixed in the same PR when the fix is docs-only, or listed for the owner when it is not.
- **Model:** medium. **Cost risk:** low.

### 5. Generate outcome sequences against the ingress maps

- **Goal:** the dedupe and pending-call maps stay correct for every mix of outcomes, before parallel two-hop generation adds threads to compose.
- **Files:** web starter tests for `A2UiIngressDeduper` and `A2UiPendingRendererCalls`. Generated sequences of run, return, throw, join, waiter timeout, TTL expiry, and cap-full on one or a few keys. Properties: the work runs at most once per stored slot; no joined future stays incomplete after its owner returns or throws; a full map executes instead of rejecting; a replay returns the stored result.
- **Acceptance:** reintroducing the PR #39 blocker (no release of the slot when work throws) fails a generated case. The case count is capped so the tests add under 10 seconds.
- **Decision for the owner:** use jqwik (a new test-scoped dependency) or plain JUnit with a seeded `Random` (no dependency, weaker shrinking).
- **Model:** medium. **Cost risk:** medium. Concurrency tests that flake cost reruns, so each case uses latches, not sleeps.

### 6. Trace ADR 004 rules to tests

- **Goal:** each behavior rule in ADR 004 has a named test, so a missing rule is visible before review.
- **Files:** a rule-to-test table appended to ADR 004 under its testing strategy. Missing tests are added. The docs check from step 4 learns to confirm that each named test class and method exists.
- **Acceptance:** deleting one listed test method fails the docs check. Every idempotency and function-resolution rule in ADR 004 has a row.
- **Model:** medium. **Cost risk:** medium (reading ADR 004 against the tests is the expensive part).

### 7. Add ArchUnit rules for the library packages

- **Goal:** the build rejects dependency mistakes in the runtime, not only in the showcase.
- **Files:** a second ArchUnit test class beside `apps/be-transform-showcase/.../architecture/ArchitectureRulesTest.java`, importing `com.kutaybuyukkorukcu.a2ui.runtime..`. Candidates, each confirmed against the code before it is added: core has no `org.springframework` dependency; the web starter does not depend on showcase classes. Each rule's failure message names the type to use instead (pstack `correct`).
- **Acceptance:** each rule is shown failing on a probe import, then the probe is reverted.
- **Model:** small. **Cost risk:** low.

### 8. Check the PR body in CI

- **Goal:** a PR without the review guide cannot pass CI.
- **Files:** a job in `.github/workflows/ci.yml` that reads the pull request body from the event payload and fails when a review-guide heading is missing. No new action or dependency.
- **Acceptance:** a draft PR without the headings fails the job. This PR's body passes it.
- **Timing:** after three PRs have used the template from step 2, so the headings have settled.
- **Model:** small. **Cost risk:** low.

### 9. Model the dedupe slots formally

- **Goal:** check every order of claim, join, complete, abandon, timeout, and eviction for one key, within stated bounds.
- **Files:** `.cursor/skills/verify-ingress-dedupe/`, generated with pstack `create-formal-verification-skill`, with its model files, the coverage record (`A2UiIngressDeduper`, the `A2UiActionService` call sites), and the sync commit.
- **Acceptance:** the skill's own proof step: a scratch change that breaks the property in the real code is caught, then dropped. The run command and tool version are in the skill.
- **Decision for the owner:** a local model-checking tool (TLA+ tools are the skill's default for ordering questions; they need a JDK, which the repository already requires). Not run in CI at first.
- **Model:** medium to large. **Cost risk:** high. Building a model that is true to the code is the most model-heavy step in this plan. Skip it if step 5 already covers the orderings the owner cares about.

### 10. Add a keyless showcase profile

- **Goal:** the showcase starts without a provider key, so an agent can drive it.
- **Files:** `apps/be-transform-showcase` only: a profile with a scripted `ChatModel` that returns fixed tool-call arguments. No library change.
- **Acceptance:** `mvn -pl apps/be-transform-showcase spring-boot:run` with that profile serves one surface on `/a2ui/surface/stream` with no key set.
- **Decision for the owner:** a scripted model in the showcase, in a profile that is not the default.
- **Model:** medium. **Cost risk:** low.

### 11. Generate the repo-local verification skill

- **Goal:** a scripted way for any agent to launch the showcase, drive streams and actions, and capture evidence.
- **Files:** `.cursor/skills/verify-spring-a2ui/` with `SKILL.md` and `features/` (stream a surface, post an event with and without an idempotency key, call an agent function, fetch a catalog), generated with pstack `create-verification-skill`.
- **Acceptance:** the skill's own proof step: launch, doctor, drive one feature, capture evidence, clean up, and the evidence survives the cleanup.
- **Model:** medium. **Cost risk:** medium.

### 12. Replay stored model responses on the `3.0.0` line

- **Goal:** real provider output runs through the real compose path in CI, with no live call.
- **Files:** port PR #32 (`StoredModelResponseReplayTest`, `StoredModelResponseStreamIntegrationTest`, fixtures under `src/test/resources/model-responses/`) to v1.0, or record a new corpus.
- **Acceptance:** each fixture asserts the outcome its file name promises (for example `missing-prop` fails validation and `retry-then-valid` emits a surface), and the emitted envelopes pass the step 3 assertion.
- **Decision for the owner:** rebase PR #32 or record again with the owner's key.
- **Model:** medium. **Cost risk:** low for Cursor usage, plus provider tokens if re-recorded.

### Later, gated

- **Browser end-to-end.** Turn on `V1WebRendererE2ETest` with Playwright for Java (owner approval) once a web v1.0 renderer can post a valid `action`. Path-filter the CI job to FE and wire changes.
- **Spotless.** One mechanical PR runs `mvn spotless:apply` and binds `spotless:check` to `verify`, after the open stack merges. It exceeds the 400-line cap by nature. Or drop Spotless (ADR 005, confirmation 6).
- **Live prompt smoke.** A manual script that runs fixed intents against a live provider and prints the validation meters, for PRs that change `DynamicA2UiPromptProvider` or the `renderA2Ui` schema.
- **Portable user-level skill.** After three PRs have used the merge gate, copy the checklist, review guide, and tier table into a user-level skill for other repositories.
- **First maintenance passes.** `maintain-verification-skill` at the first release cut after step 11. `maintain-formal-verification-skill` on the first PR that touches the step 9 coverage record.

## Done

- [ ] ADR 005 accepted by the owner
- [ ] Steps 1 to 8
- [ ] Steps 9 to 12, or each one dropped by the owner by name
- [ ] Fold this plan into ADR 005 and delete it, per `.cursor/rules/docs-plans.mdc`
