package com.fogui.backend.model.fogui;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Component props classes - aligned with frontend renderers
 */
public class ComponentProps {

    private ComponentProps() {
    }

    /**
     * Card component props - aligns with CardRendererProps
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardProps {
        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("data")
        private Object data;
    }

    /**
     * List component props - aligns with ListRendererProps
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListProps {
        @JsonProperty("title")
        private String title;

        @JsonProperty("items")
        private List<Object> items;

        @JsonProperty("layout")
        @Builder.Default
        private String layout = "grid"; // "list", "grid", "compact"
    }

    /**
     * Table component props - aligns with TableRendererProps
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableProps {
        @JsonProperty("title")
        private String title;

        @JsonProperty("columns")
        private List<Object> columns;

        @JsonProperty("rows")
        private List<Map<String, Object>> rows;

        @JsonProperty("sortable")
        private Boolean sortable;
    }

    /**
     * Chart component props - aligns with ChartRendererProps
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartProps {
        @JsonProperty("type")
        private String type; // "bar", "line", "pie", "area"

        @JsonProperty("title")
        private String title;

        @JsonProperty("data")
        private List<Map<String, Object>> data;

        @JsonProperty("xAxis")
        private String xAxis;

        @JsonProperty("yAxis")
        private String yAxis;
    }

    /**
     * Form field definition - aligns with frontend FormField
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormFieldProps {
        @JsonProperty("name")
        private String name;

        @JsonProperty("label")
        private String label;

        @JsonProperty("type")
        @Builder.Default
        private String type = "text"; // "text", "number", "email", "date", "select", "textarea"

        @JsonProperty("placeholder")
        private String placeholder;

        @JsonProperty("required")
        private Boolean required;

        @JsonProperty("options")
        private List<String> options; // For select fields

        @JsonProperty("defaultValue")
        private Object defaultValue;
    }

    /**
     * Form component props - aligns with FormRendererProps
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormProps {
        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("fields")
        private List<FormFieldProps> fields;

        @JsonProperty("submitText")
        @Builder.Default
        private String submitText = "Submit";
    }

    /**
     * MiniCard block props for KPIs/metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniCardBlockProps {
        @JsonProperty("cards")
        private List<MiniCard> cards;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniCard {
        @JsonProperty("title")
        private String title;

        @JsonProperty("value")
        private String value;

        @JsonProperty("trend")
        private String trend; // "up", "down", "neutral"

        @JsonProperty("change")
        private String change;
    }

    /**
     * Callout component props
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalloutProps {
        @JsonProperty("variant")
        @Builder.Default
        private String variant = "info"; // "info", "warning", "success", "error"

        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;
    }

    /**
     * Confirmation dialog props
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmationProps {
        @JsonProperty("title")
        private String title;

        @JsonProperty("message")
        private String message;

        @JsonProperty("confirmText")
        @Builder.Default
        private String confirmText = "Confirm";

        @JsonProperty("cancelText")
        @Builder.Default
        private String cancelText = "Cancel";

        @JsonProperty("variant")
        @Builder.Default
        private String variant = "info"; // "info", "warning", "danger"

        @JsonProperty("data")
        private Object data;
    }
}
