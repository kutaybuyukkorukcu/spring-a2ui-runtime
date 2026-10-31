# Dynamic compose tool forcing beyond OpenAI

**Status:** in flight (not on `main`)
**Branch:** `cursor/spring-ai-1-1-8-tool-forcing-ae96` (stacked on `cursor/dynamic-compose-tool-forcing-ce02`)
**Backlog:** [Dynamic compose tool forcing beyond OpenAI](../../BACKLOG.md#dynamic-compose-tool-forcing-beyond-openai)

## In

- Classpath-conditional forced tool choice in `A2UiForcedToolChoiceFactory`. No provider SPI.
- Spring AI BOM **1.1.8** GA. Spring Boot stays **3.4.1**.
- OpenAI named `tool_choice` (golden path), including OpenAI-compatible servers. The 1.1.8 helper is `ToolChoiceBuilder.function`.
- Anthropic named `tool_choice` (`AnthropicApi.ToolChoiceTool`, `type` `tool`).
- Native Mistral `tool_choice=any` (enum only; each hop registers one tool).
- Fail-closed `TOOL_CHOICE_UNAVAILABLE` when the active provider cannot force. No empty `ChatOptions` skip.
- Active provider from `A2UiProviderResolver` (configured chat model property). Unknown provider with exactly one ChatOptions type on the classpath uses that type. Several types and no property fails closed.
- Keep `returnDirect` and the two one-shot hops.
- Unit tests per provider, mocked HTTP checks of the OpenAI, Anthropic, and Mistral bodies, and a check that a reply with no tool call fails closed.
- Support matrix in [multi-provider Spring AI](../guides/multi-provider-spring-ai.md), including providers that no Boot 3 Spring AI release can force.

## Out

- A new provider abstraction layer.
- Moving getting started off OpenAI.
- Quality-parity promises, benchmarks, A2UI v1.0, parallel two-hop.
- Spring Boot 4 and Spring AI 2.x. Google GenAI `ToolChoice` (mode `ANY` plus `allowedFunctionNames`) exists there. Vertex Gemini chat and native Azure OpenAI chat are removed there. Bedrock Converse and native Ollama still have no tool choice there.
- Live calls to providers that have no API key in this environment.

## Constraints

- Spring AI `1.1.8`. Boot parent stays `3.4.1`.
- Provider jars stay test-scoped on the web starter. Published modules do not gain a compile dependency on Anthropic. The factory keeps using reflection.
- PR base is `cursor/dynamic-compose-tool-forcing-ce02`. Not merged.

## Done

- [x] Factory builds OpenAI, Anthropic, and Mistral options and fail-closes the rest
- [x] Compose hops pass the resolved provider
- [x] Matrix, changelog, and this plan name 1.1.8 and the providers that stay fail-closed
- [x] `mvn verify` green on the branch
