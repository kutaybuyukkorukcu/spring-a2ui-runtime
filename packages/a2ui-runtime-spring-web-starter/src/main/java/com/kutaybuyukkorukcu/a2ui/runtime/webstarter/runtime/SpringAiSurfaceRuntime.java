package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm.A2UiLlmMappingException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm.A2UiLlmOutput;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm.A2UiLlmOutputMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SpringAiSurfaceRuntime implements A2UiSurfaceRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAiSurfaceRuntime.class);
    private static final long SLOW_LLM_CALL_WARN_MS = 30_000L;

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final List<Advisor> advisors;
    private final Environment environment;
    private final A2UiWebProperties properties;
    private final A2UiPromptProvider promptProvider;
    private final A2UiLlmOutputMapper llmOutputMapper;

    public SpringAiSurfaceRuntime(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            List<Advisor> advisors,
            Environment environment,
            A2UiWebProperties properties,
            A2UiPromptProvider promptProvider,
            A2UiLlmOutputMapper llmOutputMapper) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.advisors = advisors == null ? List.of() : advisors;
        this.environment = environment;
        this.properties = properties;
        this.promptProvider = promptProvider;
        this.llmOutputMapper = llmOutputMapper;
    }

    @Override
    public List<A2UiMessage> generate(A2UiSurfaceRequest request, String requestId, String catalogId) {
        long requestStartNs = System.nanoTime();
        LOGGER.info("A2UI generate start: requestId={}, catalogId={}", requestId, catalogId);

        ChatClient chatClient = createClient();

        A2UiPromptContext promptContext = new A2UiPromptContext(
                request.content(),
                buildContextHints(request),
                catalogId,
                extractSupportedCatalogIds(request)
        );

        String systemPrompt = promptProvider.createSystemPrompt(promptContext);
        String userPrompt = promptProvider.createUserPrompt(promptContext);
        LOGGER.debug(
                "A2UI generate prompts prepared: requestId={}, systemPromptChars={}, userPromptChars={}",
                requestId,
                systemPrompt.length(),
                userPrompt.length());

        A2UiLlmOutput output;
        long llmCallStartNs = System.nanoTime();
        LOGGER.info("A2UI generate invoking LLM: requestId={}", requestId);
        try {
            output = chatClient.prompt()
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(A2UiLlmOutput.class);
        } catch (Exception e) {
            LOGGER.warn(
                    "A2UI generate LLM call failed: requestId={}, llmDurationMs={}",
                    requestId,
                    elapsedMs(llmCallStartNs),
                    e);
            return fallbackMessages(request, requestId, catalogId, "llm_call_failed", e);
        }
        long llmDurationMs = elapsedMs(llmCallStartNs);
        if (llmDurationMs >= SLOW_LLM_CALL_WARN_MS) {
            LOGGER.warn("A2UI generate slow LLM call: requestId={}, llmDurationMs={}", requestId, llmDurationMs);
        } else {
            LOGGER.info("A2UI generate LLM completed: requestId={}, llmDurationMs={}", requestId, llmDurationMs);
        }

        if (output == null || output.messages() == null || output.messages().isEmpty()) {
            return fallbackMessages(request, requestId, catalogId, "empty_llm_output", null);
        }

        List<A2UiMessage> messages;
        long mappingStartNs = System.nanoTime();
        try {
            messages = llmOutputMapper.map(output);
        } catch (A2UiLlmMappingException ex) {
            return fallbackMessages(request, requestId, catalogId, "mapping_failed:" + ex.getReason(), ex);
        } catch (IllegalArgumentException ex) {
            return fallbackMessages(request, requestId, catalogId, "mapping_failed:illegal_argument", ex);
        }

        LOGGER.info(
                "A2UI generate completed: requestId={}, messageCount={}, mappingDurationMs={}, totalDurationMs={}",
                requestId,
                messages.size(),
                elapsedMs(mappingStartNs),
                elapsedMs(requestStartNs));

        return messages;
    }

    @Override
    public String getActiveModelName() {
        if (properties.getRuntime().getModelName() != null) {
            return properties.getRuntime().getModelName();
        }
        return environment.getProperty("spring.ai.chat.options.model", "unknown");
    }

    ChatClient createClient() {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("No ChatClient.Builder available. Ensure a ChatModel is configured.");
        }
        for (Advisor advisor : advisors) {
            builder = builder.defaultAdvisors(advisor);
        }
        return builder.build();
    }

    @Override
    public Flux<A2UiMessage> stream(A2UiSurfaceRequest request, String requestId, String catalogId) {
        long requestStartNs = System.nanoTime();
        LOGGER.info("A2UI stream start: requestId={}, catalogId={}", requestId, catalogId);

        ChatClient chatClient = createClient();

        A2UiPromptContext promptContext = new A2UiPromptContext(
                request.content(),
                buildContextHints(request),
                catalogId,
                extractSupportedCatalogIds(request)
        );

        String systemPrompt = promptProvider.createSystemPrompt(promptContext);
        String userPrompt = promptProvider.createUserPrompt(promptContext);

        BeanOutputConverter<A2UiLlmOutput> converter = new BeanOutputConverter<>(A2UiLlmOutput.class);
        String structuredUserPrompt = userPrompt
            + "\n\nReturn ONLY valid JSON. Do not include markdown fences or prose.\n"
            + converter.getFormat();

        AtomicInteger chunkCount = new AtomicInteger();
        AtomicLong chunkChars = new AtomicLong();
        long llmCallStartNs = System.nanoTime();
        LOGGER.info("A2UI stream invoking LLM stream: requestId={}", requestId);

        return chatClient.prompt()
                .system(systemPrompt)
            .user(structuredUserPrompt)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    chunkCount.incrementAndGet();
                    chunkChars.addAndGet(chunk.length());
                })
                .reduce("", String::concat)
                .flatMapMany(content -> {
                    long llmDurationMs = elapsedMs(llmCallStartNs);
                    if (llmDurationMs >= SLOW_LLM_CALL_WARN_MS) {
                        LOGGER.warn(
                                "A2UI stream slow LLM stream completion: requestId={}, llmDurationMs={}, chunkCount={}, chunkChars={}",
                                requestId,
                                llmDurationMs,
                                chunkCount.get(),
                                chunkChars.get());
                    } else {
                        LOGGER.info(
                                "A2UI stream LLM stream completed: requestId={}, llmDurationMs={}, chunkCount={}, chunkChars={}",
                                requestId,
                                llmDurationMs,
                                chunkCount.get(),
                                chunkChars.get());
                    }
                    try {
                        A2UiLlmOutput output = converter.convert(content);
                        List<A2UiMessage> mapped = llmOutputMapper.map(output);
                        LOGGER.info(
                                "A2UI stream mapping completed: requestId={}, messageCount={}, totalDurationMs={}",
                                requestId,
                                mapped.size(),
                                elapsedMs(requestStartNs));
                        return Flux.fromIterable(mapped);
                    } catch (A2UiLlmMappingException ex) {
                        return Flux.fromIterable(fallbackMessages(request, requestId, catalogId, "stream_mapping_failed:" + ex.getReason(), ex));
                    } catch (IllegalArgumentException ex) {
                        return Flux.fromIterable(fallbackMessages(request, requestId, catalogId, "stream_mapping_failed:illegal_argument", ex));
                    }
                })
                .onErrorResume(ex -> Flux.fromIterable(fallbackMessages(request, requestId, catalogId, "stream_failed", ex)));
    }

    private List<A2UiMessage> fallbackMessages(
            A2UiSurfaceRequest request,
            String requestId,
            String catalogId,
            String reason,
            Throwable cause) {
        if (cause == null) {
            LOGGER.warn("Falling back to deterministic A2UI surface: requestId={}, reason={}", requestId, reason);
        } else {
            LOGGER.warn(
                    "Falling back to deterministic A2UI surface: requestId={}, reason={}",
                    requestId,
                    reason,
                    cause);
        }

        String surfaceId = "main";
        String rootId = "fallback-root";
        String resolvedCatalogId = resolveFallbackCatalogId(request, catalogId);
        String requestText = request != null ? request.content() : null;
        String safeText = truncateForFallback(requestText);

        A2UiMessage.ComponentDefinition root = new A2UiMessage.ComponentDefinition(
                rootId,
                Map.of("Text", Map.of("text", Map.of("literalString", safeText))));

        return List.of(
                new A2UiMessage.SurfaceUpdate(surfaceId, List.of(root)),
                new A2UiMessage.BeginRendering(surfaceId, rootId, resolvedCatalogId, null));
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private String resolveFallbackCatalogId(A2UiSurfaceRequest request, String catalogId) {
        if (catalogId != null && !catalogId.isBlank()) {
            return catalogId;
        }
        if (request != null
                && request.a2uiClientCapabilities() != null
                && request.a2uiClientCapabilities().supportedCatalogIds() != null) {
            String candidate = request.a2uiClientCapabilities().supportedCatalogIds().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .findFirst()
                    .orElse(null);
            if (candidate != null) {
                return candidate;
            }
        }
        return A2UiCatalogIds.STANDARD_V0_8;
    }

    private String truncateForFallback(String content) {
        if (content == null || content.isBlank()) {
            return "Generated fallback surface.";
        }
        String compact = content.trim().replaceAll("\\s+", " ");
        if (compact.length() <= 140) {
            return compact;
        }
        return compact.substring(0, 140) + "...";
    }

    private String buildContextHints(A2UiSurfaceRequest request) {
        if (request.context() == null) return null;
        StringBuilder hints = new StringBuilder();
        if (request.context().intent() != null) {
            hints.append("Intent: ").append(request.context().intent()).append(". ");
        }
        if (request.context().preferredComponents() != null && !request.context().preferredComponents().isEmpty()) {
            hints.append("Preferred components: ").append(String.join(", ", request.context().preferredComponents())).append(". ");
        }
        if (request.context().instructions() != null) {
            hints.append(request.context().instructions());
        }
        String value = hints.toString().trim();
        return value.isEmpty() ? null : value;
    }

    private List<String> extractSupportedCatalogIds(A2UiSurfaceRequest request) {
        if (request.a2uiClientCapabilities() == null) return List.of();
        List<String> ids = request.a2uiClientCapabilities().supportedCatalogIds();
        return ids == null ? List.of() : ids;
    }
}