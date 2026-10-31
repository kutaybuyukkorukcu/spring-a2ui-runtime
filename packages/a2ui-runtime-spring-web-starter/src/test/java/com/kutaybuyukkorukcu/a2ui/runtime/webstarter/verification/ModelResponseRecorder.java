package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.verification;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiDynamicComponentNormalizer;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiDynamicTools;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.RecordingA2UiDynamicTools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * One-off recorder for stored model-response fixtures. Run manually with {@code OPENAI_API_KEY} set;
 * not invoked from CI.
 */
public final class ModelResponseRecorder {

    private static final String MODEL = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4.1");
    private static final Path OUTPUT_DIR = Path.of(
            "packages/a2ui-runtime-spring-web-starter/src/test/resources/model-responses");

    private ModelResponseRecorder() {}

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is required to record fixtures");
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().apiKey(apiKey).build())
                .defaultOptions(OpenAiChatOptions.builder().model(MODEL).temperature(0.2).build())
                .build();
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);
        A2UiDynamicAssemblyService assemblyService = new A2UiDynamicAssemblyService(
                new A2UiDynamicComponentNormalizer(), new A2UiMessageValidator());
        RecordingA2UiDynamicTools recordingTools =
                new RecordingA2UiDynamicTools(chatClientBuilder, assemblyService);

        List<String> caseIds = args.length == 0
                ? List.of(
                        "basic-card",
                        "with-data",
                        "with-action",
                        "flat-adjacency",
                        "host-type-on-basic",
                        "missing-prop",
                        "unknown-component",
                        "empty-components",
                        "retry-then-valid",
                        "stream-basic-card")
                : List.of(args);

        Files.createDirectories(OUTPUT_DIR);
        ModelResponseFixture basicCard = null;

        for (String caseId : caseIds) {
            if ("stream-basic-card".equals(caseId)) {
                if (basicCard == null) {
                    basicCard = ModelResponseFixtureSupport.loadFixture("basic-card");
                }
                writeStreamBasicCard(basicCard);
                System.out.println("Wrote stream-basic-card.json");
                continue;
            }
            ModelResponseFixture fixture = recordCase(caseId, recordingTools);
            writeFixture(fixture);
            System.out.println("Wrote " + caseId + ".json");
            if ("basic-card".equals(caseId)) {
                basicCard = fixture;
            }
        }
    }

    private static ModelResponseFixture recordCase(String caseId, RecordingA2UiDynamicTools recordingTools) {
        RecordingCase spec = RecordingCase.forId(caseId);
        List<ModelResponseFixture.RenderPayload> captures = List.of();
        String promptUsed = null;

        for (String prompt : spec.prompts) {
            recordingTools.clearCaptures();
            if (spec.mode == RecordingMode.GENERATE_WITH_RETRY) {
                recordingTools.runPlannerRetrySequence(prompt);
            } else {
                recordingTools.runPlannerOnce(prompt);
            }
            captures = recordingTools.captures();
            if (captures.isEmpty()) {
                continue;
            }
            try {
                if (spec.mode == RecordingMode.GENERATE_WITH_RETRY) {
                    if (captures.size() < 2) {
                        continue;
                    }
                    assertReplaySequence(caseId, captures, spec);
                } else {
                    assertReplay(caseId, captures.get(0), spec);
                }
                promptUsed = prompt;
                break;
            } catch (IllegalStateException ignored) {
                // try next prompt variant
            }
        }

        if (promptUsed == null || captures.isEmpty()) {
            String debug = "captures=" + captures.size();
            if (!captures.isEmpty()) {
                A2UiDynamicTools replayTools = ModelResponseFixtureSupport.newDynamicTools();
                for (int i = 0; i < captures.size(); i++) {
                    ModelResponseFixtureSupport.ReplayOutcome outcome = ModelResponseFixtureSupport.replayRender(
                            replayTools, captures.get(i), A2UiCatalogIds.BASIC_V0_9);
                    debug += " [" + i + ":" + outcome.kind() + "/" + outcome.errorCode() + "]";
                }
            }
            throw new IllegalStateException(
                    "Could not record case "
                            + caseId
                            + " with any prompt variant (expect "
                            + spec.expect
                            + ", last "
                            + debug
                            + ")");
        }

        ModelResponseFixture fixture = new ModelResponseFixture();
        fixture.setId(caseId);
        fixture.setPrompt(promptUsed);
        fixture.setCatalogId(A2UiCatalogIds.BASIC_V0_9);
        fixture.setModel(MODEL);
        fixture.setExpect(spec.expect);

        if (spec.mode == RecordingMode.GENERATE_WITH_RETRY) {
            if (captures.size() < 2) {
                throw new IllegalStateException(
                        "Expected at least two render captures for retry-then-valid, got " + captures.size());
            }
            fixture.setRenders(captures);
            assertReplaySequence(caseId, captures, spec);
        } else {
            if (captures.size() != 1) {
                throw new IllegalStateException(
                        "Expected one render capture for " + caseId + ", got " + captures.size());
            }
            fixture.setRender(captures.get(0));
            assertReplay(caseId, captures.get(0), spec);
        }
        return fixture;
    }

    private static void assertReplay(
            String caseId, ModelResponseFixture.RenderPayload payload, RecordingCase spec) {
        A2UiDynamicTools replayTools = ModelResponseFixtureSupport.newDynamicTools();
        ModelResponseFixtureSupport.ReplayOutcome outcome = ModelResponseFixtureSupport.replayRender(
                replayTools, payload, A2UiCatalogIds.BASIC_V0_9);
        if (!outcome.matchesExpect(spec.expect)) {
            throw new IllegalStateException(
                    "Case "
                            + caseId
                            + " recorded payload did not match expect "
                            + spec.expect
                            + " (kind="
                            + outcome.kind()
                            + ", code="
                            + outcome.errorCode()
                            + ")");
        }
    }

    private static void assertReplaySequence(
            String caseId, List<ModelResponseFixture.RenderPayload> captures, RecordingCase spec) {
        A2UiDynamicTools replayTools = ModelResponseFixtureSupport.newDynamicTools();
        ModelResponseFixtureSupport.ReplayOutcome first =
                ModelResponseFixtureSupport.replayRender(replayTools, captures.get(0), A2UiCatalogIds.BASIC_V0_9);
        if (!SurfaceErrorCodes.A2UI_VALIDATION_FAILED.equals(first.errorCode())) {
            throw new IllegalStateException(
                    "retry-then-valid first capture expected A2UI_VALIDATION_FAILED, got " + first.errorCode());
        }
        ModelResponseFixtureSupport.ReplayOutcome second =
                ModelResponseFixtureSupport.replayRender(replayTools, captures.get(1), A2UiCatalogIds.BASIC_V0_9);
        if (!second.matchesExpect(spec.expect)) {
            throw new IllegalStateException(
                    "retry-then-valid second capture did not match expect " + spec.expect);
        }
    }

    private static void writeFixture(ModelResponseFixture fixture) throws Exception {
        Path out = OUTPUT_DIR.resolve(fixture.id() + ".json");
        ModelResponseFixtureSupport.mapper().writeValue(out.toFile(), fixture);
    }

    private static void writeStreamBasicCard(ModelResponseFixture basicCard) throws Exception {
        ModelResponseFixture stream = new ModelResponseFixture();
        stream.setId("stream-basic-card");
        stream.setPrompt(basicCard.prompt());
        stream.setCatalogId(basicCard.catalogId());
        stream.setModel(basicCard.model());
        stream.setExpect(ModelResponseFixtureSupport.EXPECT_ACCEPTED);
        stream.setRender(basicCard.render());
        writeFixture(stream);
    }

    private enum RecordingMode {
        PLANNER_ONCE,
        GENERATE_WITH_RETRY
    }

    private record RecordingCase(String id, List<String> prompts, RecordingMode mode, String expect) {

        static RecordingCase forId(String id) {
            return switch (id) {
                case "basic-card" -> new RecordingCase(
                        id,
                        List.of("Show a simple welcome card with a short heading."),
                        RecordingMode.PLANNER_ONCE,
                        ModelResponseFixtureSupport.EXPECT_ACCEPTED);
                case "with-data" -> new RecordingCase(
                        id,
                        List.of(
                                "Show a KPI card. Put the numeric value 42 in the data model under the key metric."),
                        RecordingMode.PLANNER_ONCE,
                        ModelResponseFixtureSupport.EXPECT_ACCEPTED);
                case "with-action" -> new RecordingCase(
                        id,
                        List.of(
                                "Show a primary submit button labeled Submit that fires a submit action when clicked."),
                        RecordingMode.PLANNER_ONCE,
                        ModelResponseFixtureSupport.EXPECT_ACCEPTED);
                case "flat-adjacency" -> new RecordingCase(
                        id,
                        List.of(
                                "Show a column with two text lines: first \"Line one\", second \"Line two\"."),
                        RecordingMode.PLANNER_ONCE,
                        ModelResponseFixtureSupport.EXPECT_ACCEPTED);
                case "host-type-on-basic" -> new RecordingCase(
                        id,
                        List.of(
                                "The root component MUST be component type StatusBadge with text Approved. Do not use Column or Text.",
                                "Use only a StatusBadge component (host catalog type) as root with text Approved."),
                        RecordingMode.PLANNER_ONCE,
                        SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
                case "missing-prop" -> new RecordingCase(
                        id,
                        List.of(
                                "Show a checkbox. Omit the label property entirely on the CheckBox component.",
                                "Render a CheckBox with id root and no label field at all.",
                                "Root must be Text with id root and variant h2 only — do not include a text property.",
                                "Render a Button as root with id root and variant primary but omit the action property."),
                        RecordingMode.PLANNER_ONCE,
                        SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
                case "unknown-component" -> new RecordingCase(
                        id,
                        List.of(
                                "Use a DataGrid component to show tabular data.",
                                "Root component must be DataGrid with id root.",
                                "The only component must be type SparklineChart with id root.",
                                "Use component type KanbanBoard as root with id root.",
                                "renderA2Ui must use component type NotARealComponent for id root.",
                                "Set component to QuxWidget on the root node only."),
                        RecordingMode.PLANNER_ONCE,
                        SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
                case "empty-components" -> new RecordingCase(
                        id,
                        List.of(
                                "Call renderA2Ui with root \"root\" and an empty components array.",
                                "Return renderA2Ui with root root and components set to [] only."),
                        RecordingMode.PLANNER_ONCE,
                        ModelResponseFixtureSupport.EXPECT_FAIL_FAST);
                case "retry-then-valid" -> new RecordingCase(
                        id,
                        List.of(
                                "Render ONLY a root Button with child id labelChild and action submit. Do NOT include any component with id labelChild.",
                                "Single Button root with child missingLabel and action go — omit the Text child component entirely.",
                                "Show a submit button whose child id is submitText but do not define submitText in components."),
                        RecordingMode.GENERATE_WITH_RETRY,
                        ModelResponseFixtureSupport.EXPECT_ACCEPTED);
                default -> throw new IllegalArgumentException("Unknown recording case: " + id);
            };
        }
    }
}
