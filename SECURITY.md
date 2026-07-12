# Security Policy

## Supported Versions

Security fixes are applied to the latest published release line.

| Version | Supported |
| ------- | --------- |
| 1.1.x   | Yes       |
| 1.0.x   | No        |
| < 1.0   | No        |

## Reporting a Vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.

Prefer one of:

1. [GitHub Security Advisories](https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/security/advisories/new)
   for this repository (private report), or
2. Contact the maintainer privately via GitHub:
   [@kutaybuyukkorukcu](https://github.com/kutaybuyukkorukcu)

Include enough detail to reproduce the issue (affected module, version, and a
minimal PoC when possible). You should receive an acknowledgement within a few
days. Please give us reasonable time to investigate and ship a fix before any
public disclosure.

## Scope Notes

This project validates and streams [A2UI](https://a2ui.org/) messages and
integrates with LLM providers through Spring AI. Reports that involve prompt
injection, unsafe surface rendering assumptions, or credential handling in the
showcase app are welcome — especially when they affect the reusable packages
under `packages/`.
