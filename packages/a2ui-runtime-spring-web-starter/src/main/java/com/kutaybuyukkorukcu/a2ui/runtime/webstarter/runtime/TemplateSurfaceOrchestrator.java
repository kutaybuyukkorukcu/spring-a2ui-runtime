package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.TemplateModePromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiTemplateTools;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.TemplateRenderSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TemplateSurfaceOrchestrator {

    public static final String DEFAULT_SURFACE_ID = "main";

    private final ChatClient.Builder chatClientBuilder;
    private final List<Advisor> advisors;
    private final TemplateModePromptProvider promptProvider;
    private final A2UiTemplateTools templateTools;
    private final boolean lifecycleEventsEnabled;

    public TemplateSurfaceOrchestrator(
            ChatClient.Builder chatClientBuilder,
            List<Advisor> advisors,
            TemplateModePromptProvider promptProvider,
            A2UiTemplateTools templateTools) {
        this(chatClientBuilder, advisors, promptProvider, templateTools, null);
    }

    public TemplateSurfaceOrchestrator(
            ChatClient.Builder chatClientBuilder,
            List<Advisor> advisors,
            TemplateModePromptProvider promptProvider,
            A2UiTemplateTools templateTools,
            A2UiWebProperties webProperties) {
        this.chatClientBuilder = chatClientBuilder;
        this.advisors = advisors == null ? List.of() : advisors;
        this.promptProvider = promptProvider;
        this.templateTools = templateTools;
        this.lifecycleEventsEnabled = webProperties != null && webProperties.getStream().isLifecycleEvents();
    }

    public Flux<A2UiRuntimeEvent> stream(A2UiSurfaceRequest request, String requestId, String catalogId) {
        return Mono.fromCallable(() -> {
            String runId = requestId;
            A2UiRuntimeEventCollector collector = lifecycleEventsEnabled
                    ? new A2UiRuntimeEventCollector(runId, true)
                    : A2UiRuntimeEventCollector.DISABLED;
            TemplateRenderSession session = new TemplateRenderSession(DEFAULT_SURFACE_ID, catalogId, collector);
            A2UiPromptContext promptContext = new A2UiPromptContext(
                    request.content(),
                    buildContextHints(request),
                    catalogId,
                    extractSupportedCatalogIds(request));

            ChatClient chatClient = createClient();
            String assistantContent = chatClient.prompt()
                    .system(promptProvider.createSystemPrompt())
                    .user(promptProvider.createUserPrompt(promptContext))
                    .tools(templateTools)
                    .toolContext(Map.of(A2UiTemplateTools.SESSION_CONTEXT_KEY, session))
                    .call()
                    .content();
            collector.assistantText(assistantContent);

            if (!session.hasRenderedMessages()) {
                throw new SurfaceExecutionException(
                        "Template orchestration did not produce a rendered surface",
                        SurfaceErrorCodes.TRANSFORM_FAILED,
                        null);
            }
            return toRuntimeEvents(collector, session.renderedMessages());
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
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

    private ChatClient createClient() {
        ChatClient.Builder builder = chatClientBuilder.clone();
        for (Advisor advisor : advisors) {
            builder = builder.defaultAdvisors(advisor);
        }
        return builder.build();
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
