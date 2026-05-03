package com.fogui.contract.a2ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic validator for outbound A2UI v0.8 message batches.
 */
public class A2UiMessageValidator {

    private static final String SURFACE_ID_SUFFIX = ".surfaceId";
    private static final String SURFACE_ID_REQUIRED = "surfaceId is required";

    private final A2UiCatalogRegistry catalogRegistry;

    public A2UiMessageValidator() {
        this(A2UiCatalogRegistry.shared());
    }

    A2UiMessageValidator(A2UiCatalogRegistry catalogRegistry) {
        this.catalogRegistry = catalogRegistry;
    }

    public List<A2UiValidationError> validate(List<A2UiMessage> messages) {
        return validate(messages, A2UiValidationContext.empty());
    }

    public List<A2UiValidationError> validate(
            List<A2UiMessage> messages,
            A2UiValidationContext context
    ) {
        List<A2UiValidationError> errors = new ArrayList<>();

        validateVersion(context, errors);

        if (messages == null) {
            errors.add(error("$", A2UiValidationErrorCode.NULL_MESSAGE_BATCH, "message batch must not be null"));
            return errors;
        }

        for (int index = 0; index < messages.size(); index++) {
            validateMessage(messages.get(index), "$[" + index + "]", errors);
        }

        validateMessageSequence(messages, errors);

        return errors;
    }

    public boolean isValid(List<A2UiMessage> messages) {
        return validate(messages).isEmpty();
    }

    private void validateVersion(A2UiValidationContext context, List<A2UiValidationError> errors) {
        if (context == null || context.getRequestedVersion() == null || context.getRequestedVersion().isBlank()) {
            return;
        }

        if (!A2UiProtocol.SUPPORTED_VERSION.equals(context.getRequestedVersion())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("requestedVersion", context.getRequestedVersion());
            details.put("supportedVersion", A2UiProtocol.SUPPORTED_VERSION);
            errors.add(error(
                    "$",
                    A2UiValidationErrorCode.UNSUPPORTED_VERSION,
                    "requested A2UI version is not supported",
                    details));
        }
    }

    private void validateMessage(
            A2UiMessage message,
            String path,
            List<A2UiValidationError> errors
    ) {
        if (message == null) {
            errors.add(error(path, A2UiValidationErrorCode.NULL_MESSAGE, "message must not be null"));
            return;
        }

        int fieldCount = 0;
        if (message.getSurfaceUpdate() != null) {
            fieldCount++;
        }
        if (message.getDataModelUpdate() != null) {
            fieldCount++;
        }
        if (message.getBeginRendering() != null) {
            fieldCount++;
        }
        if (message.getDeleteSurface() != null) {
            fieldCount++;
        }

        if (fieldCount != 1) {
            errors.add(error(
                    path,
                    A2UiValidationErrorCode.INVALID_MESSAGE_ENVELOPE,
                    "message must contain exactly one A2UI event payload"));
            return;
        }

        if (message.getSurfaceUpdate() != null) {
            validateSurfaceUpdate(path + ".surfaceUpdate", message.getSurfaceUpdate(), errors);
        }
        if (message.getDataModelUpdate() != null) {
            validateDataModelUpdate(path + ".dataModelUpdate", message.getDataModelUpdate(), errors);
        }
        if (message.getBeginRendering() != null) {
            validateBeginRendering(path + ".beginRendering", message.getBeginRendering(), errors);
        }
        if (message.getDeleteSurface() != null) {
            validateDeleteSurface(path + ".deleteSurface", message.getDeleteSurface(), errors);
        }
    }

    private void validateMessageSequence(List<A2UiMessage> messages, List<A2UiValidationError> errors) {
        Map<String, SurfaceState> surfaces = new LinkedHashMap<>();

        for (int index = 0; index < messages.size(); index++) {
            A2UiMessage message = messages.get(index);
            if (message != null && hasExactlyOnePayload(message) && message.getSurfaceUpdate() != null) {
                registerSurfaceUpdate(message.getSurfaceUpdate(), surfaces);
            } else if (message != null && hasExactlyOnePayload(message) && message.getBeginRendering() != null) {
                validateBeginRenderingSequence(
                        message.getBeginRendering(),
                        "$[" + index + "].beginRendering",
                        surfaces,
                        errors);
            } else if (message != null
                    && hasExactlyOnePayload(message)
                    && message.getDeleteSurface() != null
                    && !isBlank(message.getDeleteSurface().getSurfaceId())) {
                surfaces.remove(message.getDeleteSurface().getSurfaceId());
            }
        }
    }

