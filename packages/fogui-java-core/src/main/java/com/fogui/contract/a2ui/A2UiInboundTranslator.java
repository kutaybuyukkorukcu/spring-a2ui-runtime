package com.fogui.contract.a2ui;

import com.fogui.contract.FogUiCanonicalContract;
import com.fogui.contract.FogUiErrorCode;
import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import com.fogui.model.fogui.ThinkingItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inbound translator from A2UI-like payloads into FogUI canonical contract.
 * v1 scope intentionally supports a conservative subset and emits explicit
 * fallback blocks for unknown nodes.
 */
public class A2UiInboundTranslator {

    public static final String SUPPORTED_A2UI_VERSION = A2UiProtocol.SUPPORTED_VERSION;
    private static final String FALLBACK_COMPONENT_TYPE = "A2UiUnsupportedNode";

    public A2UiTranslationResult translate(Map<String, Object> a2uiPayload) {
        List<A2UiTranslationError> errors = new ArrayList<>();
        GenerativeUIResponse response = GenerativeUIResponse.builder().build();

        if (a2uiPayload == null) {
            errors.add(error("$", FogUiErrorCode.NULL_PAYLOAD, "A2UI payload must not be null"));
            return A2UiTranslationResult.builder().response(response).errors(errors).build();
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sourceProtocol", "a2ui");
        metadata.put("supportedVersion", SUPPORTED_A2UI_VERSION);
        metadata.put(FogUiCanonicalContract.METADATA_CONTRACT_VERSION_KEY, FogUiCanonicalContract.CURRENT_CONTRACT_VERSION);
        response.setMetadata(metadata);

        response.setThinking(translateThinking(a2uiPayload.get("thinking"), errors));
        response.setContent(translateContent(a2uiPayload.get("content"), errors, "$.content"));

        return A2UiTranslationResult.builder()
                .response(FogUiCanonicalContract.ensureContractVersionMetadata(response))
                .errors(errors)
                .build();
    }

    private List<ThinkingItem> translateThinking(Object rawThinking, List<A2UiTranslationError> errors) {
        List<ThinkingItem> items = new ArrayList<>();
        if (rawThinking == null) {
            return items;
        }

        if (!(rawThinking instanceof List<?> list)) {
            errors.add(error("$.thinking", FogUiErrorCode.INVALID_THINKING, "thinking must be an array"));
            return items;
        }

        for (int i = 0; i < list.size(); i++) {
            ThinkingItem item = translateThinkingItem(list.get(i), i, errors);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    private ThinkingItem translateThinkingItem(
            Object raw,
            int index,
            List<A2UiTranslationError> errors
    ) {
        if (!(raw instanceof Map<?, ?> map)) {
            errors.add(error(
                    "$.thinking[" + index + "]",
                    FogUiErrorCode.INVALID_THINKING_ITEM,
                    "thinking item must be an object"));
            return null;
        }

        String message = stringify(map.get("message"));
        Object rawStatus = map.containsKey("status") ? map.get("status") : "complete";
        String status = stringify(rawStatus);
        String timestamp = stringify(map.get("timestamp"));

        ThinkingItem item = ThinkingItem.builder()
                .message(message == null ? "" : message)
                .status(status == null || status.isBlank() ? "complete" : status)
                .build();

        if (timestamp != null && !timestamp.isBlank()) {
            item.setTimestamp(timestamp);
        } else {
            // Avoid default Instant.now() so translation stays deterministic for the same payload.
            item.setTimestamp(null);
        }

        return item;
    }

    private List<ContentBlock> translateContent(Object rawContent, List<A2UiTranslationError> errors, String path) {
        List<ContentBlock> blocks = new ArrayList<>();
        if (rawContent == null) {
            return blocks;
        }

        if (!(rawContent instanceof List<?> list)) {
            errors.add(error(path, FogUiErrorCode.INVALID_CONTENT, "content must be an array"));
            return blocks;
        }

        for (int i = 0; i < list.size(); i++) {
            blocks.add(translateBlock(list.get(i), path + "[" + i + "]", errors));
        }
        return blocks;
    }

    private ContentBlock translateBlock(Object rawBlock, String path, List<A2UiTranslationError> errors) {
        if (!(rawBlock instanceof Map<?, ?> map)) {
            errors.add(error(path, FogUiErrorCode.INVALID_BLOCK, "content block must be an object"));
            return fallbackBlock(rawBlock, "invalid_block");
        }

        String type = stringify(map.get("type"));
        if ("text".equalsIgnoreCase(type)) {
            String value = stringify(map.get("value"));
            if (value == null) {
                value = stringify(map.get("text"));
            }
            if (value == null) {
                errors.add(error(path + ".value", FogUiErrorCode.MISSING_TEXT, "text block requires value/text"));
                value = "";
            }
            return ContentBlock.text(value);
        }

        String componentType = extractComponentType(map);
        if (componentType != null) {
            Object rawChildren = map.get("children");
            List<ContentBlock> children = rawChildren == null
                    ? null
                    : translateContent(rawChildren, errors, path + ".children");

            Object rawProps = map.get("props");
            Map<String, Object> props = rawProps instanceof Map<?, ?> typed
                    ? castMap(typed)
                    : Map.of();

            ContentBlock block = ContentBlock.component(componentType, props);
            block.setChildren(children);
            return block;
        }

        errors.add(error(path, FogUiErrorCode.UNSUPPORTED_NODE, "unsupported A2UI node, fallback emitted"));
        return fallbackBlock(castMap(map), "unsupported_node");
    }

    private ContentBlock fallbackBlock(Object rawNode, String reason) {
        return ContentBlock.component(
                FALLBACK_COMPONENT_TYPE,
                Map.of(
                        "reason", reason,
                        "raw", rawNode
                )
        );
    }

    private String extractComponentType(Map<?, ?> map) {
        String componentType = stringify(map.get("componentType"));
        if (componentType != null && !componentType.isBlank()) {
            return componentType;
        }

        componentType = stringify(map.get("name"));
        if (componentType != null && !componentType.isBlank()) {
            return componentType;
        }

        String type = stringify(map.get("type"));
        if ("component".equalsIgnoreCase(type)) {
            return "unknown";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private A2UiTranslationError error(String path, FogUiErrorCode code, String message) {
        return A2UiTranslationError.builder()
                .path(path)
                .code(code.code())
                .category(code.category().name())
                .message(message)
                .build();
    }
}
