package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.verification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiDynamicComponentNormalizer;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEventCollector;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.RenderA2UiArgs;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiDynamicTools;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.DynamicRenderSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ModelResponseFixtureSupport {

    public static final String NEGOTIATED_SURFACE_ID = "main";
    public static final String EXPECT_ACCEPTED = "accepted";
    public static final String EXPECT_FAIL_FAST = "fail-fast";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private ModelResponseFixtureSupport() {}

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static List<ModelResponseFixture> loadAllFixtures() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:model-responses/*.json");
        List<ModelResponseFixture> fixtures = new ArrayList<>();
        for (Resource resource : resources) {
            fixtures.add(loadFixture(resource));
        }
        fixtures.sort(Comparator.comparing(ModelResponseFixture::id));
        return fixtures;
    }

    public static ModelResponseFixture loadFixture(String id) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource("classpath:model-responses/" + id + ".json");
        if (!resource.exists()) {
            throw new IOException("Missing model response fixture: " + id);
        }
        return loadFixture(resource);
    }

    private static ModelResponseFixture loadFixture(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            ModelResponseFixture fixture = MAPPER.readValue(in, ModelResponseFixture.class);
            if (fixture.id() == null || fixture.id().isBlank()) {
                throw new IOException("Fixture id is required in " + resource.getFilename());
            }
            if (fixture.renderSequence().isEmpty()) {
                throw new IOException("Fixture " + fixture.id() + " has no render payload");
            }
            return fixture;
        }
    }

    public static A2UiDynamicTools newDynamicTools() {
        A2UiDynamicAssemblyService assemblyService = new A2UiDynamicAssemblyService(
                new A2UiDynamicComponentNormalizer(), new A2UiMessageValidator());
        ChatModel unusedChatModel = prompt -> {
            throw new UnsupportedOperationException("replay does not call the model");
        };
        return new A2UiDynamicTools(
                ChatClient.builder(unusedChatModel),
                List.of(),
                new com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DynamicA2UiPromptProvider(),
                assemblyService,
                com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics.noop(),
                com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry.shared());
    }

    public static ReplayOutcome replayRender(
            A2UiDynamicTools dynamicTools, ModelResponseFixture.RenderPayload payload, String catalogId) {
        DynamicRenderSession session = new DynamicRenderSession(
                NEGOTIATED_SURFACE_ID,
                catalogId,
                "replay",
                null,
                A2UiRuntimeEventCollector.DISABLED);
        ToolContext toolContext = new ToolContext(Map.of(A2UiDynamicTools.SESSION_CONTEXT_KEY, session));
        try {
            dynamicTools.renderA2Ui(
                    payload.surfaceId(),
                    payload.root(),
                    payload.components(),
                    payload.data(),
                    toolContext);
            if (!session.hasRenderedMessages()) {
                return ReplayOutcome.failFast(null);
            }
            return ReplayOutcome.accepted(session.renderedMessages());
        } catch (SurfaceExecutionException ex) {
            return ReplayOutcome.validationOrTransform(ex);
        }
    }

    public static ReplayOutcome replayAssemble(ModelResponseFixture.RenderPayload payload, String catalogId) {
        A2UiDynamicAssemblyService assemblyService = new A2UiDynamicAssemblyService(
                new A2UiDynamicComponentNormalizer(), new A2UiMessageValidator());
        RenderA2UiArgs args = new RenderA2UiArgs(
                payload.surfaceId(), payload.root(), payload.components(), payload.data());
        try {
            List<A2UiMessage> messages =
                    assemblyService.assemble(args, catalogId, NEGOTIATED_SURFACE_ID);
            return ReplayOutcome.accepted(messages);
        } catch (SurfaceExecutionException ex) {
            return ReplayOutcome.validationOrTransform(ex);
        }
    }

    public static String resolvedCatalogId(ModelResponseFixture fixture) {
        return fixture.catalogId() == null || fixture.catalogId().isBlank()
                ? A2UiCatalogIds.BASIC_V0_9
                : fixture.catalogId();
    }

    public record ReplayOutcome(String kind, String errorCode, List<A2UiMessage> messages) {

        static ReplayOutcome accepted(List<A2UiMessage> messages) {
            return new ReplayOutcome(EXPECT_ACCEPTED, null, messages);
        }

        static ReplayOutcome failFast(String errorCode) {
            return new ReplayOutcome(EXPECT_FAIL_FAST, errorCode, List.of());
        }

        static ReplayOutcome validationOrTransform(SurfaceExecutionException ex) {
            String code = ex.getErrorCode();
            if (SurfaceErrorCodes.A2UI_VALIDATION_FAILED.equals(code)) {
                return new ReplayOutcome(SurfaceErrorCodes.A2UI_VALIDATION_FAILED, code, List.of());
            }
            return failFast(code);
        }

        boolean matchesExpect(String expect) {
            if (EXPECT_ACCEPTED.equals(expect)) {
                return EXPECT_ACCEPTED.equals(kind) && messages != null && !messages.isEmpty();
            }
            if (EXPECT_FAIL_FAST.equals(expect)) {
                return !EXPECT_ACCEPTED.equals(kind);
            }
            return expect != null && expect.equals(errorCode);
        }
    }
}
