package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEnvelopeException;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiErrorResponse;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiRendererToAgentParser;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiIngressResult;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.RequestCorrelationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class A2UiActionController {

    static final String ACTIONS_PATH = "/a2ui/actions";
    static final String IDEMPOTENCY_KEY_HEADER = "X-A2UI-Idempotency-Key";

    private final A2UiActionService actionService;
    private final RequestCorrelationService requestCorrelationService;

    public A2UiActionController(A2UiActionService actionService, RequestCorrelationService requestCorrelationService) {
        this.actionService = actionService;
        this.requestCorrelationService = requestCorrelationService;
    }

    @PostMapping(ACTIONS_PATH)
    public ResponseEntity<?> handleClientEvent(
            @RequestHeader(value = RequestCorrelationService.REQUEST_ID_HEADER, required = false) String requestIdHeader,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestBody JsonNode payload) {
        String requestId = requestCorrelationService.resolveRequestId(requestIdHeader);

        try {
            A2UiIngressResult result = actionService.handle(
                    A2UiRendererToAgentParser.parse(payload), requestId, idempotencyKey);
            return ResponseEntity.status(result.status())
                    .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
                    .body(result.body());
        } catch (A2UiEnvelopeException ex) {
            HttpStatus status = A2UiErrorCode.UNSUPPORTED_VERSION.code().equals(ex.code())
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.BAD_REQUEST;
            return error(status, requestId, ex.getMessage(), ex.code(), null);
        } catch (A2UiActionException ex) {
            HttpStatus status;
            if (A2UiActionErrorCodes.ACTION_NOT_HANDLED.equals(ex.getErrorCode())) {
                status = HttpStatus.UNPROCESSABLE_ENTITY;
            } else if (A2UiActionErrorCodes.CONFIRMATION_REQUIRED.equals(ex.getErrorCode())) {
                status = HttpStatus.CONFLICT;
            } else if (A2UiActionErrorCodes.INVALID_ACTION_RESPONSE.equals(ex.getErrorCode())) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            } else {
                status = HttpStatus.BAD_REQUEST;
            }
            return error(status, requestId, ex.getMessage(), ex.getErrorCode(), ex.getDetails());
        } catch (Exception ex) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, requestId, "A2UI action handling failed", "ACTION_FAILED", null);
        }
    }

    private static ResponseEntity<A2UiErrorResponse> error(
            HttpStatus status, String requestId, String message, String code, Object details) {
        return ResponseEntity.status(status)
                .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
                .body(new A2UiErrorResponse(message, code, details, requestId));
    }
}
