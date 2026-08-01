package com.kutaybuyukkorukcu.a2ui.runtime.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class A2UiSurfaceBuffer {

    private final Map<String, SurfaceState> surfaces = new ConcurrentHashMap<>();

    public SurfaceState getOrCreateSurface(String surfaceId) {
        return surfaces.computeIfAbsent(surfaceId, k -> new SurfaceState());
    }

    public SurfaceState getSurface(String surfaceId) {
        return surfaces.get(surfaceId);
    }

    public boolean hasSurface(String surfaceId) {
        return surfaces.containsKey(surfaceId);
    }

    public void deleteSurface(String surfaceId) {
        surfaces.remove(surfaceId);
    }

    public void applyCreateSurface(A2UiMessage.CreateSurface create) {
        SurfaceState state = getOrCreateSurface(create.surfaceId());
        state.setCreated(true);
        state.setCatalogId(create.catalogId());
        state.setSendDataModel(Boolean.TRUE.equals(create.sendDataModel()));
        if (create.theme() != null) {
            state.setTheme(create.theme());
        }
    }

    public void applyUpdateComponents(A2UiMessage.UpdateComponents update) {
        SurfaceState state = getOrCreateSurface(update.surfaceId());
        for (A2UiMessage.ComponentDefinition component : update.components()) {
            state.addComponent(component.id(), component.componentType());
        }
    }

    public void applyUpdateDataModel(A2UiMessage.UpdateDataModel update) {
        SurfaceState state = getOrCreateSurface(update.surfaceId());
        String path = update.path() == null || update.path().isBlank() ? "/" : update.path();
        state.applyDataValue(path, update.value());
    }

    public Set<String> surfaceIds() {
        return Set.copyOf(surfaces.keySet());
    }

    public void clear() {
        surfaces.clear();
    }

    public static final class SurfaceState {
        private final Map<String, String> componentMap = new LinkedHashMap<>();
        private final Map<String, Object> dataModel = new LinkedHashMap<>();
        private boolean created = false;
        private boolean sendDataModel = false;
        private String catalogId;
        private Map<String, Object> theme;

        public void addComponent(String componentId, String componentType) {
            componentMap.put(componentId, componentType);
        }

        public boolean hasComponent(String componentId) {
            return componentMap.containsKey(componentId);
        }

        public String componentTypeOf(String componentId) {
            return componentMap.get(componentId);
        }

        public Set<String> componentIds() {
            return Set.copyOf(componentMap.keySet());
        }

        public void setCreated(boolean created) {
            this.created = created;
        }

        public boolean isCreated() {
            return created;
        }

        /** @deprecated use {@link #isCreated()} — v0.9.1 has no beginRendering */
        @Deprecated
        public boolean isRenderingBegun() {
            return created;
        }

        /** @deprecated no-op compatibility for callers still setting beginRendering flags */
        @Deprecated
        public void setRenderingBegun(boolean begun) {
            this.created = begun;
        }

        public String getRootComponentId() {
            return hasComponent("root") ? "root" : null;
        }

        /** @deprecated root is always component id {@code root} in v0.9.1 */
        @Deprecated
        public void setRootComponentId(String rootComponentId) {
            // no-op: root is identified by component id "root"
        }

        public void setCatalogId(String catalogId) {
            this.catalogId = catalogId;
        }

        public String getCatalogId() {
            return catalogId;
        }

        public void setSendDataModel(boolean sendDataModel) {
            this.sendDataModel = sendDataModel;
        }

        public boolean isSendDataModel() {
            return sendDataModel;
        }

        public void setTheme(Map<String, Object> theme) {
            this.theme = theme == null ? null : Map.copyOf(theme);
        }

        public Map<String, Object> getTheme() {
            return theme;
        }

        @SuppressWarnings("unchecked")
        public void applyDataValue(String path, Object value) {
            if (path == null || path.isBlank() || "/".equals(path)) {
                dataModel.clear();
                if (value == null) {
                    return;
                }
                if (value instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        dataModel.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    return;
                }
                throw new IllegalArgumentException("root data model value must be an object");
            }

            String normalized = path.startsWith("/") ? path.substring(1) : path;
            if (normalized.isBlank()) {
                applyDataValue("/", value);
                return;
            }

            String[] parts = normalized.split("/");
            if (value == null) {
                deleteAtPath(parts);
                return;
            }
            setDataAtPath(parts, value);
        }

        @SuppressWarnings("unchecked")
        private void setDataAtPath(String[] parts, Object value) {
            Map<String, Object> current = dataModel;
            for (int i = 0; i < parts.length - 1; i++) {
                Object next = current.get(parts[i]);
                if (!(next instanceof Map)) {
                    next = new LinkedHashMap<>();
                    current.put(parts[i], next);
                }
                current = (Map<String, Object>) next;
            }
            current.put(parts[parts.length - 1], value);
        }

        @SuppressWarnings("unchecked")
        private void deleteAtPath(String[] parts) {
            Map<String, Object> current = dataModel;
            for (int i = 0; i < parts.length - 1; i++) {
                Object next = current.get(parts[i]);
                if (!(next instanceof Map<?, ?> map)) {
                    return;
                }
                current = (Map<String, Object>) map;
            }
            current.remove(parts[parts.length - 1]);
        }

        public Object getDataAtPath(String path) {
            if (path == null || path.isEmpty() || "/".equals(path)) {
                return dataModel;
            }
            String[] parts = path.startsWith("/") ? path.substring(1).split("/") : path.split("/");
            Object current = dataModel;
            for (String part : parts) {
                if (current instanceof Map<?, ?> map) {
                    current = map.get(part);
                } else {
                    return null;
                }
            }
            return current;
        }
    }
}
