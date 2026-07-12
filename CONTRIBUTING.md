# Contributing to spring-a2ui-runtime

Thanks for taking the time to contribute. This document covers how to work on
the code in this repository.

This project is released under the [MIT License](LICENSE).

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md).
By participating, you are expected to uphold that code.

## Reporting security issues

If you believe you found a security vulnerability, **do not** open a public
issue. See [SECURITY.md](SECURITY.md).

## Questions vs bugs

* **Usage / “how do I…?”** — open a
  [GitHub Discussion](https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/discussions)
  (or an issue if Discussions are not enabled yet). Point at the relevant doc
  under [`docs/`](docs/) when you can.
* **Bug** — open an issue with steps to reproduce, Spring Boot / Spring AI
  versions, `generation-mode` (`template` or `dynamic`), and a minimal sample if
  possible.
* **Enhancement** — open an issue first for design discussion when the change
  touches public HTTP contracts or package APIs.

Before filing something new, search existing
[issues](https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/issues) and
[pull requests](https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/pulls).

## Project layout

| Path | Role |
| ---- | ---- |
| `packages/a2ui-runtime-core` | Protocol models, validation, catalogs |
| `packages/a2ui-runtime-spring-starter` | Spring AI orchestration / runtime wiring |
| `packages/a2ui-runtime-spring-web-starter` | HTTP + SSE endpoints (auto-configuration) |
| `apps/be-transform-showcase` | Sample host application (not published) |
| `apps/fe-a2ui-demo` | Sample `@a2ui/react` client (not published) |
| `docs/` | ADRs, implementation plans, API reference |

Reusable behavior belongs in `packages/`. Showcase-only shortcuts stay in
`apps/`.

## Building and testing

Requirements:

* JDK **21**
* Maven 3.9+ (or the wrapper if present)

From the repository root:

```shell
mvn test -B -ntp
mvn verify -B -ntp
```

Formatting is enforced with Spotless. If CI complains about format:

```shell
mvn spotless:apply -B -ntp
```

### Showcase (optional, needs an API key)

```shell
export OPENAI_API_KEY=...

# Template mode (default Spring profile in the showcase)
mvn -pl apps/be-transform-showcase spring-boot:run

# Dynamic mode
mvn -pl apps/be-transform-showcase spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=dynamic"
```

Frontend demo:

```shell
cd apps/fe-a2ui-demo
npm install
npm run dev
```

## Versioning

Do **not** bump `<revision>` in `pom.xml` on ordinary feature PRs.
Version bumps happen on the release branch / release PR only (see
[`docs/plans/phase-release-v0.8.md`](docs/plans/phase-release-v0.8.md)).

## Pull requests

A good PR is small, reviewable, and explains *why*.

Before you open one:

1. Rebase (or merge) onto current `main`.
2. Add or update tests for the behavior you changed.
3. Run `mvn verify -B -ntp` locally.
4. Update the nearest doc when you change public behavior
   (`docs/rest-api.md`, guides, or an ADR).
5. Keep package APIs stable unless the PR is explicitly an API change.

PR description tips:

* Link related issues (`Fixes #123`).
* Call out `generation-mode` impact (`template`, `dynamic`, or both).
* Note any follow-up work you intentionally left out.

### Commit messages

Prefer short, imperative subjects — the style used across Spring projects:

* `Add catalog property validation for CheckBox`
* `Fix SSE error event when validation fails`

Avoid noisy prefixes in the subject (`feat:`, `fix:`) unless your local workflow
depends on them. Put issue links in the body.

### AI-assisted contributions

Contributions drafted with coding agents are fine **when a human reviews the
diff and owns the result**. Please say so in the PR if a substantial portion was
agent-generated, and double-check tests and public docs yourself.

Accounts that exist only to submit unattended bot PRs are not welcome.

## Design docs

Larger changes should line up with:

* [`BACKLOG.md`](BACKLOG.md) — current execution order
* [`docs/adr/`](docs/adr/) — accepted decisions (start with ADR 001)
* [`docs/plans/`](docs/plans/) — phase plans agents and humans share

If your idea conflicts with an ADR, open an issue before coding the alternative.

## License

By contributing, you agree that your contributions will be licensed under the
MIT License covering this repository.
