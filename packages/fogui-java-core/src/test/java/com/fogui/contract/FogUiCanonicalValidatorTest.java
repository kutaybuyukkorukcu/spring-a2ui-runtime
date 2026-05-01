package com.fogui.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FogUiCanonicalValidatorTest {

  private final FogUiCanonicalValidator validator = new FogUiCanonicalValidator();

  @Test
  void shouldValidateCanonicalPayload() {
    GenerativeUIResponse response =
        GenerativeUIResponse.builder().content(List.of(ContentBlock.text("Hello"))).build();

    assertTrue(validator.isValid(response));
  }

  @Test
  void shouldRejectInvalidTextValueType() {
    ContentBlock invalidText = ContentBlock.builder().type("text").value(123).build();

    GenerativeUIResponse response =
        GenerativeUIResponse.builder().content(List.of(invalidText)).build();

    assertFalse(validator.isValid(response));
  }

  @Test
  void shouldRejectMissingContractVersionWhenExpectedVersionProvided() {
    GenerativeUIResponse response =
        GenerativeUIResponse.builder()
            .thinking(List.of())
            .content(List.of(ContentBlock.text("ok")))
            .build();

    List<CanonicalValidationError> errors =
        validator.validate(
            response,
            CanonicalValidationContext.builder()
                .expectedContractVersion(FogUiCanonicalContract.CURRENT_CONTRACT_VERSION)
                .build());

    assertTrue(errors.stream().anyMatch(e -> "MISSING_CONTRACT_VERSION".equals(e.getCode())));
  }

  @Test
  void shouldRejectContractVersionMismatchWhenExpectedVersionProvided() {
    GenerativeUIResponse response =
        GenerativeUIResponse.builder()
            .thinking(List.of())
            .content(List.of(ContentBlock.text("ok")))
            .metadata(Map.of(FogUiCanonicalContract.METADATA_CONTRACT_VERSION_KEY, "fogui/0.9"))
            .build();

    List<CanonicalValidationError> errors =
        validator.validate(
            response,
            CanonicalValidationContext.builder()
                .expectedContractVersion(FogUiCanonicalContract.CURRENT_CONTRACT_VERSION)
                .build());

    CanonicalValidationError mismatch =
        errors.stream()
            .filter(e -> "CONTRACT_VERSION_MISMATCH".equals(e.getCode()))
            .findFirst()
            .orElseThrow();

    assertEquals("VALIDATION", mismatch.getCategory());
    assertEquals(
        FogUiCanonicalContract.CURRENT_CONTRACT_VERSION,
        mismatch.getDetails().get("expectedContractVersion"));
    assertEquals("fogui/0.9", mismatch.getDetails().get("actualContractVersion"));
  }

  @Test
  void shouldPassExpectedContractVersionValidationWhenVersionMatches() {
    GenerativeUIResponse response =
        GenerativeUIResponse.builder()
            .thinking(List.of())
            .content(List.of(ContentBlock.text("ok")))
            .metadata(
                Map.of(
                    FogUiCanonicalContract.METADATA_CONTRACT_VERSION_KEY,
                    FogUiCanonicalContract.CURRENT_CONTRACT_VERSION))
            .build();

    List<CanonicalValidationError> errors =
        validator.validate(
            response,
            CanonicalValidationContext.builder()
                .expectedContractVersion(FogUiCanonicalContract.CURRENT_CONTRACT_VERSION)
                .build());

    assertTrue(errors.isEmpty());
  }
}
