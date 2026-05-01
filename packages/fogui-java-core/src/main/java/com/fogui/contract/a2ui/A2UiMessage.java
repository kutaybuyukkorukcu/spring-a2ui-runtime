package com.fogui.contract.a2ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class A2UiMessage {

    private SurfaceUpdate surfaceUpdate;
    private DataModelUpdate dataModelUpdate;
    private BeginRendering beginRendering;
    private DeleteSurface deleteSurface;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SurfaceUpdate {

        private String surfaceId;

        @Builder.Default
        private List<ComponentDefinition> components = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ComponentDefinition {

        private String id;

        @Builder.Default
        private Map<String, Object> component = new LinkedHashMap<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataModelUpdate {

        private String surfaceId;
        private String path;

        @Builder.Default
        private List<DataEntry> contents = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataEntry {

        private String key;
        private String valueString;
        private Number valueNumber;
        private Boolean valueBoolean;
        private List<DataEntry> valueMap;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BeginRendering {

        private String surfaceId;
        private String root;
        private String catalogId;
        private Map<String, Object> styles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeleteSurface {

        private String surfaceId;
    }
}