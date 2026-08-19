package com.kutaybuyukkorukcu.a2ui.runtime.validation;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiProtocol;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class A2UiMessageValidator {

    private static final String SURFACE_ID_SUFFIX = ".surfaceId";
    private static final String SURFACE_ID_REQUIRED = "surfaceId is required";
    private static final String ROOT_COMPONENT_ID = "root";

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

        A2UiValidationContext resolved = resolveCatalogContext(context, messages);
        for (int i = 0; i < messages.size(); i++) {
            validateMessage(messages.get(i), "$[" + i + "]", resolved, diagnostics);
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

    /**
     * Validates one envelope. Catalog is taken from {@code context}, else inferred when
     * {@code message} is {@code CreateSurface}. Empty context keeps the global type union
     * and skips prop checks — pass {@link A2UiValidationContext#forCatalog(String)} for
     * catalog-scoped fail-fast on {@code updateComponents}.
     */
    public List<A2UiDiagnostic> validateSingle(A2UiMessage message, A2UiValidationContext context) {
        List<A2UiDiagnostic> diagnostics = new ArrayList<>();
        validateVersion(context, diagnostics);
        A2UiValidationContext resolved = resolveCatalogContext(
                context, message == null ? List.of() : List.of(message));
        validateMessage(message, "$[0]", resolved, diagnostics);
        return diagnostics;
    }

    private void validateVersion(A2UiValidationContext context, List<A2UiDiagnostic> diagnostics) {
        if (context == null || context.requestedVersion() == null || context.requestedVersion().isBlank()) {
            return;
        }
        if (!A2UiProtocol.isSupportedVersion(context.requestedVersion())) {
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
            case A2UiMessage.CreateSurface cs -> validateCreateSurface(path + ".createSurface", cs, diagnostics);
            case A2UiMessage.UpdateComponents uc ->
                    validateUpdateComponents(path + ".updateComponents", uc, context, diagnostics);
            case A2UiMessage.UpdateDataModel udm ->
                    validateUpdateDataModel(path + ".updateDataModel", udm, diagnostics);
            case A2UiMessage.DeleteSurface ds -> validateDeleteSurface(path + ".deleteSurface", ds, diagnostics);
        }
    }

    private void validateCreateSurface(String path, A2UiMessage.CreateSurface cs, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(cs.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
        if (isBlank(cs.catalogId())) {
            diagnostics.add(diagnostic(path + ".catalogId", A2UiErrorCode.MISSING_CATALOG_ID, "catalogId is required"));
        } else if (!catalogRegistry.isSupportedCatalogId(cs.catalogId())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("catalogId", cs.catalogId());
            details.put("supportedCatalogIds", List.copyOf(catalogRegistry.supportedCatalogIds()));
            diagnostics.add(diagnostic(path + ".catalogId", A2UiErrorCode.UNSUPPORTED_CATALOG_ID,
                    "catalogId is not supported by this runtime", details));
        }
    }

    private void validateUpdateComponents(String path, A2UiMessage.UpdateComponents uc,
                                          A2UiValidationContext context, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(uc.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }

        List<A2UiMessage.ComponentDefinition> components = uc.components();
        if (components == null) {
            diagnostics.add(diagnostic(path + ".components", A2UiErrorCode.INVALID_COMPONENT_DEFINITION,
                    "components must be an array"));
            return;
        }

        for (int i = 0; i < components.size(); i++) {
            validateComponentDefinition(components.get(i), path + ".components[" + i + "]", context, diagnostics);
        }
    }

    private void validateComponentDefinition(A2UiMessage.ComponentDefinition cd, String path,
                                              A2UiValidationContext context, List<A2UiDiagnostic> diagnostics) {
        if (cd == null) {
            diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_COMPONENT_DEFINITION,
                    "component definition must not be null"));
            return;
        }

        if (isBlank(cd.id())) {
            diagnostics.add(diagnostic(path + ".id", A2UiErrorCode.MISSING_COMPONENT_ID, "component id is required"));
        }

        String componentType = cd.componentType();
        if (isBlank(componentType)) {
            diagnostics.add(diagnostic(path + ".component", A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                    "component type string is required"));
            return;
        }

        if (!isSupportedComponentType(componentType, context)) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("componentType", componentType);
            details.put("supportedCatalogIds", List.copyOf(catalogRegistry.supportedCatalogIds()));
            if (context != null && context.catalogId() != null && !context.catalogId().isBlank()) {
                details.put("catalogId", context.catalogId());
            }
            diagnostics.add(diagnostic(path + ".component", A2UiErrorCode.UNKNOWN_COMPONENT_TYPE,
                    "component type is not supported by the published catalog", details));
            return;
        }

        if (context != null && context.catalogId() != null && !context.catalogId().isBlank()) {
            List<A2UiDiagnostic> propDiagnostics = catalogSchemaValidator.validateComponentProps(
                    componentType, context.catalogId(), cd.componentProperties(), path);
            diagnostics.addAll(propDiagnostics);
        }
    }

    private void validateUpdateDataModel(String path, A2UiMessage.UpdateDataModel udm,
                                         List<A2UiDiagnostic> diagnostics) {
        if (isBlank(udm.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
        if (udm.path() != null && udm.path().isBlank()) {
            diagnostics.add(diagnostic(path + ".path", A2UiErrorCode.INVALID_DATA_UPDATE,
                    "path must not be blank if present"));
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
            if (message == null) {
                continue;
            }

            switch (message) {
                case A2UiMessage.CreateSurface cs -> {
                    if (!isBlank(cs.surfaceId())) {
                        SurfaceState existing = surfaces.get(cs.surfaceId());
                        if (existing != null && existing.created) {
                            Map<String, Object> details = new LinkedHashMap<>();
                            details.put("surfaceId", cs.surfaceId());
                            diagnostics.add(diagnostic(
                                    "$[" + i + "].createSurface",
                                    A2UiErrorCode.INVALID_MESSAGE_SEQUENCE,
                                    "createSurface must not target an already created surface without deleteSurface",
                                    details));
                        } else {
                            surfaces.put(cs.surfaceId(), new SurfaceState(true));
                        }
                    }
                }
                case A2UiMessage.UpdateComponents uc ->
                        validateUpdateAgainstCreated(uc.surfaceId(), uc.components(),
                                "$[" + i + "].updateComponents", surfaces, diagnostics);
                case A2UiMessage.UpdateDataModel udm ->
                        validateSurfaceCreated(udm.surfaceId(), "$[" + i + "].updateDataModel", surfaces, diagnostics);
                case A2UiMessage.DeleteSurface ds -> {
                    if (!isBlank(ds.surfaceId())) {
                        surfaces.remove(ds.surfaceId());
                    }
                }
            }
        }

        for (Map.Entry<String, SurfaceState> entry : surfaces.entrySet()) {
            SurfaceState state = entry.getValue();
            if (state.created && !state.componentIds.contains(ROOT_COMPONENT_ID)) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("surfaceId", entry.getKey());
                details.put("knownComponentIds", List.copyOf(state.componentIds));
                diagnostics.add(diagnostic(
                        "$",
                        A2UiErrorCode.UNKNOWN_ROOT_COMPONENT,
                        "surface must define a component with id \"root\"",
                        details));
            }
        }
    }

    private void validateUpdateAgainstCreated(
            String surfaceId,
            List<A2UiMessage.ComponentDefinition> components,
            String path,
            Map<String, SurfaceState> surfaces,
            List<A2UiDiagnostic> diagnostics) {
        validateSurfaceCreated(surfaceId, path, surfaces, diagnostics);
        if (isBlank(surfaceId) || components == null) {
            return;
        }
        SurfaceState state = surfaces.computeIfAbsent(surfaceId, k -> new SurfaceState(false));
        for (A2UiMessage.ComponentDefinition cd : components) {
            if (cd != null && !isBlank(cd.id())) {
                state.componentIds.add(cd.id());
            }
        }
    }

    private void validateSurfaceCreated(
            String surfaceId,
            String path,
            Map<String, SurfaceState> surfaces,
            List<A2UiDiagnostic> diagnostics) {
        if (isBlank(surfaceId)) {
            return;
        }
        SurfaceState state = surfaces.get(surfaceId);
        if (state == null || !state.created) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("surfaceId", surfaceId);
            diagnostics.add(diagnostic(
                    path,
                    A2UiErrorCode.INVALID_MESSAGE_SEQUENCE,
                    "update messages require a prior createSurface for the same surfaceId",
                    details));
        }
    }

    private A2UiValidationContext resolveCatalogContext(A2UiValidationContext context, List<A2UiMessage> messages) {
        if (context != null && context.catalogId() != null && !context.catalogId().isBlank()) {
            return context;
        }
        String catalogId = catalogIdFrom(messages);
        if (catalogId == null) {
            return context == null ? A2UiValidationContext.empty() : context;
        }
        String version = context == null ? null : context.requestedVersion();
        return A2UiValidationContext.forVersionAndCatalog(version, catalogId);
    }

    /**
     * Catalog id from the first {@code CreateSurface} in {@code messages}, else {@code null}.
     * {@link #validateSingle} and batch {@link #validate} use this when the context has no catalog.
     */
    public static String catalogIdFrom(List<A2UiMessage> messages) {
        if (messages == null) {
            return null;
        }
        for (A2UiMessage message : messages) {
            if (message instanceof A2UiMessage.CreateSurface createSurface
                    && createSurface.catalogId() != null
                    && !createSurface.catalogId().isBlank()) {
                return createSurface.catalogId();
            }
        }
        return null;
    }

    private boolean isSupportedComponentType(String componentType, A2UiValidationContext context) {
        if (context != null && context.catalogId() != null && !context.catalogId().isBlank()) {
            return catalogRegistry.componentTypesForCatalog(context.catalogId()).contains(componentType);
        }
        return catalogRegistry.supportsComponentType(componentType);
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

    private static final class SurfaceState {
        private final boolean created;
        private final Set<String> componentIds = new LinkedHashSet<>();

        private SurfaceState(boolean created) {
            this.created = created;
        }
    }
}
