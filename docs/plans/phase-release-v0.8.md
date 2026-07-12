# Implementation Plan: v0.8 Official Release

**Status:** Ready to execute — runtime GA criteria already met (Phases 0–2.5 ✅).
**Prerequisite:** Phase 2.5 complete on `main` (template + dynamic without semantic repair).
**Backlog:** [`BACKLOG.md`](../../BACKLOG.md) — v0.8 release section
**Branch:** `chore/v0.8-release` from `main` (or slice PRs directly to `main`).
**Protocol:** A2UI **v0.8** wire format only. v0.9 is Phase X (after this release).

---

## Goal

Ship the first **official open-source release** of spring-a2ui as a usable Spring runtime for A2UI v0.8:

1. **Freeze** — prove regression green; no repair paths; both generation modes documented as GA.
2. **Package** — OSS repo hygiene + developer quickstart so strangers can adopt without tribal knowledge.
3. **Publish** — versioned Maven Central artifacts + GitHub Release notes.

This phase is **release engineering + docs**, not new generative-UI features.

---

## Release policy (what we claim)

| Mode | Claim | Notes |
|------|-------|--------|
| `generation-mode=template` | **GA** | Deterministic builders; no normalizer |
| `generation-mode=dynamic` | **GA** | Thin assembler + catalog validation + bounded retry; no semantic repair |
| Non-A2UI chat/agent pipes | **Not included** | A2UI-native SSE only (ADR 001) |
| A2UI v0.9 | **Not included** | Phase X after this release |
| Custom template SPI | **Not required for GA** | Documented as Later; optional utilization follow-up |

**Primary persona:** Spring app developers embedding generative surfaces (forms, cards, confirmations, specialist-agent widgets) via `POST /a2ui/surface/stream`.

---

## Version decision

| Option | Tag / `${revision}` | Outcome |
|--------|---------------------|---------|
| ~~`0.8.0`~~ | Rejected | Maven would treat existing Central `1.0.0` as newer |
| ~~Keep `1.0.0`~~ | Rejected | Already published (early drop); versions are immutable |
| **`1.1.0` (chosen)** | Git tag `v1.1.0` | Next Maven line; documents A2UI **v0.8** protocol GA |

**Context:** `1.0.0` is already on Maven Central for **this** repository (`spring-a2ui-runtime`) from an earlier publish. It cannot be removed or overwritten. **1.1.0** is the fresh GA release consumers should use.

Artifacts published (existing workflow):

- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-core:1.1.0`
- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-spring-starter:1.1.0`
- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-spring-web-starter:1.1.0`
- Parent POM `spring-a2ui-runtime:1.1.0` (reactor) is published alongside via `-am`

Showcase app is **not** published (`maven.deploy.skip`).

**Note:** Phase name remains “v0.8 release” because the **protocol** is A2UI v0.8. Library semver is independent (`1.1.0`).

---

## Slice overview (implement one by one)

```
R.1 OSS foundation files
  → R.2 Developer docs (README + getting started)
    → R.3 Doc freshness / ADR status
      → R.4 Version + CHANGELOG
        → R.5 CI/CD verification
          → R.6 Freeze verification (tests)
            → R.7 Publish (Central + GitHub Release)
```

Each slice is a small PR. Do not start R.7 until R.1–R.6 are green.

---

## R.1 — OSS foundation files

**Why:** Repo has no root README / LICENSE file / community docs. pom claims MIT but there is no `LICENSE` file. Strangers (and Maven Central reviewers) expect these.

### Deliverables

| File | Purpose |
|------|---------|
| `LICENSE` | MIT text (match pom `<licenses>`) |
| `CODE_OF_CONDUCT.md` | Contributor Covenant (or short equivalent) |
| `CONTRIBUTING.md` | How to build, test, PR; link plans/backlog; Java 21 + Maven |
| `SECURITY.md` | How to report vulnerabilities (private contact / GitHub Security Advisories) |
| `.github/ISSUE_TEMPLATE/` | Optional but recommended: bug + feature templates |
| `.github/PULL_REQUEST_TEMPLATE.md` | Optional: checklist (tests, docs) |

### Acceptance

- [x] `LICENSE` present at repo root
- [x] `CONTRIBUTING.md` documents `mvn verify -B -ntp` and showcase run commands
- [x] `SECURITY.md` present
- [x] `CODE_OF_CONDUCT.md` present

### Non-goals

- Rewriting RESEARCH_NOTES.md
- Full website / docs site

**Status:** ✅ Done on `chore/v0.8-release` (also issue/PR templates).

---

## R.2 — Developer docs (README + getting started)

**Why:** Product builders need a 5-minute path from dependency → first SSE surface.

### Deliverables

1. **Root `README.md`** (required) — single composition for GitHub landing:
   - What it is (Spring runtime for A2UI v0.8)
   - Status badge(s): CI, Maven Central (after publish; placeholder OK before)
   - Quickstart: Maven dependency on `a2ui-runtime-spring-web-starter`
   - Minimal Boot snippet / property table (`generation-mode`, base path)
   - Link to `docs/rest-api.md` and `docs/guides/dynamic-generative-ui.md`
   - Template vs dynamic one-liner
   - Local showcase: how to run `be-transform-showcase` + `fe-a2ui-demo`
   - Package map (core / spring-starter / web-starter)
   - License + link to CONTRIBUTING

2. **`docs/guides/getting-started.md`** (required) — step-by-step:
   - Prerequisites (Java 21, Spring Boot 3.4.x, Spring AI OpenAI config)
   - Add dependency
   - Enable auto-config
   - Call `POST /a2ui/surface/stream` (curl example from rest-api)
   - Choose `template` vs `dynamic`
   - Wire a client (`@a2ui/react` / demo pointer)
   - Common errors (`CONTENT_REQUIRED`, `A2UI_VALIDATION_FAILED`, catalog mismatch)

3. **Optional short READMEs** under `apps/be-transform-showcase` and `apps/fe-a2ui-demo` if useful for local demo only (not a substitute for root README).

### Acceptance

- [x] Root README exists and a new contributor can find install + stream endpoint without reading BACKLOG
- [x] Getting-started guide walks template **and** dynamic
- [x] No instructions that reference removed sync `POST /a2ui/surface`

### Suggested README dependency snippet

```xml
<dependency>
  <groupId>com.kutaybuyukkorukcu.a2ui.runtime</groupId>
  <artifactId>a2ui-runtime-spring-web-starter</artifactId>
  <version>1.1.0</version>
