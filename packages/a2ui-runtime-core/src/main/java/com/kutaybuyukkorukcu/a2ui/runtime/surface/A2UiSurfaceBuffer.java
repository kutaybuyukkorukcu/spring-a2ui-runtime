package com.kutaybuyukkorukcu.a2ui.runtime.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.DataEntry;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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

    public void applySurfaceUpdate(A2UiMessage.SurfaceUpdate update) {
        SurfaceState state = getOrCreateSurface(update.surfaceId());
        for (A2UiMessage.ComponentDefinition component : update.components()) {
            state.addComponent(component.id(), component.componentType());
        }
    }

    public void applyDataModelUpdate(A2UiMessage.DataModelUpdate update) {
        SurfaceState state = getOrCreateSurface(update.surfaceId());
        String basePath = update.path() == null ? "" : update.path();
        state.applyDataEntries(basePath, update.contents());
    }

    public void applyBeginRendering(A2UiMessage.BeginRendering beginRendering) {
        SurfaceState state = getOrCreateSurface(beginRendering.surfaceId());
        state.setRenderingBegun(true);
        state.setRootComponentId(beginRendering.root());
        state.setCatalogId(beginRendering.catalogId());
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
        private boolean renderingBegun = false;
        private String rootComponentId;
        private String catalogId;

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

        public void setRenderingBegun(boolean begun) {
            this.renderingBegun = begun;
        }

        public boolean isRenderingBegun() {
            return renderingBegun;
        }

        public void setRootComponentId(String rootComponentId) {
            this.rootComponentId = rootComponentId;
        }

        public String getRootComponentId() {
            return rootComponentId;
        }

        public void setCatalogId(String catalogId) {
            this.catalogId = catalogId;
        }

        public String getCatalogId() {
            return catalogId;
        }

        public void applyDataEntries(String basePath, List<DataEntry> entries) {
            for (DataEntry entry : entries) {
                String fullPath = basePath.isEmpty() ? entry.key() : basePath + "/" + entry.key();
                if (entry.valueMap() != null) {
                    applyNestedData(fullPath, entry.valueMap());
                } else if (entry.valueString() != null) {
                    setDataAtPath(fullPath, entry.valueString());
                } else if (entry.valueNumber() != null) {
                    setDataAtPath(fullPath, entry.valueNumber());
                } else if (entry.valueBoolean() != null) {
                    setDataAtPath(fullPath, entry.valueBoolean());
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void applyNestedData(String basePath, List<DataEntry> entries) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (DataEntry entry : entries) {
                if (entry.valueMap() != null) {
                    Map<String, Object> child = new LinkedHashMap<>();
                    for (DataEntry childEntry : entry.valueMap()) {
                        child.put(childEntry.key(), getValueFromEntry(childEntry));
                    }
                    nested.put(entry.key(), child);
                } else {
                    nested.put(entry.key(), getValueFromEntry(entry));
                }
            }
            setDataAtPath(basePath, nested);
        }

        private Object getValueFromEntry(DataEntry entry) {
            if (entry.valueString() != null) return entry.valueString();
            if (entry.valueNumber() != null) return entry.valueNumber();
            if (entry.valueBoolean() != null) return entry.valueBoolean();
            if (entry.valueMap() != null) return entry.valueMap();
            return null;
        }

        @SuppressWarnings("unchecked")
        private void setDataAtPath(String path, Object value) {
            String[] parts = path.split("/");
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

        public Object getDataAtPath(String path) {
            if (path == null || path.isEmpty()) {
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