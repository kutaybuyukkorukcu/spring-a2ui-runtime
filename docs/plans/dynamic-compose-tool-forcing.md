# Dynamic compose tool forcing beyond OpenAI

**Status:** in flight (not on `main`)
**Branch:** `cursor/dynamic-compose-tool-forcing-ce02`
**Backlog:** [Dynamic compose tool forcing beyond OpenAI](../../BACKLOG.md#dynamic-compose-tool-forcing-beyond-openai)

## In

- Classpath-conditional forced tool choice in `A2UiForcedToolChoiceFactory` for the providers Spring AI `1.1.0-M2` can actually force.
- OpenAI named `tool_choice` (golden path), including OpenAI-compatible servers.
- Native Mistral `tool_choice=any` (enum only; each hop registers one tool).
- Fail-closed `TOOL_CHOICE_UNAVAILABLE` when the active provider cannot force. No empty `ChatOptions` skip.
- Active provider from `A2UiProviderResolver` (configured chat model property). Unknown provider with exactly one ChatOptions type on the classpath uses that type. Several types and no property fails closed.
- Keep `returnDirect` and the two one-shot hops.
- Unit tests per provider, a mocked HTTP check of the OpenAI and Mistral bodies, and a check that a reply with no tool call fails closed.
- Support matrix in [multi-provider Spring AI](../guides/multi-provider-spring-ai.md).

## Out

- A new provider abstraction layer.
- Moving getting started off OpenAI.
- Quality-parity promises, benchmarks, A2UI v1.0, parallel two-hop.
- Bumping the Spring AI BOM. Anthropic, Vertex, Google GenAI, Bedrock Converse, native Azure OpenAI, and native Ollama do not expose tool choice in `1.1.0-M2`, so they fail closed.
- Live calls to providers that have no API key in this environment.

## Constraints

- Spring AI `1.1.0-M2`. Do not bump.
- Provider jars are test-scoped on the web starter. Published modules do not gain a compile dependency on them. The factory keeps using reflection.
- PR targets `main` and is not merged. Later v1.0 work can stack on this branch.

## Done

- [x] Factory builds OpenAI and Mistral options and fail-closes the rest
- [x] Compose hops pass the resolved provider
- [x] Matrix, changelog, and this plan
- [x] `mvn verify` green on the branch