</dependency>
```

**Status:** ✅ Done on `chore/v0.8-release` (Spring AI–style README tone; no marketing fluff).

---

## R.3 — Doc freshness / ADR status

**Why:** Stale docs undermine trust at release.

### Tasks

1. ADR 001: change Status from **Draft** → **Accepted** (decisions already shipped).
2. Fix [`.github/copilot-instructions.md`](../../.github/copilot-instructions.md): remove sync `POST /a2ui/surface` from public surface list; stream-only.
3. Confirm [`docs/rest-api.md`](../rest-api.md) matches shipped endpoints and `generation-mode` property.
4. Confirm [`docs/guides/dynamic-generative-ui.md`](../guides/dynamic-generative-ui.md) reflects thin assembler (no repair language).
5. Update phase plan headers that still say “in progress” if any; leave historical checklists intact.
6. Align SCM / URL naming: decide whether GitHub repo is `spring-a2ui` vs `spring-a2ui-runtime` and make `pom.xml` `<url>` / `<scm>` match reality (document the chosen canonical name in README).

### Acceptance

- [x] ADR Status = Accepted
- [x] Agent/copilot public-surface list matches rest-api (no sync endpoint)
- [x] pom SCM URL matches the actual GitHub repository (`spring-a2ui-runtime`)

**Status:** ✅ Done — also fixed `generation-mode` docs to match library default `dynamic`.

---

## R.4 — Version + CHANGELOG

### Tasks

1. Set root `pom.xml` `<revision>1.1.0</revision>` (from `1.0.0`; do **not** reuse `1.0.0`).
2. Add root **`CHANGELOG.md`** with a `## [1.1.0] — YYYY-MM-DD` section:
   - Highlights: stream-only SSE; template GA; dynamic GA (two-hop + validate + retry); catalog prop validation; no semantic repair
   - Note relationship to Central `1.0.0` (earlier drop of this project)
   - Packages published
   - Known limitations: OpenAI-first; A2UI surface SSE only; no v0.9; custom template SPI later
3. Draft GitHub Release body (can live in CHANGELOG and be copied on tag).

### Acceptance

- [x] `${revision}` is `1.1.0`
- [x] CHANGELOG exists and matches shipped behavior
- [x] No claim of v0.9 support

**Status:** ✅ Version + CHANGELOG set on `chore/v0.8-release`.

---

## R.5 — CI/CD verification

Existing workflows:

- [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) — `mvn test` + `mvn verify` on push/PR to `main`
- [`.github/workflows/publish-packages.yml`](../../.github/workflows/publish-packages.yml) — Central deploy on `release: published` or `workflow_dispatch`

### Tasks

1. Confirm CI is green on `main` after R.1–R.4 land.
2. Verify GitHub Actions secrets exist (names from workflow):
   - `CENTRAL_TOKEN_USERNAME`
   - `CENTRAL_TOKEN_PASSWORD`
   - `CENTRAL_GPG_PASSPHRASE`
   - `CENTRAL_GPG_PRIVATE_KEY`
3. Optional hardening (if cheap):
   - Add workflow badge to README
   - Ensure publish job cannot run on forks without secrets (already fails closed)
   - Document in CONTRIBUTING: “Do not bump revision on feature PRs; release branch/PR only”
