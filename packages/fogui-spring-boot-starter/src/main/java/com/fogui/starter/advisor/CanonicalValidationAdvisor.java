package com.fogui.starter.advisor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.contract.CanonicalValidationContext;
import com.fogui.contract.CanonicalValidationError;
import com.fogui.contract.FogUiCanonicalContract;
import com.fogui.contract.FogUiCanonicalValidator;
import com.fogui.model.fogui.GenerativeUIResponse;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Call-path advisor that enforces canonical contract metadata and validation in a
 * deterministic manner.
 */
public class CanonicalValidationAdvisor implements CallAdvisor {

    private static final String DIAGNOSTICS_KEY = "diagnostics";

    private final FogUiCanonicalValidator canonicalValidator;
    private final ObjectMapper objectMapper;
    private final boolean failFast;

    public CanonicalValidationAdvisor(
            FogUiCanonicalValidator canonicalValidator,
            ObjectMapper objectMapper,
            boolean failFast
    ) {
        this.canonicalValidator = canonicalValidator;
        this.objectMapper = objectMapper;
        this.failFast = failFast;
    }

    @Override
    public @NonNull ChatClientResponse adviseCall(
            @NonNull ChatClientRequest request,
            @NonNull CallAdvisorChain chain
    ) {
        ChatClientResponse response = Objects.requireNonNull(chain.nextCall(request));
        return validateAndNormalize(response, request);
    }

    private @NonNull ChatClientResponse validateAndNormalize(
            @NonNull ChatClientResponse response,
            @NonNull ChatClientRequest request
    ) {
        String rawAssistantContent = readAssistantContent(response);
        if (rawAssistantContent == null || rawAssistantContent.isBlank()) {
            if (failFast) {
                throw missingResponseException(request, "Assistant content is empty");
            }
            return response;
        }

        GenerativeUIResponse canonicalResponse;
        try {
            canonicalResponse = objectMapper.readValue(rawAssistantContent, GenerativeUIResponse.class);
        } catch (JsonProcessingException ex) {
            if (failFast) {
                throw parseException(request, ex);
            }
            return response;
        }

        List<CanonicalValidationError> diagnostics = validateCanonicalResponse(canonicalResponse);

        if (!diagnostics.isEmpty() && failFast) {
            throw validationException(request, diagnostics);
        }

        if (!diagnostics.isEmpty()) {
            return response;
        }

        FogUiCanonicalContract.ensureContractVersionMetadata(canonicalResponse);

        try {
            String normalizedJson = objectMapper.writeValueAsString(canonicalResponse);
            return rewriteAssistantContent(response, normalizedJson, request);
        } catch (JsonProcessingException ex) {
            if (failFast) {
                throw parseException(request, ex);
            }
            return response;
        }
    }

    private List<CanonicalValidationError> validateCanonicalResponse(GenerativeUIResponse canonicalResponse) {
        String declaredContractVersion = FogUiCanonicalContract.readContractVersion(canonicalResponse);
        if (declaredContractVersion == null || declaredContractVersion.isBlank()) {
            return canonicalValidator.validate(canonicalResponse, CanonicalValidationContext.empty());
        }

        return canonicalValidator.validate(
                canonicalResponse,
                CanonicalValidationContext.builder()
                        .expectedContractVersion(FogUiCanonicalContract.CURRENT_CONTRACT_VERSION)
                        .build());
    }

        private @NonNull ChatClientResponse rewriteAssistantContent(
            @NonNull ChatClientResponse response,
            String normalizedJson,
            @NonNull ChatClientRequest request
    ) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            if (failFast) {
                throw missingResponseException(request, "Chat response result is missing");
            }
            return response;
        }

        Generation firstGeneration = chatResponse.getResult();
        AssistantMessage firstAssistantMessage = firstGeneration.getOutput();

        AssistantMessage normalizedAssistant = AssistantMessage.builder()
            .content(Objects.requireNonNull(normalizedJson))
                .properties(firstAssistantMessage.getMetadata())
                .toolCalls(firstAssistantMessage.getToolCalls())
                .media(firstAssistantMessage.getMedia())
                .build();

        Generation normalizedGeneration = new Generation(normalizedAssistant, firstGeneration.getMetadata());

        List<Generation> generations = new ArrayList<>();
        if (chatResponse.getResults() != null) {
            generations.addAll(chatResponse.getResults());
        }
        if (generations.isEmpty()) {
            generations.add(normalizedGeneration);
        } else {
            generations.set(0, normalizedGeneration);
        }

        ChatResponse normalizedChatResponse = ChatResponse.builder()
                .from(chatResponse)
                .generations(generations)
                .build();

        return response.mutate().chatResponse(normalizedChatResponse).build();
    }

    private String readAssistantContent(ChatClientResponse response) {
        if (response == null) {
            return null;
        }

        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return null;
        }

        return chatResponse.getResult().getOutput().getText();
    }

    private FogUiAdvisorException missingResponseException(ChatClientRequest request, String reason) {
        Map<String, Object> details = baseDetails(request);
        details.put("reason", reason);
        return new FogUiAdvisorException(
                "Canonical response is missing",
                FogUiAdvisorErrorCodes.CANONICAL_RESPONSE_MISSING,
                details);
    }

    private FogUiAdvisorException parseException(ChatClientRequest request, Exception ex) {
        Map<String, Object> details = baseDetails(request);
        details.put("exceptionType", ex.getClass().getSimpleName());
        details.put("reason", ex.getMessage());
        return new FogUiAdvisorException(
                "Canonical response parsing failed",
                FogUiAdvisorErrorCodes.CANONICAL_PARSE_FAILED,
                details);
    }

    private FogUiAdvisorException validationException(
            ChatClientRequest request,
            List<CanonicalValidationError> diagnostics
    ) {
        Map<String, Object> details = baseDetails(request);
        details.put(DIAGNOSTICS_KEY, diagnostics);
        return new FogUiAdvisorException(
                "Canonical validation failed",
                FogUiAdvisorErrorCodes.CANONICAL_VALIDATION_FAILED,
                details);
    }

    private Map<String, Object> baseDetails(ChatClientRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("expectedContractVersion", FogUiCanonicalContract.CURRENT_CONTRACT_VERSION);
        if (request != null) {
            Object requestId = request.context().get(FogUiAdvisorContextKeys.REQUEST_ID);
            if (requestId != null) {
                details.put("requestId", requestId);
            }

            Object routeMode = request.context().get(FogUiAdvisorContextKeys.ROUTE_MODE);
            if (routeMode != null) {
                details.put("routeMode", routeMode);
            }
        }
        return details;
    }

    @Override
    public int getOrder() {
        return FogUiAdvisorOrder.CANONICAL_VALIDATION;
    }

    @Override
    public @NonNull String getName() {
        return "foguiCanonicalValidationAdvisor";
    }
}

