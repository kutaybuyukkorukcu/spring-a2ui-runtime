package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationException;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiErrorResponse;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceResponse;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRequestCatalogNegotiator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiSurfaceService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.RequestCorrelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
public class A2UiSurfaceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiSurfaceController.class);
    private static final String SURFACE_PATH = "/a2ui/surface";
    private static final int TOKEN_ESTIMATE_DIVISOR = 4;
    private static final BigDecimal COST_PER_MILLION_TOKENS_USD = new BigDecimal("0.60");

    private final A2UiSurfaceService surfaceService;
    private final RequestCorrelationService requestCorrelationService;
    private final A2UiWebProperties webProperties;
    private final A2UiRuntimeMetrics runtimeMetrics;

    public A2UiSurfaceController(A2UiSurfaceService surfaceService,
                                  RequestCorrelationService requestCorrelationService,
                                  A2UiWebProperties webProperties,
                                  A2UiRuntimeMetrics runtimeMetrics) {
        this.surfaceService = surfaceService;
        this.requestCorrelationService = requestCorrelationService;
        this.webProperties = webProperties;
        this.runtimeMetrics = runtimeMetrics;
    }

    @PostMapping(value = SURFACE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> generateSurface(
            @RequestHeader(value = RequestCorrelationService.REQUEST_ID_HEADER, required = false) String requestIdHeader,
            @RequestBody A2UiSurfaceRequest request) {
        String requestId = requestCorrelationService.resolveRequestId(requestIdHeader);

        try {
            String catalogId = A2UiRequestCatalogNegotiator.negotiateCatalogId(request);
            long startTime = System.currentTimeMillis();
            List<A2UiMessage> messages = surfaceService.generate(request, requestId, catalogId);
            long processingTime = System.currentTimeMillis() - startTime;

            int estimatedTokens = estimateTokens(request.content());
            A2UiSurfaceResponse.TransformUsage usage = new A2UiSurfaceResponse.TransformUsage(
                    estimatedTokens, surfaceService.getActiveModelName(),
                    estimateCost(estimatedTokens), processingTime);

            runtimeMetrics.recordTransformSuccess("sync");
            return withRequestIdHeaders(ResponseEntity.ok(), requestId)
                    .body(A2UiSurfaceResponse.success(messages, usage, requestId));
        } catch (SurfaceExecutionException ex) {
            runtimeMetrics.recordTransformFailure("sync", ex.getErrorCode());
            HttpStatus status = mapErrorCode(ex.getErrorCode());
            return withRequestIdHeaders(ResponseEntity.status(status), requestId)
                    .body(A2UiSurfaceResponse.failure(ex.getMessage(), ex.getErrorCode(), requestId, ex.getDetails()));
        } catch (A2UiValidationException ex) {
            runtimeMetrics.recordTransformFailure("sync", SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
            return withRequestIdHeaders(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR), requestId)
                    .body(A2UiSurfaceResponse.failure(ex.getMessage(), SurfaceErrorCodes.A2UI_VALIDATION_FAILED, requestId, null));
        } catch (Exception ex) {
            runtimeMetrics.recordTransformFailure("sync", SurfaceErrorCodes.TRANSFORM_FAILED);
            LOGGER.error("Surface generation error", ex);
            return withRequestIdHeaders(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR), requestId)
                    .body(A2UiSurfaceResponse.failure("Transformation failed: " + ex.getMessage(), SurfaceErrorCodes.TRANSFORM_FAILED, requestId, null));
        }
    }

    private int estimateTokens(String content) {
        return content == null ? 0 : content.length() / TOKEN_ESTIMATE_DIVISOR;
    }

    private BigDecimal estimateCost(int tokens) {
        return new BigDecimal(tokens).divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP).multiply(COST_PER_MILLION_TOKENS_USD);
    }

    private HttpStatus mapErrorCode(String errorCode) {
        if (SurfaceErrorCodes.CONTENT_REQUIRED.equals(errorCode)) return HttpStatus.BAD_REQUEST;
        if (SurfaceErrorCodes.NO_COMPATIBLE_CATALOG.equals(errorCode)) return HttpStatus.UNPROCESSABLE_ENTITY;
        if (SurfaceErrorCodes.TRANSFORM_PARSE_FAILED.equals(errorCode)) return HttpStatus.UNPROCESSABLE_ENTITY;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static ResponseEntity.BodyBuilder withRequestIdHeaders(ResponseEntity.BodyBuilder response, String requestId) {
        return response.header(RequestCorrelationService.REQUEST_ID_HEADER, requestId);
    }
}