4. Confirm FE demo is **not** required in CI for Java package release (current CI is Maven-only — acceptable for v0.8; note in release notes if FE is manual).

### Acceptance

- [x] Secrets checklist completed — Central publish of `1.0.0` already succeeded for this namespace; required secret *names* match `publish-packages.yml`. Re-confirm secrets still present in GitHub Actions before R.7.
- [x] Local `mvn verify` green on release branch at `1.1.0` (CI will re-run on PR to `main`)
- [x] Publish workflow reviewed; deploys only `:a2ui-runtime-core`, `:a2ui-runtime-spring-starter`, `:a2ui-runtime-spring-web-starter` (+ parent via `-am`); showcase has `maven.deploy.skip=true`

**Status:** ✅ Ready for publish from a CI/secrets standpoint (user: glance at repo Actions secrets before tagging).

---

## R.6 — Freeze verification (tests)

**Why:** Phase 2.5 plan still had an unchecked “all regression tests green” item — make it explicit for release.

### Commands (local + CI)

```bash
mvn verify -B -ntp
```

### Manual / showcase checklist

- [x] Automated suite green via `mvn verify` (includes showcase host tests)
- [ ] Optional live smoke: template + dynamic profiles with a real API key (recommended before tagging, not blocking if CI green)
- [x] Confirm codebase has **no** `fixCardComponent` / `fixButtonComponent` / `fixCheckBoxComponent` / `enforceCatalogConstraints` repair APIs

### Acceptance

- [x] `mvn verify` green (reactor `1.1.0`, 2026-07-12)
- [x] Repair-path grep is clean
- [x] Verified note in CHANGELOG

**Status:** ✅ Freeze verification complete locally.

---

## R.7 — Publish

### Sequence

1. Merge freeze PR(s) to `main`.
2. Create GitHub Release **`v1.1.0`** with CHANGELOG body (triggers `publish-packages.yml` on `release: published`).
   - Or: `workflow_dispatch` publish first as dry-run confidence, then tag release.
3. Confirm Maven Central / Central Portal listing for the three artifacts at `1.1.0`.
4. Update README badges to real Central coordinates if placeholders were used.
5. Announce (optional): short GitHub Discussion or repo Topics (`a2ui`, `spring-boot`, `spring-ai`, `generative-ui`).

### Acceptance

- [ ] Git tag `v1.1.0` exists
- [ ] Three artifacts resolvable from Maven Central (or Central Portal “published”)
- [ ] GitHub Release published with notes
- [ ] BACKLOG “v0.8 release” marked ✅

### Rollback

- If Central publish fails: fix credentials/signing; do **not** retag with force on a bad release without a written decision. Prefer `1.1.1` for follow-up fixes.

---

## Out of scope (do not block v0.8)

| Item | Tracked where |
|------|----------------|
| A2UI v0.9 migration | [`phase-x-migrating-to-v0.9.md`](phase-x-migrating-to-v0.9.md) |
| Optional interoperability / chat-shell bridge | BACKLOG — Later |
| Custom `A2UiTemplateRegistry` consumer SPI | BACKLOG — Later |
| Anthropic / Gemini / Groq parity | BACKLOG — Later |
| Docs website (MkDocs/Orchid) | Future |
| Publishing the showcase app | Never (sample only) |

---

## Definition of done (release)

- [ ] R.1–R.7 acceptance checklists complete
- [ ] Template + dynamic documented as GA for A2UI v0.8
- [ ] Artifacts on Maven Central at `1.1.0`
- [ ] Repo landing page (README) sufficient for first external integrator
- [ ] Phase X remains unstarted until this release is marked complete in BACKLOG

---

## Suggested PR order

| PR | Slice | Title suggestion |
|----|-------|------------------|
| 1 | R.1 | `chore: add LICENSE and community foundation files` |
| 2 | R.2 | `docs: add README and getting-started guide for v0.8` |
| 3 | R.3 | `docs: accept ADR 001 and fix stale public-surface docs` |
| 4 | R.4 | `chore: set revision 1.1.0 and add CHANGELOG` |
| 5 | R.5–R.6 | `chore: release freeze verification for 1.1.0` |
| 6 | R.7 | GitHub Release `v1.1.0` (no code PR; publish) |

---

## References

- [`BACKLOG.md`](../../BACKLOG.md)
- [`docs/adr/001-streaming-surface-generation.md`](../adr/001-streaming-surface-generation.md)
- [`docs/plans/phase-2.5-scalable-dynamic-runtime.md`](phase-2.5-scalable-dynamic-runtime.md)
- [`docs/plans/phase-x-migrating-to-v0.9.md`](phase-x-migrating-to-v0.9.md)
- [`docs/rest-api.md`](../rest-api.md)
- [`docs/guides/dynamic-generative-ui.md`](../guides/dynamic-generative-ui.md)
- [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)
- [`.github/workflows/publish-packages.yml`](../../.github/workflows/publish-packages.yml)
