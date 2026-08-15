package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class SpringAiSurfaceRuntime implements A2UiSurfaceRuntime {

    private final ChatClient.Builder chatClientBuilder;
    private final List<Advisor> advisors;
    private final Environment environment;
    private final A2UiWebProperties properties;
    private final GenerationModeAdapter templateAdapter;
    private final GenerationModeAdapter dynamicAdapter;
    private final boolean lifecycleEventsEnabled;

    public SpringAiSurfaceRuntime(
            ChatClient.Builder chatClientBuilder,
            List<Advisor> advisors,
            Environment environment,
            A2UiWebProperties properties,
            GenerationModeAdapter templateAdapter,
            GenerationModeAdapter dynamicAdapter) {
        this.chatClientBuilder = chatClientBuilder;
        this.advisors = advisors == null ? List.of() : advisors;
        this.environment = environment;
        this.properties = properties;
        this.templateAdapter = templateAdapter;
        this.dynamicAdapter = dynamicAdapter;
        this.lifecycleEventsEnabled = properties != null && properties.getStream().isLifecycleEvents();
    }

    @Override
    public String getActiveModelName() {
        if (properties.getRuntime().getModelName() != null) {
            return properties.getRuntime().getModelName();
        }
        return environment.getProperty("spring.ai.chat.options.model", "unknown");
    }

    @Override
    public Flux<A2UiRuntimeEvent> stream(A2UiSurfaceRequest request, String requestId, String catalogId) {
        GenerationModeAdapter adapter = isTemplateMode() ? templateAdapter : dynamicAdapter;
        return Mono.fromCallable(() -> {
                    A2UiRuntimeEventCollector collector = lifecycleEventsEnabled
                            ? new A2UiRuntimeEventCollector(requestId, true)
                            : A2UiRuntimeEventCollector.DISABLED;
                    A2UiPromptContext promptContext = new A2UiPromptContext(
                            request.content(),
                            buildContextHints(request),
                            catalogId,
                            extractSupportedCatalogIds(request));
                    ChatClient chatClient = createClient();
                    List<A2UiMessage> renderedMessages = adapter.generate(chatClient, promptContext, collector);
                    if (renderedMessages == null || renderedMessages.isEmpty()) {
                        throw new SurfaceExecutionException(
                                adapter.missingSurfaceMessage(),
                                SurfaceErrorCodes.TRANSFORM_FAILED,
                                null);
                    }
                    return toRuntimeEvents(collector, renderedMessages);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    private boolean isTemplateMode() {
        return properties != null && "template".equalsIgnoreCase(properties.getRuntime().getGenerationMode());
    }

    private ChatClient createClient() {
        ChatClient.Builder builder = chatClientBuilder.clone();
        for (Advisor advisor : advisors) {
            builder = builder.defaultAdvisors(advisor);
        }
        return builder.build();
    }

    private static List<A2UiRuntimeEvent> toRuntimeEvents(
            A2UiRuntimeEventCollector collector,
            List<A2UiMessage> renderedMessages) {
        List<A2UiRuntimeEvent> events = new ArrayList<>(collector.drain());
        for (A2UiMessage message : renderedMessages) {
            events.add(new A2UiRuntimeEvent.Surface(message));
        }
        return events;
    }

    private static String buildContextHints(A2UiSurfaceRequest request) {
        if (request.context() == null) {
            return null;
        }
        StringBuilder hints = new StringBuilder();
        if (request.context().intent() != null) {
            hints.append("Intent: ").append(request.context().intent()).append(". ");
        }
        if (request.context().preferredComponents() != null && !request.context().preferredComponents().isEmpty()) {
            hints.append("Preferred components: ")
                    .append(String.join(", ", request.context().preferredComponents()))
                    .append(". ");
        }
        if (request.context().instructions() != null) {
            hints.append(request.context().instructions());
        }
        String value = hints.toString().trim();
        return value.isEmpty() ? null : value;
    }

    private static List<String> extractSupportedCatalogIds(A2UiSurfaceRequest request) {
        if (request.a2uiClientCapabilities() == null) {
            return List.of();
        }
        List<String> ids = request.a2uiClientCapabilities().supportedCatalogIds();
        return ids == null ? List.of() : ids;
    }
}
