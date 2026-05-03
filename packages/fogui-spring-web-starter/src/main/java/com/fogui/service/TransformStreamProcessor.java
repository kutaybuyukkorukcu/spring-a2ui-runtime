package com.fogui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.contract.CanonicalValidationContext;
import com.fogui.contract.CanonicalValidationError;
import com.fogui.contract.FogUiCanonicalContract;
import com.fogui.contract.FogUiCanonicalValidator;
import com.fogui.contract.a2ui.A2UiErrorResponse;
import com.fogui.contract.a2ui.A2UiMessage;
import com.fogui.contract.a2ui.A2UiMessageValidationException;
import com.fogui.contract.a2ui.A2UiOutboundMapper;
import com.fogui.model.fogui.GenerativeUIResponse;
import com.fogui.model.transform.TransformRequest;
import com.fogui.starter.advisor.FogUiAdvisorContextKeys;
import com.fogui.starter.advisor.FogUiAdvisorErrorCodes;
import com.fogui.starter.advisor.FogUiAdvisorException;
import com.fogui.webstarter.prompt.TransformPromptContext;
import com.fogui.webstarter.prompt.TransformPromptProvider;
import com.fogui.webstarter.service.A2UiRequestCatalogNegotiator;
import com.fogui.webstarter.runtime.FogUiTransformRuntime;
import com.fogui.webstarter.service.TransformExecutionException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

