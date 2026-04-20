package com.fogui.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.model.fogui.GenerativeUIResponse;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Canonical Contract Conformance Fixtures")
class CanonicalConformanceFixtureTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final FogUiCanonicalValidator VALIDATOR = new FogUiCanonicalValidator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureCases")
    void shouldValidateFixtureDeterministically(String fixtureName, CanonicalFixture fixture) {
        GenerativeUIResponse payload = OBJECT_MAPPER.convertValue(fixture.payload(), GenerativeUIResponse.class);
        CanonicalValidationContext context = fixture.context() == null
                ? CanonicalValidationContext.empty()
                : CanonicalValidationContext.builder()
                .expectedContractVersion(fixture.context().expectedContractVersion())
                .build();

        List<CanonicalValidationError> firstPass = VALIDATOR.validate(payload, context);
        List<CanonicalValidationError> secondPass = VALIDATOR.validate(payload, context);

        // Determinism guard: same input and context produce the same ordered errors.
        assertEquals(firstPass, secondPass, "validation output should be deterministic");
        assertEquals(fixture.expectedValid(), firstPass.isEmpty());

        if (fixture.expectedErrors() == null || fixture.expectedErrors().isEmpty()) {
            assertTrue(firstPass.isEmpty(), "no errors expected for valid fixture");
            return;
        }

        assertEquals(
                fixture.expectedErrors().stream().map(FixtureError::code).toList(),
                firstPass.stream().map(CanonicalValidationError::getCode).toList(),
                "error codes should match fixture");
        assertEquals(
                fixture.expectedErrors().stream().map(FixtureError::path).toList(),
                firstPass.stream().map(CanonicalValidationError::getPath).toList(),
                "error paths should match fixture");
        assertEquals(
                fixture.expectedErrors().stream().map(FixtureError::category).toList(),
                firstPass.stream().map(CanonicalValidationError::getCategory).toList(),
                "error categories should match fixture");
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> fixtureCases() throws IOException, URISyntaxException {
        Path fixturesRoot = Path.of(CanonicalConformanceFixtureTest.class.getClassLoader()
                .getResource("fixtures/canonical")
                .toURI());

        return Files.walk(fixturesRoot)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing(Path::toString))
                .map(path -> toArguments(path));
    }

    private static org.junit.jupiter.params.provider.Arguments toArguments(Path path) {
        try {
            CanonicalFixture fixture = OBJECT_MAPPER.readValue(path.toFile(), CanonicalFixture.class);
            return org.junit.jupiter.params.provider.Arguments.of(fixture.name(), fixture);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse fixture: " + path, ex);
        }
    }

    private record CanonicalFixture(
            String name,
            boolean expectedValid,
            FixtureContext context,
            Map<String, Object> payload,
            List<FixtureError> expectedErrors
    ) {
    }

    private record FixtureContext(String expectedContractVersion) {
    }

    private record FixtureError(String path, String code, String category) {
    }
}
