package com.fogui.webstarter.service;

import com.fogui.contract.FogUiCanonicalContract;
import com.fogui.model.fogui.GenerativeUIResponse;
import com.fogui.model.transform.TransformRequest;
import com.fogui.model.transform.TransformResponse;
import com.fogui.service.TransformErrorCodes;
import com.fogui.starter.advisor.FogUiAdvisorContextKeys;
import com.fogui.webstarter.prompt.TransformPromptProvider;
import com.fogui.webstarter.runtime.FogUiTransformRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TransformService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformService.class);

    private static final int TOKEN_ESTIMATE_DIVISOR = 4;
    private static final BigDecimal COST_PER_MILLION_TOKENS_USD = new BigDecimal("0.60");

    private final FogUiTransformRuntime transformRuntime;
    private final TransformPromptProvider transformPromptProvider;

    public TransformService(FogUiTransformRuntime transformRuntime, TransformPromptProvider transformPromptProvider) {
        this.transformRuntime = transformRuntime;
        this.transformPromptProvider = transformPromptProvider;
    }

    public TransformResponse transform(TransformRequest request, String requestId) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new TransformExecutionException(
                    "Content is required",
                    TransformErrorCodes.CONTENT_REQUIRED,
                    null);
        }

        long startTime = System.currentTimeMillis();
        var chatClient = transformRuntime.createClient();
        String contextHints = buildContextHints(request);

        var requestSpec = chatClient.prompt(transformPromptProvider.createPrompt(request.getContent(), contextHints));
        requestSpec.advisors(spec -> spec
                .param(FogUiAdvisorContextKeys.REQUEST_ID, requestId)
                .param(FogUiAdvisorContextKeys.ROUTE_MODE, FogUiAdvisorContextKeys.ROUTE_TRANSFORM));

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
        BigDecimal estimatedCost = new BigDecimal(estimatedTokens)
                .divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP)
                .multiply(COST_PER_MILLION_TOKENS_USD);

        var usage = new TransformResponse.TransformUsage(
            estimatedTokens,
            transformRuntime.getActiveModelName(),
            estimatedCost,
            processingTime);

        LOGGER.info("Transform completed in {}ms, ~{} tokens", processingTime, estimatedTokens);
        return TransformResponse.success(uiResponse, usage, requestId);
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
            hints.append("Preferred UI component families (map these to componentType, not the top-level type): ")
                    .append(String.join(", ", context.getPreferredComponents()))
                    .append(". ");
        }
        if (context.getInstructions() != null) {
            hints.append(context.getInstructions());
        }

        String value = hints.toString().trim();
        return value.isEmpty() ? null : value;
    }
}