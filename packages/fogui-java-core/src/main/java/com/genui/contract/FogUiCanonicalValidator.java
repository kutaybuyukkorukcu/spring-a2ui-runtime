package com.genui.contract;

import com.genui.model.genui.ContentBlock;
import com.genui.model.genui.GenerativeUIResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic validator for canonical FogUI payloads.
 */
public class FogUiCanonicalValidator {

    public List<CanonicalValidationError> validate(GenerativeUIResponse response) {
        return validate(response, CanonicalValidationContext.empty());
    }

    public List<CanonicalValidationError> validate(
            GenerativeUIResponse response,
            CanonicalValidationContext context
    ) {
        List<CanonicalValidationError> errors = new ArrayList<>();
        if (response == null) {
            errors.add(error("$", FogUiErrorCode.NULL_RESPONSE, "Response must not be null"));
            return errors;
        }

        validateContractVersion(response, context, errors);

        if (response.getThinking() == null) {
            errors.add(error("$.thinking", FogUiErrorCode.MISSING_THINKING, "thinking must be an array"));
        }

        if (response.getContent() == null) {
            errors.add(error("$.content", FogUiErrorCode.MISSING_CONTENT, "content must be an array"));
            return errors;
        }

        for (int i = 0; i < response.getContent().size(); i++) {
            validateBlock(response.getContent().get(i), "$.content[" + i + "]", errors);
        }

        return errors;
    }

    public boolean isValid(GenerativeUIResponse response) {
        return validate(response).isEmpty();
    }

    private void validateBlock(ContentBlock block, String path, List<CanonicalValidationError> errors) {
        if (block == null) {
            errors.add(error(path, FogUiErrorCode.NULL_BLOCK, "content block must not be null"));
            return;
        }

        if (block.getType() == null || block.getType().isBlank()) {
            errors.add(error(path + ".type", FogUiErrorCode.MISSING_TYPE, "type is required"));
            return;
        }

        if ("text".equals(block.getType())) {
            if (!(block.getValue() instanceof String)) {
                errors.add(error(
                        path + ".value",
                        FogUiErrorCode.INVALID_TEXT_VALUE,
                        "text block value must be a string"));
            }
            return;
        }

        if ("component".equals(block.getType())) {
            if (block.getComponentType() == null || block.getComponentType().isBlank()) {
                errors.add(error(
                        path + ".componentType",
                        FogUiErrorCode.MISSING_COMPONENT_TYPE,
                        "componentType is required"));
            }

            if (block.getProps() != null && !(block.getProps() instanceof java.util.Map)) {
                errors.add(error(
                        path + ".props",
                        FogUiErrorCode.INVALID_PROPS,
                        "props must be an object when provided"));
            }

            if (block.getChildren() != null) {
                for (int i = 0; i < block.getChildren().size(); i++) {
                    validateBlock(block.getChildren().get(i), path + ".children[" + i + "]", errors);
                }
            }
            return;
        }

        errors.add(error(
                path + ".type",
                FogUiErrorCode.UNSUPPORTED_TYPE,
                "type must be 'text' or 'component'"));
    }

    private void validateContractVersion(
            GenerativeUIResponse response,
            CanonicalValidationContext context,
            List<CanonicalValidationError> errors
    ) {
        if (context == null
                || context.getExpectedContractVersion() == null
                || context.getExpectedContractVersion().isBlank()) {
            return;
        }

        String actualVersion = FogUiCanonicalContract.readContractVersion(response);
        if (actualVersion == null || actualVersion.isBlank()) {
            errors.add(error(
                    "$.metadata.contractVersion",
                    FogUiErrorCode.MISSING_CONTRACT_VERSION,
                    "metadata.contractVersion is required when expected contract version is set"));
            return;
        }

        if (!context.getExpectedContractVersion().equals(actualVersion)) {
            Map<String, Object> details = new HashMap<>();
            details.put("expectedContractVersion", context.getExpectedContractVersion());
            details.put("actualContractVersion", actualVersion);
            errors.add(error(
                    "$.metadata.contractVersion",
                    FogUiErrorCode.CONTRACT_VERSION_MISMATCH,
                    "metadata.contractVersion does not match expected contract version",
                    details));
        }
    }

    private CanonicalValidationError error(String path, FogUiErrorCode code, String message) {
        return error(path, code, message, null);
    }

    private CanonicalValidationError error(
            String path,
            FogUiErrorCode code,
            String message,
            Map<String, Object> details
    ) {
        return CanonicalValidationError.builder()
                .path(path)
                .code(code.code())
                .category(code.category().name())
                .message(message)
                .details(details)
                .build();
    }
}