    private void registerSurfaceUpdate(
            A2UiMessage.SurfaceUpdate surfaceUpdate,
            Map<String, SurfaceState> surfaces
    ) {
        if (surfaceUpdate == null || isBlank(surfaceUpdate.getSurfaceId()) || surfaceUpdate.getComponents() == null) {
            return;
        }

        SurfaceState surfaceState = surfaces.computeIfAbsent(surfaceUpdate.getSurfaceId(), key -> new SurfaceState());
        for (A2UiMessage.ComponentDefinition component : surfaceUpdate.getComponents()) {
            if (component != null && !isBlank(component.getId())) {
                surfaceState.componentIds().add(component.getId());
            }
        }
    }

    private void validateBeginRenderingSequence(
            A2UiMessage.BeginRendering beginRendering,
            String path,
            Map<String, SurfaceState> surfaces,
            List<A2UiValidationError> errors
    ) {
        if (beginRendering == null || isBlank(beginRendering.getSurfaceId()) || isBlank(beginRendering.getRoot())) {
            return;
        }

        SurfaceState surfaceState = surfaces.get(beginRendering.getSurfaceId());
        if (surfaceState == null || surfaceState.componentIds().isEmpty()) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("surfaceId", beginRendering.getSurfaceId());
            details.put("root", beginRendering.getRoot());
            errors.add(error(
                    path,
                    A2UiValidationErrorCode.INVALID_MESSAGE_SEQUENCE,
                    "beginRendering must follow at least one surfaceUpdate for the same surface",
                    details));
            return;
        }

