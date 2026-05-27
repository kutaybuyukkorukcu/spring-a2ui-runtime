package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.parse.A2UiMessageParser;
import com.kutaybuyukkorukcu.a2ui.runtime.parse.A2UiParseException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import reactor.core.publisher.Flux;

import java.util.List;

public class SpringAiSurfaceRuntime implements A2UiSurfaceRuntime {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final List<Advisor> advisors;
    private final Environment environment;
    private final A2UiWebProperties properties;
    private final A2UiPromptProvider promptProvider;
    private final A2UiMessageParser messageParser;
    private final TemplateSurfaceOrchestrator templateOrchestrator;

    public SpringAiSurfaceRuntime(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            List<Advisor> advisors,
            Environment environment,
            A2UiWebProperties properties,
            A2UiPromptProvider promptProvider,
            A2UiMessageParser messageParser,
            TemplateSurfaceOrchestrator templateOrchestrator) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.advisors = advisors == null ? List.of() : advisors;
        this.environment = environment;
        this.properties = properties;
        this.promptProvider = promptProvider;
        this.messageParser = messageParser;
        this.templateOrchestrator = templateOrchestrator;
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
        if (isTemplateMode()) {
            return templateOrchestrator.stream(request, requestId, catalogId);
        }
        return streamDynamic(request, catalogId);
    }

    private boolean isTemplateMode() {
        return "template".equalsIgnoreCase(properties.getRuntime().getGenerationMode());
    }

    private Flux<A2UiMessage> streamDynamic(A2UiSurfaceRequest request, String catalogId) {
        A2UiPromptContext promptContext = new A2UiPromptContext(
                request.content(),
                buildContextHints(request),
                catalogId,
                extractSupportedCatalogIds(request)
        );

        String systemPrompt = promptProvider.createSystemPrompt(promptContext);
        String userPrompt = promptProvider.createUserPrompt(promptContext);

        return Flux.defer(() -> {
            ChatClient chatClient = createClient();
            JsonlLineAccumulator lineAccumulator = new JsonlLineAccumulator();

            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .stream()
                    .content()
                    .flatMap(chunk -> Flux.fromIterable(lineAccumulator.accumulate(chunk)))
                    .concatWith(Flux.defer(() -> Flux.fromIterable(lineAccumulator.flush())))
                    .map(this::parseStreamLine);
        });
    }

    private A2UiMessage parseStreamLine(String line) {
        try {
            return messageParser.parseLine(line, 0);
        } catch (A2UiParseException e) {
            throw new SurfaceExecutionException(
                    "Failed to parse streaming A2UI message: " + e.getMessage(),
                    SurfaceErrorCodes.TRANSFORM_PARSE_FAILED, null);
        }
    }

    private static class JsonlLineAccumulator {
        private final StringBuilder buffer = new StringBuilder();

        List<String> accumulate(String chunk) {
            buffer.append(chunk);
            List<String> completeLines = new java.util.ArrayList<>();
            int newlineIndex;
            while ((newlineIndex = buffer.indexOf("\n")) >= 0) {
                String line = buffer.substring(0, newlineIndex).trim();
                if (!line.isEmpty()) {
                    completeLines.add(line);
                }
                buffer.delete(0, newlineIndex + 1);
            }
            return completeLines;
        }

        List<String> flush() {
            String remaining = buffer.toString().trim();
            if (!remaining.isEmpty()) {
                return List.of(remaining);
            }
            return List.of();
        }
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
