# Backlog

Items intentionally deferred because they do not directly unblock demo UI rendering for random prompts.

## Reliability and Observability

- Add runtime counters for repair/fallback paths:
  - `a2ui.transform.repaired`
  - `a2ui.transform.fallback`
  - `a2ui.transform.retry.success`
  - `a2ui.transform.retry.failed`
- Add structured redacted logging for invalid model payload diagnostics.

## Generation Hardening

- Add one bounded correction retry for non-repairable parse failures with explicit schema feedback.
- Add DTO-derived JSON schema injection for strict OpenAI JSON schema mode in deterministic policy.

## Test Coverage

- Add a web-starter integration test that proves malformed multi-envelope model output still results in a renderable `surfaceUpdate -> beginRendering` sequence.
- Add stream-path tests for fallback emission when mapping/parsing fails mid-stream.
