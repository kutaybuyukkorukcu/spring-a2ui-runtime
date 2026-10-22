package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.verification;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiDynamicTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StoredModelResponseReplayTest {

    private static List<ModelResponseFixture> fixtures;
    private static A2UiDynamicTools dynamicTools;

    @BeforeAll
    static void loadFixtures() throws IOException {
        fixtures = ModelResponseFixtureSupport.loadAllFixtures();
        dynamicTools = ModelResponseFixtureSupport.newDynamicTools();
        assertThat(fixtures).hasSizeGreaterThanOrEqualTo(9);
    }

    static Stream<ModelResponseFixture> assemblyFixtures() {
        return fixtures.stream().filter(fixture -> !"stream-basic-card".equals(fixture.id()));
    }

    @ParameterizedTest
    @MethodSource("assemblyFixtures")
    void shouldReplayStoredRenderThroughRealRenderA2Ui(ModelResponseFixture fixture) {
        String catalogId = ModelResponseFixtureSupport.resolvedCatalogId(fixture);
        List<ModelResponseFixture.RenderPayload> sequence = fixture.renderSequence();

        if ("retry-then-valid".equals(fixture.id())) {
            assertThat(sequence).hasSize(2);
            ModelResponseFixtureSupport.ReplayOutcome first =
                    ModelResponseFixtureSupport.replayRender(dynamicTools, sequence.get(0), catalogId);
            assertThat(first.errorCode()).isEqualTo(SurfaceErrorCodes.A2UI_VALIDATION_FAILED);

            ModelResponseFixtureSupport.ReplayOutcome second =
                    ModelResponseFixtureSupport.replayRender(dynamicTools, sequence.get(1), catalogId);
            assertThat(second.kind()).isEqualTo(ModelResponseFixtureSupport.EXPECT_ACCEPTED);
            assertThat(second.messages().get(0)).isInstanceOf(A2UiMessage.CreateSurface.class);
            return;
        }

        ModelResponseFixtureSupport.ReplayOutcome outcome =
                ModelResponseFixtureSupport.replayRender(dynamicTools, sequence.get(0), catalogId);
        assertThat(outcome.matchesExpect(fixture.expect()))
                .as("fixture %s expect %s got kind %s code %s", fixture.id(), fixture.expect(), outcome.kind(), outcome.errorCode())
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("assemblyFixtures")
    void shouldReplayStoredRenderThroughRealAssemble(ModelResponseFixture fixture) {
        String catalogId = ModelResponseFixtureSupport.resolvedCatalogId(fixture);

        if ("retry-then-valid".equals(fixture.id())) {
            List<ModelResponseFixture.RenderPayload> sequence = fixture.renderSequence();
            assertThat(ModelResponseFixtureSupport.replayAssemble(sequence.get(0), catalogId).errorCode())
                    .isEqualTo(SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
            assertThat(ModelResponseFixtureSupport.replayAssemble(sequence.get(1), catalogId).kind())
                    .isEqualTo(ModelResponseFixtureSupport.EXPECT_ACCEPTED);
            return;
        }

        ModelResponseFixtureSupport.ReplayOutcome outcome = ModelResponseFixtureSupport.replayAssemble(
                fixture.renderSequence().get(0), catalogId);
        assertThat(outcome.matchesExpect(fixture.expect())).isTrue();
    }

    @Test
    void shouldLoadEveryExpectedCaseId() {
        List<String> ids = fixtures.stream().map(ModelResponseFixture::id).sorted().toList();
        assertThat(ids).containsExactlyInAnyOrder(
                "basic-card",
                "with-data",
                "with-action",
                "flat-adjacency",
                "host-type-on-basic",
                "missing-prop",
                "unknown-component",
                "empty-components",
                "retry-then-valid",
                "stream-basic-card");
    }
}
