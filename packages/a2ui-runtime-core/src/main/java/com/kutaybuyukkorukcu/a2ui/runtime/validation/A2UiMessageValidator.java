package com.kutaybuyukkorukcu.a2ui.runtime.validation;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiProtocol;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.DataEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class A2UiMessageValidator {

    private static final String SURFACE_ID_SUFFIX = ".surfaceId";
    private static final String SURFACE_ID_REQUIRED = "surfaceId is required";

    private final A2UiCatalogRegistry catalogRegistry;
    private final A2UiCatalogSchemaValidator catalogSchemaValidator;

    public A2UiMessageValidator() {
        this(A2UiCatalogRegistry.shared());
    }

    public A2UiMessageValidator(A2UiCatalogRegistry catalogRegistry) {
        this(catalogRegistry, new A2UiCatalogSchemaValidator(catalogRegistry));
    }

    public A2UiMessageValidator(A2UiCatalogRegistry catalogRegistry,
                                 A2UiCatalogSchemaValidator catalogSchemaValidator) {
        this.catalogRegistry = catalogRegistry;
        this.catalogSchemaValidator = catalogSchemaValidator;
    }

    public List<A2UiDiagnostic> validate(List<A2UiMessage> messages) {
        return validate(messages, A2UiValidationContext.empty());
    }

    public List<A2UiDiagnostic> validate(List<A2UiMessage> messages, A2UiValidationContext context) {
        List<A2UiDiagnostic> diagnostics = new ArrayList<>();

        validateVersion(context, diagnostics);

        if (messages == null) {
            diagnostics.add(diagnostic("$", A2UiErrorCode.NULL_MESSAGE_BATCH, "message batch must not be null"));
            return diagnostics;
        }

        for (int i = 0; i < messages.size(); i++) {
            validateMessage(messages.get(i), "$[" + i + "]", context, diagnostics);
        }

        validateSequence(messages, diagnostics);

        return diagnostics;
    }

    public boolean isValid(List<A2UiMessage> messages) {
        return validate(messages).isEmpty();
    }

    public List<A2UiDiagnostic> validateSingle(A2UiMessage message) {
        return validateSingle(message, A2UiValidationContext.empty());
    }

    public List<A2UiDiagnostic> validateSingle(A2UiMessage message, A2UiValidationContext context) {
        List<A2UiDiagnostic> diagnostics = new ArrayList<>();
        validateVersion(context, diagnostics);
        validateMessage(message, "$[0]", context, diagnostics);
        return diagnostics;
    }

    private void validateVersion(A2UiValidationContext context, List<A2UiDiagnostic> diagnostics) {
        if (context == null || context.requestedVersion() == null || context.requestedVersion().isBlank()) {
            return;
        }
        if (!A2UiProtocol.SUPPORTED_VERSION.equals(context.requestedVersion())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("requestedVersion", context.requestedVersion());
            details.put("supportedVersion", A2UiProtocol.SUPPORTED_VERSION);
            diagnostics.add(diagnostic(
                    "$", A2UiErrorCode.UNSUPPORTED_VERSION,
                    "requested A2UI version is not supported", details));
        }
    }

    private void validateMessage(A2UiMessage message, String path, A2UiValidationContext context,
                                  List<A2UiDiagnostic> diagnostics) {
        if (message == null) {
            diagnostics.add(diagnostic(path, A2UiErrorCode.NULL_MESSAGE, "message must not be null"));
            return;
        }

        switch (message) {
            case A2UiMessage.SurfaceUpdate su -> validateSurfaceUpdate(path + ".surfaceUpdate", su, context, diagnostics);
            case A2UiMessage.DataModelUpdate dmu -> validateDataModelUpdate(path + ".dataModelUpdate", dmu, diagnostics);
            case A2UiMessage.BeginRendering br -> validateBeginRendering(path + ".beginRendering", br, diagnostics);
            case A2UiMessage.DeleteSurface ds -> validateDeleteSurface(path + ".deleteSurface", ds, diagnostics);
        }
    }

    private void validateSurfaceUpdate(String path, A2UiMessage.SurfaceUpdate su,
                                        A2UiValidationContext context, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(su.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }

        List<A2UiMessage.ComponentDefinition> components = su.components();
        if (components == null) {
            diagnostics.add(diagnostic(path + ".components", A2UiErrorCode.INVALID_COMPONENT_DEFINITION, "components must be an array"));
            return;
        }

        for (int i = 0; i < components.size(); i++) {
            validateComponentDefinition(components.get(i), path + ".components[" + i + "]", context, diagnostics);
        }
    }

    private void validateComponentDefinition(A2UiMessage.ComponentDefinition cd, String path,
                                              A2UiValidationContext context, List<A2UiDiagnostic> diagnostics) {
        if (cd == null) {
            diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_COMPONENT_DEFINITION, "component definition must not be null"));
            return;
        }

        if (isBlank(cd.id())) {
            diagnostics.add(diagnostic(path + ".id", A2UiErrorCode.MISSING_COMPONENT_ID, "component id is required"));
        }

        Map<String, Object> component = cd.component();
        if (component == null || component.isEmpty() || component.size() != 1) {
            diagnostics.add(diagnostic(path + ".component", A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                    "component payload must contain exactly one component definition"));
            return;
        }

        String componentType = cd.componentType();
        if (!catalogRegistry.supportsComponentType(componentType)) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("componentType", componentType);
            details.put("supportedCatalogIds", List.copyOf(catalogRegistry.supportedCatalogIds()));
            diagnostics.add(diagnostic(path + ".component." + componentType, A2UiErrorCode.UNKNOWN_COMPONENT_TYPE,
                    "component type is not supported by the published catalog", details));
            return;
        }

        if (context != null && context.catalogId() != null && !context.catalogId().isBlank()) {
            String propsPath = path + ".component." + componentType;
            List<A2UiDiagnostic> propDiagnostics = catalogSchemaValidator.validateComponentProps(
                    componentType, context.catalogId(), cd.componentProperties(), propsPath);
            diagnostics.addAll(propDiagnostics);
        }
    }

    private void validateDataModelUpdate(String path, A2UiMessage.DataModelUpdate dmu, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(dmu.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
        if (dmu.path() != null && dmu.path().isBlank()) {
            diagnostics.add(diagnostic(path + ".path", A2UiErrorCode.INVALID_DATA_UPDATE, "path must not be blank if present"));
        }
        List<DataEntry> contents = dmu.contents();
        if (contents == null) {
            diagnostics.add(diagnostic(path + ".contents", A2UiErrorCode.INVALID_DATA_UPDATE, "contents must be an array"));
            return;
        }
        for (int i = 0; i < contents.size(); i++) {
            validateDataEntry(contents.get(i), path + ".contents[" + i + "]", diagnostics);
        }
    }

    private void validateDataEntry(DataEntry entry, String path, List<A2UiDiagnostic> diagnostics) {
        if (entry == null) {
            diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_DATA_ENTRY, "data entry must not be null"));
            return;
        }
        if (isBlank(entry.key())) {
            diagnostics.add(diagnostic(path + ".key", A2UiErrorCode.INVALID_DATA_ENTRY, "data entry key is required"));
        }
        if (entry.valueMap() != null) {
            for (int i = 0; i < entry.valueMap().size(); i++) {
                validateDataEntry(entry.valueMap().get(i), path + ".valueMap[" + i + "]", diagnostics);
            }
        }
    }

    private void validateBeginRendering(String path, A2UiMessage.BeginRendering br, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(br.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
        if (isBlank(br.root())) {
            diagnostics.add(diagnostic(path + ".root", A2UiErrorCode.MISSING_ROOT, "root is required"));
        }
        if (isBlank(br.catalogId())) {
            diagnostics.add(diagnostic(path + ".catalogId", A2UiErrorCode.MISSING_CATALOG_ID, "catalogId is required"));
        } else if (!catalogRegistry.isSupportedCatalogId(br.catalogId())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("catalogId", br.catalogId());
            details.put("supportedCatalogIds", List.copyOf(catalogRegistry.supportedCatalogIds()));
            diagnostics.add(diagnostic(path + ".catalogId", A2UiErrorCode.UNSUPPORTED_CATALOG_ID,
                    "catalogId is not supported by this runtime", details));
        }
    }

    private void validateDeleteSurface(String path, A2UiMessage.DeleteSurface ds, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(ds.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
    }

    private void validateSequence(List<A2UiMessage> messages, List<A2UiDiagnostic> diagnostics) {
        Map<String, SurfaceState> surfaces = new LinkedHashMap<>();

        for (int i = 0; i < messages.size(); i++) {
            A2UiMessage message = messages.get(i);
            if (message == null) continue;

            switch (message) {
                case A2UiMessage.SurfaceUpdate su -> registerSurfaceUpdate(su, surfaces);
                case A2UiMessage.DataModelUpdate dmu -> { /* no sequence constraint */ }
                case A2UiMessage.BeginRendering br ->
                    validateBeginRenderingSequence(br, "$[" + i + "].beginRendering", surfaces, diagnostics);
                case A2UiMessage.DeleteSurface ds -> {
                    if (!isBlank(ds.surfaceId())) {
                        surfaces.remove(ds.surfaceId());
                    }
                }
            }
        }
    }

    private void registerSurfaceUpdate(A2UiMessage.SurfaceUpdate su, Map<String, SurfaceState> surfaces) {
        if (isBlank(su.surfaceId()) || su.components() == null) return;
        SurfaceState state = surfaces.computeIfAbsent(su.surfaceId(), k -> new SurfaceState());
        for (A2UiMessage.ComponentDefinition cd : su.components()) {
            if (cd != null && !isBlank(cd.id())) {
                state.componentIds.add(cd.id());
            }
        }
    }

    private void validateBeginRenderingSequence(A2UiMessage.BeginRendering br, String path,
                                                  Map<String, SurfaceState> surfaces, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(br.surfaceId()) || isBlank(br.root())) return;

        SurfaceState state = surfaces.get(br.surfaceId());
        if (state == null || state.componentIds.isEmpty()) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("surfaceId", br.surfaceId());
            details.put("root", br.root());
            diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_MESSAGE_SEQUENCE,
                    "beginRendering must follow at least one surfaceUpdate for the same surface", details));
            return;
        }

        if (!state.componentIds.contains(br.root())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("surfaceId", br.surfaceId());
            details.put("root", br.root());
            details.put("knownComponentIds", List.copyOf(state.componentIds));
            diagnostics.add(diagnostic(path + ".root", A2UiErrorCode.UNKNOWN_ROOT_COMPONENT,
                    "beginRendering.root must reference a previously defined component on the same surface", details));
        }
    }

    private A2UiDiagnostic diagnostic(String path, A2UiErrorCode code, String message) {
        return new A2UiDiagnostic(path, code.code(), code.category().name(), message, null);
    }

    private A2UiDiagnostic diagnostic(String path, A2UiErrorCode code, String message, Map<String, Object> details) {
        return new A2UiDiagnostic(path, code.code(), code.category().name(), message, details);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SurfaceState(Set<String> componentIds) {
        SurfaceState() {
            this(new LinkedHashSet<>());
        }
    }
}