        if (!surfaceState.componentIds().contains(beginRendering.getRoot())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("surfaceId", beginRendering.getSurfaceId());
            details.put("root", beginRendering.getRoot());
            details.put("knownComponentIds", List.copyOf(surfaceState.componentIds()));
            errors.add(error(
                    path + ".root",
                    A2UiValidationErrorCode.UNKNOWN_ROOT_COMPONENT,
                    "beginRendering.root must reference a previously defined component on the same surface",
                    details));
        }
    }

    private boolean hasExactlyOnePayload(A2UiMessage message) {
        int fieldCount = 0;
        if (message.getSurfaceUpdate() != null) {
            fieldCount++;
        }
        if (message.getDataModelUpdate() != null) {
            fieldCount++;
        }
        if (message.getBeginRendering() != null) {
            fieldCount++;
        }
        if (message.getDeleteSurface() != null) {
            fieldCount++;
        }
        return fieldCount == 1;
    }

    private void validateSurfaceUpdate(
            String path,
            A2UiMessage.SurfaceUpdate surfaceUpdate,
            List<A2UiValidationError> errors
    ) {
        if (isBlank(surfaceUpdate.getSurfaceId())) {
            errors.add(error(path + SURFACE_ID_SUFFIX, A2UiValidationErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }

        if (surfaceUpdate.getComponents() == null) {
            errors.add(error(
                    path + ".components",
                    A2UiValidationErrorCode.INVALID_COMPONENT_DEFINITION,
                    "components must be an array"));
            return;
        }

        for (int index = 0; index < surfaceUpdate.getComponents().size(); index++) {
            validateComponentDefinition(
                    surfaceUpdate.getComponents().get(index),
                    path + ".components[" + index + "]",
                    errors);
        }
    }

    private void validateComponentDefinition(
            A2UiMessage.ComponentDefinition componentDefinition,
            String path,
            List<A2UiValidationError> errors
    ) {
        if (componentDefinition == null) {
            errors.add(error(path, A2UiValidationErrorCode.INVALID_COMPONENT_DEFINITION, "component definition must not be null"));
            return;
        }

        if (isBlank(componentDefinition.getId())) {
            errors.add(error(path + ".id", A2UiValidationErrorCode.MISSING_COMPONENT_ID, "component id is required"));
        }

        Map<String, Object> component = componentDefinition.getComponent();
        if (component == null || component.isEmpty() || component.size() != 1) {
            errors.add(error(
                    path + ".component",
                    A2UiValidationErrorCode.INVALID_COMPONENT_PAYLOAD,
                    "component payload must contain exactly one component definition"));
            return;
        }

        Map.Entry<String, Object> entry = component.entrySet().iterator().next();
        if (isBlank(entry.getKey()) || !(entry.getValue() instanceof Map<?, ?>)) {
            errors.add(error(
                    path + ".component",
                    A2UiValidationErrorCode.INVALID_COMPONENT_PAYLOAD,
                    "component payload must map a non-blank component name to an object"));
            return;
        }

        if (!catalogRegistry.supportsComponentType(entry.getKey())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("componentType", entry.getKey());
            details.put("supportedCatalogIds", List.copyOf(catalogRegistry.supportedCatalogIds()));
            errors.add(error(
                    path + ".component." + entry.getKey(),
                    A2UiValidationErrorCode.UNKNOWN_COMPONENT_TYPE,
                    "component type is not supported by the published catalog",
                    details));
        }
    }

    private void validateDataModelUpdate(
            String path,
            A2UiMessage.DataModelUpdate dataModelUpdate,
            List<A2UiValidationError> errors
    ) {
        if (isBlank(dataModelUpdate.getSurfaceId())) {
            errors.add(error(path + SURFACE_ID_SUFFIX, A2UiValidationErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
        if (dataModelUpdate.getPath() != null && dataModelUpdate.getPath().isBlank()) {
            errors.add(error(path + ".path", A2UiValidationErrorCode.INVALID_DATA_UPDATE, "path is required"));
        }
        if (dataModelUpdate.getContents() == null) {
            errors.add(error(path + ".contents", A2UiValidationErrorCode.INVALID_DATA_UPDATE, "contents must be an array"));
            return;
        }

        for (int index = 0; index < dataModelUpdate.getContents().size(); index++) {
            validateDataEntry(dataModelUpdate.getContents().get(index), path + ".contents[" + index + "]", errors);
        }
    }

    private void validateDataEntry(
            A2UiMessage.DataEntry dataEntry,
            String path,
            List<A2UiValidationError> errors
    ) {
        if (dataEntry == null) {
            errors.add(error(path, A2UiValidationErrorCode.INVALID_DATA_ENTRY, "data entry must not be null"));
            return;
        }

        if (isBlank(dataEntry.getKey())) {
            errors.add(error(path + ".key", A2UiValidationErrorCode.INVALID_DATA_ENTRY, "data entry key is required"));
        }

        int valueCount = 0;
        if (dataEntry.getValueString() != null) {
            valueCount++;
        }
        if (dataEntry.getValueNumber() != null) {
            valueCount++;
        }
        if (dataEntry.getValueBoolean() != null) {
            valueCount++;
        }
        if (dataEntry.getValueMap() != null) {
            valueCount++;
        }

        if (valueCount != 1) {
            errors.add(error(
                    path,
                    A2UiValidationErrorCode.INVALID_DATA_ENTRY,
                    "data entry must contain exactly one value field"));
            return;
        }

        if (dataEntry.getValueMap() != null) {
            for (int index = 0; index < dataEntry.getValueMap().size(); index++) {
                validateDataEntry(dataEntry.getValueMap().get(index), path + ".valueMap[" + index + "]", errors);
            }
        }
    }

    private void validateBeginRendering(
            String path,
            A2UiMessage.BeginRendering beginRendering,
            List<A2UiValidationError> errors
    ) {
        if (isBlank(beginRendering.getSurfaceId())) {
            errors.add(error(path + SURFACE_ID_SUFFIX, A2UiValidationErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
        if (isBlank(beginRendering.getRoot())) {
            errors.add(error(path + ".root", A2UiValidationErrorCode.MISSING_ROOT, "root is required"));
        }
        if (isBlank(beginRendering.getCatalogId())) {
            errors.add(error(path + ".catalogId", A2UiValidationErrorCode.MISSING_CATALOG_ID, "catalogId is required"));
        } else if (!catalogRegistry.isSupportedCatalogId(beginRendering.getCatalogId())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("catalogId", beginRendering.getCatalogId());
            details.put("supportedCatalogIds", List.copyOf(catalogRegistry.supportedCatalogIds()));
            errors.add(error(
                    path + ".catalogId",
                    A2UiValidationErrorCode.UNSUPPORTED_CATALOG_ID,
                    "catalogId is not supported by this runtime",
                    details));
        }
    }

    private void validateDeleteSurface(
            String path,
            A2UiMessage.DeleteSurface deleteSurface,
            List<A2UiValidationError> errors
    ) {
        if (isBlank(deleteSurface.getSurfaceId())) {
            errors.add(error(path + SURFACE_ID_SUFFIX, A2UiValidationErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
    }

    private A2UiValidationError error(String path, A2UiValidationErrorCode code, String message) {
        return error(path, code, message, null);
    }

    private A2UiValidationError error(
            String path,
            A2UiValidationErrorCode code,
            String message,
            Map<String, Object> details
    ) {
        return A2UiValidationError.builder()
                .path(path)
                .code(code.code())
                .category(code.category().name())
                .message(message)
                .details(details)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SurfaceState(Set<String> componentIds) {

        private SurfaceState() {
            this(new LinkedHashSet<>());
        }
    }
}