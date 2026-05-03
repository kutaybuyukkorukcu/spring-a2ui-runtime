package com.fogui.contract.a2ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("A2UI Message Validator Fixtures")
class A2UiMessageValidatorFixtureTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final A2UiMessageValidator VALIDATOR = new A2UiMessageValidator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureCases")
    void shouldValidateFixtureDeterministically(String fixtureName, A2UiMessageFixture fixture) {
        List<A2UiValidationError> firstPass =
                VALIDATOR.validate(fixture.messages(), A2UiValidationContext.forVersion(fixture.requestedVersion()));
        List<A2UiValidationError> secondPass =
                VALIDATOR.validate(fixture.messages(), A2UiValidationContext.forVersion(fixture.requestedVersion()));

        assertEquals(firstPass, secondPass, "validation output should be deterministic");
        assertEquals(
                fixture.expectedErrors().stream().map(FixtureError::path).toList(),
                firstPass.stream().map(A2UiValidationError::getPath).toList(),
                "error paths should match fixture");
        assertEquals(
                fixture.expectedErrors().stream().map(FixtureError::code).toList(),
                firstPass.stream().map(A2UiValidationError::getCode).toList(),
                "error codes should match fixture");
        assertEquals(
                fixture.expectedErrors().stream().map(FixtureError::category).toList(),
                firstPass.stream().map(A2UiValidationError::getCategory).toList(),
                "error categories should match fixture");
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> fixtureCases()
            throws IOException, URISyntaxException {
        Path fixturesRoot = Path.of(
                A2UiMessageValidatorFixtureTest.class
                        .getClassLoader()
                        .getResource("fixtures/a2ui/messages")
                        .toURI());

        return Files.walk(fixturesRoot)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing(Path::toString))
                .map(A2UiMessageValidatorFixtureTest::toArguments);
    }

    private static org.junit.jupiter.params.provider.Arguments toArguments(Path path) {
        try {
            A2UiMessageFixture fixture = OBJECT_MAPPER.readValue(path.toFile(), A2UiMessageFixture.class);
            return org.junit.jupiter.params.provider.Arguments.of(fixture.name(), fixture);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse fixture: " + path, ex);
        }
    }

    private record A2UiMessageFixture(
            String name,
            String requestedVersion,
            List<A2UiMessage> messages,
            List<FixtureError> expectedErrors
    ) {}

    private record FixtureError(String path, String code, String category) {}
}