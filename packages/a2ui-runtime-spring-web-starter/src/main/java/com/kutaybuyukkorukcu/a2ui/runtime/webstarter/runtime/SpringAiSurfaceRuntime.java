package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm.A2UiLlmOutput;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm.A2UiLlmOutputMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import reactor.core.publisher.Flux;

import java.util.List;

public class SpringAiSurfaceRuntime implements A2UiSurfaceRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAiSurfaceRuntime.class);

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
        ChatClient chatClient = createClient();

        A2UiPromptContext promptContext = new A2UiPromptContext(
                request.content(),
                buildContextHints(request),
                catalogId,
                extractSupportedCatalogIds(request)
        );

        String systemPrompt = promptProvider.createSystemPrompt(promptContext);
        String userPrompt = promptProvider.createUserPrompt(promptContext);

        A2UiLlmOutput output = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(A2UiLlmOutput.class);

        if (output == null || output.messages() == null || output.messages().isEmpty()) {
            throw new SurfaceExecutionException(
                    "LLM returned empty or unparseable response",
                    SurfaceErrorCodes.TRANSFORM_FAILED, null);
        }

        List<A2UiMessage> messages = llmOutputMapper.map(output);

        LOGGER.info("Generated {} A2UI messages", messages.size());

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
        builder = builder.defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT);
        for (Advisor advisor : advisors) {
            builder = builder.defaultAdvisors(advisor);
        }
        return builder.build();
    }

    @Override
    public Flux<A2UiMessage> stream(A2UiSurfaceRequest request, String requestId, String catalogId) {
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

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .reduce("", String::concat)
                .flatMapMany(content -> {
                    A2UiLlmOutput output = converter.convert(content);
                    return Flux.fromIterable(llmOutputMapper.map(output));
                });
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