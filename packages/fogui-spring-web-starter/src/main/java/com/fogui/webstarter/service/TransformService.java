package com.fogui.webstarter.service;

import com.fogui.contract.FogUiCanonicalContract;
import com.fogui.model.fogui.GenerativeUIResponse;
import com.fogui.model.transform.TransformRequest;
import com.fogui.model.transform.TransformResponse;
import com.fogui.service.TransformErrorCodes;
import com.fogui.starter.advisor.FogUiAdvisorContextKeys;
import com.fogui.webstarter.prompt.TransformPromptContext;
import com.fogui.webstarter.prompt.TransformPromptProvider;
import com.fogui.webstarter.runtime.FogUiTransformRuntime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformService.class);

  private static final int TOKEN_ESTIMATE_DIVISOR = 4;
  private static final BigDecimal COST_PER_MILLION_TOKENS_USD = new BigDecimal("0.60");

  private final FogUiTransformRuntime transformRuntime;
  private final TransformPromptProvider transformPromptProvider;

  public TransformService(
      FogUiTransformRuntime transformRuntime, TransformPromptProvider transformPromptProvider) {
    this.transformRuntime = transformRuntime;
    this.transformPromptProvider = transformPromptProvider;
  }

  public TransformResponse transform(TransformRequest request, String requestId) {
    ensureContentPresent(request);
    return transform(request, requestId, A2UiRequestCatalogNegotiator.negotiateCatalogId(request));
    }

    public TransformResponse transform(TransformRequest request, String requestId, String catalogId) {
    ensureContentPresent(request);

    long startTime = System.currentTimeMillis();
    var chatClient = transformRuntime.createClient();
    String contextHints = buildContextHints(request);
    TransformPromptContext promptContext =
      new TransformPromptContext(
        request.getContent(), contextHints, catalogId, extractSupportedCatalogIds(request));

    var requestSpec = chatClient.prompt(transformPromptProvider.createPrompt(promptContext));
    requestSpec.advisors(
      spec ->
        spec.param(FogUiAdvisorContextKeys.REQUEST_ID, requestId)
          .param(
            FogUiAdvisorContextKeys.ROUTE_MODE, FogUiAdvisorContextKeys.ROUTE_TRANSFORM));

    GenerativeUIResponse uiResponse = requestSpec.call().entity(GenerativeUIResponse.class);
    if (uiResponse == null) {
      throw new TransformExecutionException(
        "Failed to parse transformation result",
        TransformErrorCodes.TRANSFORM_PARSE_FAILED,
        null);
    }

    FogUiCanonicalContract.ensureContractVersionMetadata(uiResponse);

    long processingTime = System.currentTimeMillis() - startTime;
    int estimatedTokens = estimateTokens(request.getContent());
    BigDecimal estimatedCost =
      new BigDecimal(estimatedTokens)
        .divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP)
        .multiply(COST_PER_MILLION_TOKENS_USD);

    var usage =
      new TransformResponse.TransformUsage(
        estimatedTokens, transformRuntime.getActiveModelName(), estimatedCost, processingTime);

    LOGGER.info("Transform completed in {}ms, ~{} tokens", processingTime, estimatedTokens);
    return TransformResponse.success(uiResponse, usage, requestId);
    }

    private void ensureContentPresent(TransformRequest request) {
    if (request == null || request.getContent() == null || request.getContent().isBlank()) {
      throw new TransformExecutionException(
          "Content is required", TransformErrorCodes.CONTENT_REQUIRED, null);
    }
  }

  private int estimateTokens(String content) {
    return content.length() / TOKEN_ESTIMATE_DIVISOR;
  }

  private String buildContextHints(TransformRequest request) {
    if (request.getContext() == null) {
      return null;
    }

    var context = request.getContext();
    StringBuilder hints = new StringBuilder();

    if (context.getIntent() != null) {
      hints.append("Intent: ").append(context.getIntent()).append(". ");
    }
    if (context.getPreferredComponents() != null && !context.getPreferredComponents().isEmpty()) {
      hints
          .append(
              "Preferred UI component families (map these to componentType, not the top-level type): ")
          .append(String.join(", ", context.getPreferredComponents()))
          .append(". ");
    }
    if (context.getInstructions() != null) {
      hints.append(context.getInstructions());
    }

    String value = hints.toString().trim();
    return value.isEmpty() ? null : value;
  }

  private List<String> extractSupportedCatalogIds(TransformRequest request) {
    if (request.getA2UiClientCapabilities() == null) {
      return List.of();
    }
    List<String> supportedCatalogIds = request.getA2UiClientCapabilities().getSupportedCatalogIds();
    return supportedCatalogIds == null ? List.of() : supportedCatalogIds;
  }
}
