package com.genui.contract.a2ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genui.contract.FogUiCanonicalContract;
import com.genui.model.genui.ContentBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("A2UI Compatibility Fixtures")
class A2UiCompatibilityFixtureTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final A2UiInboundTranslator TRANSLATOR = new A2UiInboundTranslator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureCases")
    void shouldTranslateFixtureDeterministically(String fixtureName, A2UiFixture fixture) {
        A2UiTranslationResult firstPass = TRANSLATOR.translate(fixture.payload());
        A2UiTranslationResult secondPass = TRANSLATOR.translate(fixture.payload());

        assertEquals(firstPass, secondPass, "translation output should be deterministic");
        assertEquals(fixture.expectedThinkingCount(), firstPass.getResponse().getThinking().size());
        assertEquals(fixture.expectedBlocks().size(), firstPass.getResponse().getContent().size());
        assertEquals("a2ui", firstPass.getResponse().getMetadata().get("sourceProtocol"));
        assertEquals(
                A2UiInboundTranslator.SUPPORTED_A2UI_VERSION,
                firstPass.getResponse().getMetadata().get("supportedVersion"));
        assertEquals(
                FogUiCanonicalContract.CURRENT_CONTRACT_VERSION,
                firstPass.getResponse().getMetadata().get(FogUiCanonicalContract.METADATA_CONTRACT_VERSION_KEY));

        assertEquals(
                fixture.expectedErrors().stream().map(FixtureError::path).toList(),
                firstPass.getErrors().stream().map(A2UiTranslationError::getPath).toList(),
                "error paths should match fixture");
        assertEquals(
                fixture.expectedErrors().stream().map(FixtureError::code).toList(),
                firstPass.getErrors().stream().map(A2UiTranslationError::getCode).toList(),
                "error codes should match fixture");
        assertEquals(
                fixture.expectedErrors().stream().map(FixtureError::category).toList(),
                firstPass.getErrors().stream().map(A2UiTranslationError::getCategory).toList(),
                "error categories should match fixture");

        for (int index = 0; index < fixture.expectedBlocks().size(); index++) {
            FixtureBlock expectedBlock = fixture.expectedBlocks().get(index);
            ContentBlock actualBlock = firstPass.getResponse().getContent().get(index);

            assertEquals(expectedBlock.type(), actualBlock.getType(), "block type should match fixture");
            if (expectedBlock.componentType() != null) {
                assertEquals(expectedBlock.componentType(), actualBlock.getComponentType(), "component type should match fixture");
            }
            if (expectedBlock.value() != null) {
                assertEquals(expectedBlock.value(), actualBlock.getValue(), "text value should match fixture");
            }
            if (expectedBlock.fallbackReason() != null) {
                Map<?, ?> props = (Map<?, ?>) actualBlock.getProps();
                assertEquals(expectedBlock.fallbackReason(), props.get("reason"), "fallback reason should match fixture");
            }
        }
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> fixtureCases() throws IOException, URISyntaxException {
        Path fixturesRoot = Path.of(A2UiCompatibilityFixtureTest.class.getClassLoader()
                .getResource("fixtures/a2ui")
                .toURI());

        return Files.walk(fixturesRoot)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing(Path::toString))
                .map(A2UiCompatibilityFixtureTest::toArguments);
    }

    private static org.junit.jupiter.params.provider.Arguments toArguments(Path path) {
        try {
            A2UiFixture fixture = OBJECT_MAPPER.readValue(path.toFile(), A2UiFixture.class);
            return org.junit.jupiter.params.provider.Arguments.of(fixture.name(), fixture);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse fixture: " + path, ex);
        }
    }

    private record A2UiFixture(
            String name,
            Map<String, Object> payload,
            int expectedThinkingCount,
            List<FixtureBlock> expectedBlocks,
            List<FixtureError> expectedErrors
    ) {
    }

    private record FixtureBlock(String type, String componentType, String value, String fallbackReason) {
    }

    private record FixtureError(String path, String code, String category) {
    }
}