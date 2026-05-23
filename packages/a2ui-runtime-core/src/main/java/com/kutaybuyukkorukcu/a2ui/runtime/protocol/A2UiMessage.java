package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public sealed interface A2UiMessage {

    String surfaceId();

    record SurfaceUpdate(
            @JsonProperty("surfaceId") String surfaceId,
            @JsonProperty("components") List<ComponentDefinition> components
    ) implements A2UiMessage {
        public SurfaceUpdate {
            components = components == null ? List.of() : List.copyOf(components);
        }
    }

    record DataModelUpdate(
            @JsonProperty("surfaceId") String surfaceId,
            @JsonProperty("path") String path,
            @JsonProperty("contents") List<DataEntry> contents
    ) implements A2UiMessage {
        public DataModelUpdate {
            contents = contents == null ? List.of() : List.copyOf(contents);
        }
    }

    record BeginRendering(
            @JsonProperty("surfaceId") String surfaceId,
            @JsonProperty("root") String root,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("catalogId") String catalogId,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("styles") Map<String, Object> styles
    ) implements A2UiMessage {

        public BeginRendering(@JsonProperty("surfaceId") String surfaceId,
                              @JsonProperty("root") String root,
                              @JsonProperty("styles") Map<String, Object> styles) {
            this(surfaceId, root, null, styles);
        }
    }

    record DeleteSurface(
            @JsonProperty("surfaceId") String surfaceId
    ) implements A2UiMessage {
    }

    record ComponentDefinition(
            @JsonProperty("id") String id,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("weight") Double weight,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("component") Map<String, Object> component
    ) {
        public ComponentDefinition {
            if (component == null || component.size() != 1) {
                throw new IllegalArgumentException(
                        "component must contain exactly one key (the component type), but got: " + component);
            }
        }

        public ComponentDefinition(@JsonProperty("id") String id,
                                   @JsonProperty("component") Map<String, Object> component) {
            this(id, null, component);
        }

        public String componentType() {
            return component.keySet().iterator().next();
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> componentProperties() {
            return (Map<String, Object>) component.values().iterator().next();
        }
    }
}