public class TransformStreamProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformStreamProcessor.class);

  private static final String MESSAGE_EVENT = "message";
  private static final String ERROR_EVENT = "error";
  private static final String REQUEST_ID_KEY = "requestId";
  private static final String EXCEPTION_TYPE_KEY = "exceptionType";

  private final FogUiTransformRuntime transformRuntime;
  private final TransformPromptProvider transformPromptProvider;
  private final UIResponseParser responseParser;
  private final StreamPatchReconciler streamPatchReconciler;
  private final FogUiCanonicalValidator canonicalValidator;
  private final ObjectMapper objectMapper;
  private final A2UiOutboundMapper a2UiOutboundMapper;

  public TransformStreamProcessor(
      FogUiTransformRuntime transformRuntime,
      TransformPromptProvider transformPromptProvider,
      UIResponseParser responseParser,
      StreamPatchReconciler streamPatchReconciler,
      FogUiCanonicalValidator canonicalValidator,
      ObjectMapper objectMapper) {
    this(
        transformRuntime,
        transformPromptProvider,
        responseParser,
        streamPatchReconciler,
        canonicalValidator,
        objectMapper,
        new A2UiOutboundMapper());
  }

  TransformStreamProcessor(
      FogUiTransformRuntime transformRuntime,
      TransformPromptProvider transformPromptProvider,
      UIResponseParser responseParser,
      StreamPatchReconciler streamPatchReconciler,
      FogUiCanonicalValidator canonicalValidator,
      ObjectMapper objectMapper,
      A2UiOutboundMapper a2UiOutboundMapper) {
    this.transformRuntime = transformRuntime;
    this.transformPromptProvider = transformPromptProvider;
    this.responseParser = responseParser;
    this.streamPatchReconciler = streamPatchReconciler;
    this.canonicalValidator = canonicalValidator;
    this.objectMapper = objectMapper;
    this.a2UiOutboundMapper = a2UiOutboundMapper;
  }

  public void processStreamRequest(TransformRequest request, SseEmitter emitter, String requestId) {
    String catalogId = validateRequest(request, emitter, requestId);
    if (catalogId == null) {
      return;
    }

    try {
      var chatClient = transformRuntime.createClient();
      var prompt = transformPromptProvider.createPrompt(buildPromptContext(request, catalogId));
      var fullContent = new StringBuilder();
      var previousResponse = new AtomicReference<GenerativeUIResponse>(null);
      var renderStarted = new AtomicBoolean(false);
      String advisorRequestId = requestId == null ? "" : requestId;

      var requestSpec = chatClient.prompt(Objects.requireNonNull(prompt));
      requestSpec.advisors(
          spec ->
              spec.param(FogUiAdvisorContextKeys.REQUEST_ID, advisorRequestId)
                  .param(
                      FogUiAdvisorContextKeys.ROUTE_MODE,
                      FogUiAdvisorContextKeys.ROUTE_TRANSFORM_STREAM));
      requestSpec.stream()
          .content()
          .doOnNext(
              chunk -> {
                fullContent.append(chunk);
                emitPartialResult(fullContent, emitter, previousResponse, renderStarted, catalogId);
              })
          .doOnComplete(
              () -> handleStreamComplete(emitter, fullContent, requestId, renderStarted, catalogId))
          .doOnError(error -> handleStreamError(error, emitter, requestId))
          .onErrorResume(error -> Flux.empty())
          .subscribe();

    } catch (Exception ex) {
      handleProcessError(ex, emitter, requestId);
    }
  }

  private String validateRequest(TransformRequest request, SseEmitter emitter, String requestId) {
    if (request == null || request.getContent() == null || request.getContent().isBlank()) {
      sendErrorAndComplete(
          emitter, "Content is required", TransformErrorCodes.CONTENT_REQUIRED, requestId, null);
      return null;
    }
    try {
      return A2UiRequestCatalogNegotiator.negotiateCatalogId(request);
    } catch (TransformExecutionException ex) {
      sendErrorAndComplete(emitter, ex.getMessage(), ex.getErrorCode(), requestId, ex.getDetails());
      return null;
    }
  }

  private void sendErrorAndComplete(
      SseEmitter emitter, String errorMessage, String errorCode, String requestId, Object details) {
    try {
      sendStreamErrorEvent(emitter, errorMessage, errorCode, requestId, details);
      emitter.complete();
    } catch (IOException ex) {
      emitter.completeWithError(ex);
    }
  }

  private void sendStreamErrorEvent(
      SseEmitter emitter, String errorMessage, String errorCode, String requestId, Object details)
      throws IOException {
    A2UiErrorResponse errorBody =
        a2UiOutboundMapper.toErrorResponse(
            errorMessage, errorCode, details, requestId == null ? "" : requestId);
    emitter.send(SseEmitter.event().name(ERROR_EVENT).data(writeJson(errorBody)));
  }

  private String extractContextHints(TransformRequest request) {
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

  private TransformPromptContext buildPromptContext(TransformRequest request, String catalogId) {
    return new TransformPromptContext(
        request.getContent(),
        extractContextHints(request),
        catalogId,
        extractSupportedCatalogIds(request));
  }

  private List<String> extractSupportedCatalogIds(TransformRequest request) {
    if (request.getA2UiClientCapabilities() == null) {
      return List.of();
    }
    List<String> supportedCatalogIds = request.getA2UiClientCapabilities().getSupportedCatalogIds();
    return supportedCatalogIds == null ? List.of() : supportedCatalogIds;
  }

  private void emitPartialResult(
      StringBuilder fullContent,
      SseEmitter emitter,
      AtomicReference<GenerativeUIResponse> previousResponse,
      AtomicBoolean renderStarted,
      String catalogId) {
    var partial = responseParser.tryParsePartial(fullContent.toString());
    var reconciled = streamPatchReconciler.reconcile(previousResponse.get(), partial);
    var normalized = normalizeCanonicalResponse(reconciled, false, null);
    if (normalized == null || Objects.equals(normalized, previousResponse.get())) {
      return;
    }

    previousResponse.set(normalized);

    try {
      emitA2UiMessages(
          emitter,
          a2UiOutboundMapper.toMessages(
              normalized,
              A2UiOutboundMapper.DEFAULT_SURFACE_ID,
              !renderStarted.get(),
              catalogId));
      renderStarted.set(true);
    } catch (IOException ex) {
      LOGGER.error("Error sending partial result", ex);
    }
  }

  private void handleStreamComplete(
      SseEmitter emitter,
      StringBuilder fullContent,
      String requestId,
      AtomicBoolean renderStarted,
      String catalogId) {
    try {
      sendStreamResult(emitter, fullContent.toString(), requestId, renderStarted, catalogId);
      emitter.complete();
    } catch (Exception ex) {
      handleStreamError(ex, emitter, requestId);
    }
  }

  private void sendStreamResult(
      SseEmitter emitter,
      String content,
      String requestId,
      AtomicBoolean renderStarted,
      String catalogId)
      throws IOException {
    emitA2UiMessages(
        emitter,
        a2UiOutboundMapper.toMessages(
            parseAndNormalizeFinalResponse(content, requestId),
            A2UiOutboundMapper.DEFAULT_SURFACE_ID,
            !renderStarted.get(),
            catalogId));
  }

  private GenerativeUIResponse parseAndNormalizeFinalResponse(String content, String requestId) {
    if (content == null || content.isBlank()) {
      throw missingResponseException(requestId, "Assistant content is empty");
    }

    try {
      GenerativeUIResponse response = objectMapper.readValue(content, GenerativeUIResponse.class);
      return normalizeCanonicalResponse(response, true, requestId);
    } catch (FogUiAdvisorException ex) {
      throw ex;
    } catch (Exception ex) {
      throw parseException(requestId, ex);
    }
  }

  private GenerativeUIResponse normalizeCanonicalResponse(
      GenerativeUIResponse response, boolean failOnInvalid, String requestId) {
    if (response == null) {
      return null;
    }

    List<CanonicalValidationError> diagnostics = validateCanonicalResponse(response);
    if (!diagnostics.isEmpty()) {
      if (failOnInvalid) {
        throw validationException(requestId, diagnostics);
      }
      return null;
    }

    return FogUiCanonicalContract.ensureContractVersionMetadata(response);
  }

  private List<CanonicalValidationError> validateCanonicalResponse(GenerativeUIResponse response) {
    String declaredContractVersion = FogUiCanonicalContract.readContractVersion(response);
    if (declaredContractVersion == null || declaredContractVersion.isBlank()) {
      return canonicalValidator.validate(response, CanonicalValidationContext.empty());
    }

    return canonicalValidator.validate(
        response,
        CanonicalValidationContext.builder()
            .expectedContractVersion(FogUiCanonicalContract.CURRENT_CONTRACT_VERSION)
            .build());
  }

  private FogUiAdvisorException missingResponseException(String requestId, String reason) {
    Map<String, Object> details = baseDetails(requestId);
    details.put("reason", reason);
    return new FogUiAdvisorException(
        "Canonical response is missing",
        FogUiAdvisorErrorCodes.CANONICAL_RESPONSE_MISSING,
        details);
  }

  private FogUiAdvisorException parseException(String requestId, Exception ex) {
    Map<String, Object> details = baseDetails(requestId);
    details.put(EXCEPTION_TYPE_KEY, ex.getClass().getSimpleName());
    details.put("reason", ex.getMessage());
    return new FogUiAdvisorException(
        "Canonical response parsing failed",
        FogUiAdvisorErrorCodes.CANONICAL_PARSE_FAILED,
        details);
  }

  private FogUiAdvisorException validationException(
      String requestId, List<CanonicalValidationError> diagnostics) {
    Map<String, Object> details = baseDetails(requestId);
    details.put("diagnostics", diagnostics);
    return new FogUiAdvisorException(
        "Canonical validation failed", FogUiAdvisorErrorCodes.CANONICAL_VALIDATION_FAILED, details);
  }

  private Map<String, Object> baseDetails(String requestId) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("expectedContractVersion", FogUiCanonicalContract.CURRENT_CONTRACT_VERSION);
    details.put("routeMode", FogUiAdvisorContextKeys.ROUTE_TRANSFORM_STREAM);
    if (requestId != null) {
      details.put(REQUEST_ID_KEY, requestId);
    }
    return details;
  }

  private void emitA2UiMessage(SseEmitter emitter, Object message) throws IOException {
    emitter.send(SseEmitter.event().name(MESSAGE_EVENT).data(writeJson(message)));
  }

  private void emitA2UiMessages(SseEmitter emitter, List<A2UiMessage> messages) throws IOException {
    for (A2UiMessage message : messages) {
      emitA2UiMessage(emitter, message);
    }
  }

  private @NonNull String writeJson(Object value) throws IOException {
    return Objects.requireNonNull(objectMapper.writeValueAsString(value));
  }

  private void handleStreamError(Throwable error, SseEmitter emitter, String requestId) {
    LOGGER.error("Stream error", error);
    try {
      sendStreamErrorEvent(
          emitter,
          error.getMessage() != null ? error.getMessage() : "Stream processing failed",
          resolveErrorCode(error),
          requestId,
          resolveErrorDetails(error));
      emitter.complete();
    } catch (IOException ioException) {
      emitter.completeWithError(ioException);
    }
  }

  private void handleProcessError(Exception ex, SseEmitter emitter, String requestId) {
    LOGGER.error("Transform stream error", ex);
    sendErrorAndComplete(emitter, ex.getMessage(), resolveErrorCode(ex), requestId, resolveErrorDetails(ex));
  }

  private String resolveErrorCode(Throwable error) {
    if (error instanceof TransformExecutionException executionException) {
      return executionException.getErrorCode();
    }
    if (error instanceof FogUiAdvisorException advisorException) {
      return advisorException.getErrorCode();
    }
    if (error instanceof A2UiMessageValidationException) {
      return TransformErrorCodes.A2UI_VALIDATION_FAILED;
    }
    return TransformErrorCodes.STREAM_FAILED;
  }

  private Object resolveErrorDetails(Throwable error) {
    if (error instanceof TransformExecutionException executionException) {
      return executionException.getDetails();
    }
    if (error instanceof FogUiAdvisorException advisorException) {
      return advisorException.getDetails();
    }
    if (error instanceof A2UiMessageValidationException validationException) {
      return Map.of("diagnostics", validationException.getDiagnostics());
    }
    return Map.of(EXCEPTION_TYPE_KEY, error.getClass().getSimpleName());
  }
}
