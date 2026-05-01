package com.fogui.webstarter.service;

import com.fogui.contract.CanonicalValidationContext;
import com.fogui.contract.CanonicalValidationError;
import com.fogui.contract.FogUiCanonicalContract;
import com.fogui.contract.FogUiCanonicalValidator;
import com.fogui.contract.a2ui.A2UiInboundTranslator;
import com.fogui.contract.a2ui.A2UiTranslationResult;
import java.util.List;
import java.util.Map;

public class A2UiCompatibilityService {

  private final A2UiInboundTranslator a2UiInboundTranslator;
  private final FogUiCanonicalValidator fogUiCanonicalValidator;

  public A2UiCompatibilityService(
      A2UiInboundTranslator a2UiInboundTranslator,
      FogUiCanonicalValidator fogUiCanonicalValidator) {
    this.a2UiInboundTranslator = a2UiInboundTranslator;
    this.fogUiCanonicalValidator = fogUiCanonicalValidator;
  }

  public Map<String, Object> translateInboundA2Ui(Map<String, Object> payload, String requestId) {
    A2UiTranslationResult translation = a2UiInboundTranslator.translate(payload);
    List<CanonicalValidationError> validationErrors =
        fogUiCanonicalValidator.validate(
            translation.getResponse(),
            CanonicalValidationContext.builder()
                .expectedContractVersion(FogUiCanonicalContract.CURRENT_CONTRACT_VERSION)
                .build());

    boolean success = translation.getErrors().isEmpty() && validationErrors.isEmpty();
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    result.put("success", success);
    result.put("requestId", requestId);
    result.put("result", translation.getResponse());
    result.put("translationErrors", translation.getErrors());
    result.put("validationErrors", validationErrors);
    return result;
  }
}
