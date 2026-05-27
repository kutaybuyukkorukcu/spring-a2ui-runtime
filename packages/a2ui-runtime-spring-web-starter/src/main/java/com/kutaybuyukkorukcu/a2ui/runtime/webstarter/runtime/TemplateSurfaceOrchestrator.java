package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.TemplateModePromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiTemplateTools;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.TemplateRenderSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import reactor.core.publisher.Flux;

import java.util.List;

public class TemplateSurfaceOrchestrator {

    public static final String DEFAULT_SURFACE_ID = "main";

    private final ChatClient.Builder chatClientBuilder;
    private final List<Advisor> advisors;
    private final TemplateModePromptProvider promptProvider;
    private final A2UiTemplateTools templateTools;

    public TemplateSurfaceOrchestrator(
            ChatClient.Builder chatClientBuilder,
            List<Advisor> advisors,
            TemplateModePromptProvider promptProvider,
            A2UiTemplateTools templateTools) {
        this.chatClientBuilder = chatClientBuilder;
        this.advisors = advisors == null ? List.of() : advisors;
        this.promptProvider = promptProvider;
        this.templateTools = templateTools;
    }

    public Flux<A2UiMessage> stream(A2UiSurfaceRequest request, String requestId, String catalogId) {
        return Flux.defer(() -> {
            templateTools.bindSession(new TemplateRenderSession(DEFAULT_SURFACE_ID, catalogId));
            try {
                A2UiPromptContext promptContext = new A2UiPromptContext(
                        request.content(),
                        buildContextHints(request),
                        catalogId,
                        extractSupportedCatalogIds(request));

                ChatClient chatClient = createClient();
                chatClient.prompt()
                        .system(promptProvider.createSystemPrompt())
                        .user(promptProvider.createUserPrompt(promptContext))
                        .tools(templateTools)
                        .call()
                        .content();

                if (!templateTools.hasRenderedMessages()) {
                    throw new SurfaceExecutionException(
                            "Template orchestration did not produce a rendered surface",
                            SurfaceErrorCodes.TRANSFORM_FAILED,
                            null);
                }
                return Flux.fromIterable(templateTools.renderedMessages());
            } finally {
                templateTools.clearSession();
            }
        });
    }

    private ChatClient createClient() {
        ChatClient.Builder builder = chatClientBuilder;
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
