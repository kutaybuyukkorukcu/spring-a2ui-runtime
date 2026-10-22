package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.verification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ModelResponseFixture {

    private String id;
    private String prompt;
    private String catalogId;
    private String model;
    private RenderPayload render;
    private List<RenderPayload> renders;
    private String expect;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("prompt")
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @JsonProperty("catalogId")
    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    @JsonProperty("model")
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @JsonProperty("render")
    public RenderPayload getRender() {
        return render;
    }

    public void setRender(RenderPayload render) {
        this.render = render;
    }

    @JsonProperty("renders")
    public List<RenderPayload> getRenders() {
        return renders;
    }

    public void setRenders(List<RenderPayload> renders) {
        this.renders = renders;
    }

    @JsonProperty("expect")
    public String getExpect() {
        return expect;
    }

    public void setExpect(String expect) {
        this.expect = expect;
    }

    public String id() {
        return id;
    }

    public String prompt() {
        return prompt;
    }

    public String catalogId() {
        return catalogId;
    }

    public String model() {
        return model;
    }

    public RenderPayload render() {
        return render;
    }

    public List<RenderPayload> renders() {
        return renders;
    }

    public String expect() {
        return expect;
    }

    public List<RenderPayload> renderSequence() {
        if (renders != null && !renders.isEmpty()) {
            return renders;
        }
        if (render != null) {
            return List.of(render);
        }
        return List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class RenderPayload {
        private String surfaceId;
        private String root;
        private List<java.util.Map<String, Object>> components;
        private java.util.Map<String, Object> data;

        @JsonProperty("surfaceId")
        public String getSurfaceId() {
            return surfaceId;
        }

        public void setSurfaceId(String surfaceId) {
            this.surfaceId = surfaceId;
        }

        @JsonProperty("root")
        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        @JsonProperty("components")
        public List<java.util.Map<String, Object>> getComponents() {
            return components;
        }

        public void setComponents(List<java.util.Map<String, Object>> components) {
            this.components = components;
        }

        @JsonProperty("data")
        public java.util.Map<String, Object> getData() {
            return data;
        }

        public void setData(java.util.Map<String, Object> data) {
            this.data = data;
        }

        public String surfaceId() {
            return surfaceId;
        }

        public String root() {
            return root;
        }

        public List<java.util.Map<String, Object>> components() {
            return components;
        }

        public java.util.Map<String, Object> data() {
            return data;
        }
    }
}
