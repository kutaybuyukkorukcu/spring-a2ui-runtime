package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DynamicA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiDynamicTools;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.DynamicRenderSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

public class DynamicSurfaceOrchestrator {

    public static final String DEFAULT_SURFACE_ID = "main";
    public static final String GENERATE_TOOL_NAME = "generateA2Ui";

    private final ChatClient.Builder chatClientBuilder;
    private final List<Advisor> advisors;
    private final DynamicA2UiPromptProvider promptProvider;
    private final A2UiDynamicTools dynamicTools;

    public DynamicSurfaceOrchestrator(
            ChatClient.Builder chatClientBuilder,
            List<Advisor> advisors,
            DynamicA2UiPromptProvider promptProvider,
            A2UiDynamicTools dynamicTools) {
        this.chatClientBuilder = chatClientBuilder;
        this.advisors = advisors == null ? List.of() : advisors;
        this.promptProvider = promptProvider;
        this.dynamicTools = dynamicTools;
    }

    public Flux<A2UiMessage> stream(A2UiSurfaceRequest request, String requestId, String catalogId) {
        return Mono.fromCallable(() -> {
            String contextHints = buildContextHints(request);
            DynamicRenderSession session = new DynamicRenderSession(
                    DEFAULT_SURFACE_ID,
                    catalogId,
                    request.content(),
                    contextHints);
            A2UiPromptContext promptContext = new A2UiPromptContext(
                    request.content(),
                    contextHints,
                    catalogId,
                    extractSupportedCatalogIds(request));

            ChatClient chatClient = createClient();
            chatClient.prompt()
                    .system(promptProvider.createPrimarySystemPrompt())
                    .user(promptProvider.createPrimaryUserPrompt(promptContext))
                    .toolNames(GENERATE_TOOL_NAME)
                    .tools(dynamicTools)
                    .toolContext(Map.of(A2UiDynamicTools.SESSION_CONTEXT_KEY, session))
                    .call()
                    .content();

            if (!session.hasRenderedMessages()) {
                throw new SurfaceExecutionException(
                        "Dynamic orchestration did not produce a rendered surface",
                        SurfaceErrorCodes.TRANSFORM_FAILED,
                        null);
            }
            return session.renderedMessages();
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
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
