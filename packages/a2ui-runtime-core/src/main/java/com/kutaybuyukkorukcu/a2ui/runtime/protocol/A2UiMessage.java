package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface A2UiMessage {

    String surfaceId();

    record CreateSurface(
            @JsonProperty("surfaceId") String surfaceId,
            @JsonProperty("catalogId") String catalogId,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("theme") Map<String, Object> theme,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("sendDataModel") Boolean sendDataModel,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("components") List<ComponentDefinition> components,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("dataModel") Object dataModel
    ) implements A2UiMessage {
        public CreateSurface(String surfaceId, String catalogId) {
            this(surfaceId, catalogId, null, null, null, null);
        }

        public CreateSurface {
            components = components == null ? null : List.copyOf(components);
        }
    }

    record UpdateComponents(
            @JsonProperty("surfaceId") String surfaceId,
            @JsonProperty("components") List<ComponentDefinition> components
    ) implements A2UiMessage {
        public UpdateComponents {
            components = components == null ? List.of() : List.copyOf(components);
        }
    }

    record UpdateDataModel(
            @JsonProperty("surfaceId") String surfaceId,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("path") String path,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("value") Object value
    ) implements A2UiMessage {
    }

    record DeleteSurface(
            @JsonProperty("surfaceId") String surfaceId
    ) implements A2UiMessage {
    }

    /**
     * Agent asks the host frontend to run a catalog function.
     * {@code catalogId} is required on this envelope.
     */
    record CallRendererFunction(
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("surfaceId") String surfaceId,
            @JsonProperty("functionCallId") String functionCallId,
            @JsonProperty("catalogId") String catalogId,
            @JsonProperty("call") String call,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("args") Map<String, Object> args
    ) implements A2UiMessage {
        public CallRendererFunction {
            args = args == null || args.isEmpty() ? null : Map.copyOf(args);
        }
    }

    /**
     * Result of a renderer {@code callAgentFunction}. No surface id on the wire.
     */
    record AgentFunctionResponse(
            @JsonProperty("functionCallId") String functionCallId,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Object value,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String errorCode,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String errorMessage
    ) implements A2UiMessage {
        public AgentFunctionResponse {
            boolean hasError = errorCode != null;
            if (hasError && (errorCode.isBlank() || errorMessage == null || errorMessage.isBlank())) {
                throw new IllegalArgumentException("function error requires code and message");
            }
        }

        @Override
        public String surfaceId() {
            return null;
        }

        public boolean failed() {
            return errorCode != null;
        }

        public static AgentFunctionResponse ok(String functionCallId, Object value) {
            return new AgentFunctionResponse(functionCallId, value, null, null);
        }

        public static AgentFunctionResponse error(String functionCallId, String code, String message) {
            return new AgentFunctionResponse(functionCallId, null, code, message);
        }
    }

    /**
     * Flat v0.9.1 component: {@code {"id":"...","component":"Text","text":"Hello"}}.
     * Sibling properties (excluding {@code id} and {@code component}) live in {@link #properties()}.
     */
    record ComponentDefinition(
            @JsonProperty("id") String id,
            @JsonProperty("component") String component,
            Map<String, Object> properties
    ) {
        public ComponentDefinition {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("component id is required");
            }
            if (component == null || component.isBlank()) {
                throw new IllegalArgumentException("component type is required");
            }
            properties = properties == null
                    ? Map.of()
                    : Map.copyOf(new LinkedHashMap<>(properties));
        }

        public ComponentDefinition(String id, String component) {
            this(id, component, Map.of());
        }

        public String componentType() {
            return component;
        }

        public Map<String, Object> componentProperties() {
            return properties;
        }

        /** Build from a flat planner/tool map ({@code id}, {@code component}, plus props). */
        public static ComponentDefinition fromFlatMap(Map<String, Object> flat) {
            if (flat == null) {
                throw new IllegalArgumentException("component map is required");
            }
            Object id = flat.get("id");
            Object type = flat.get("component");
            if (!(id instanceof String idValue) || idValue.isBlank()) {
                throw new IllegalArgumentException("component id is required");
            }
            if (!(type instanceof String typeValue) || typeValue.isBlank()) {
                throw new IllegalArgumentException("component type must be a non-blank string");
            }
            Map<String, Object> props = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : flat.entrySet()) {
                String key = entry.getKey();
                if ("id".equals(key) || "component".equals(key) || "weight".equals(key)) {
                    continue;
                }
                props.put(key, entry.getValue());
            }
            // Preserve optional weight as a property if present (catalog ComponentCommon)
            Object weight = flat.get("weight");
            if (weight != null) {
                props.put("weight", weight);
            }
            return new ComponentDefinition(idValue, typeValue, props);
        }

        public Map<String, Object> toFlatMap() {
            Map<String, Object> flat = new LinkedHashMap<>();
            flat.put("id", id);
            flat.put("component", component);
            flat.putAll(properties);
            return flat;
        }
    }
}
