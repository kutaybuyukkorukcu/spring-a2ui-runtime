package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reusable {@link A2UiSurfaceSpec} for controlled-layout templates: a fixed component tree
 * (optionally shaped by slot values, e.g. to add an optional row) plus slot values copied
 * verbatim into the surface data model at {@code "/"}.
 * <p>
 * Hosts authoring a {@link A2UiTemplateCustomizer} typically build their component tree with
 * {@link A2UiMessage.ComponentDefinition} records directly and use this class instead of
 * reimplementing the slot → data model plumbing that every fixed template needs.
 */
public final class A2UiFixedSurfaceSpec implements A2UiSurfaceSpec {

    private final String templateId;
    private final String rootComponentId;
    private final Set<String> requiredSlots;
    private final Set<String> optionalSlots;
    private final Function<Map<String, String>, List<ComponentDefinition>> componentsSupplier;
    private final List<String> dataModelKeys;

    private A2UiFixedSurfaceSpec(Builder builder) {
        this.templateId = builder.templateId;
        this.rootComponentId = builder.rootComponentId;
        this.requiredSlots = Set.copyOf(builder.requiredSlots);
        this.optionalSlots = Set.copyOf(builder.optionalSlots);
        this.componentsSupplier = builder.componentsSupplier;
        this.dataModelKeys = builder.dataModelKeys == null
                ? mergedSlotKeys(builder.requiredSlots, builder.optionalSlots)
                : List.copyOf(builder.dataModelKeys);
    }

    public static Builder builder(String templateId, String rootComponentId) {
        return new Builder(templateId, rootComponentId);
    }

    @Override
    public String templateId() {
        return templateId;
    }

    @Override
    public String rootComponentId() {
        return rootComponentId;
    }

    @Override
    public Set<String> requiredSlots() {
        return requiredSlots;
    }

    @Override
    public Set<String> optionalSlots() {
        return optionalSlots;
    }

    @Override
    public List<A2UiMessage> buildMessages(String surfaceId, Map<String, String> slots) {
        List<ComponentDefinition> components = componentsSupplier.apply(slots);

        Map<String, Object> data = new LinkedHashMap<>();
        for (String key : dataModelKeys) {
            String value = slots.get(key);
            if (value != null) {
                data.put(key, value);
            }
        }

        List<A2UiMessage> messages = new ArrayList<>();
        messages.add(new A2UiMessage.UpdateComponents(surfaceId, components));
        if (!data.isEmpty()) {
            messages.add(new A2UiMessage.UpdateDataModel(surfaceId, "/", data));
        }
        return List.copyOf(messages);
    }

    private static List<String> mergedSlotKeys(Set<String> required, Set<String> optional) {
        List<String> keys = new ArrayList<>(required);
        keys.addAll(optional);
        return List.copyOf(keys);
    }

    public static final class Builder {

        private final String templateId;
        private final String rootComponentId;
        private Set<String> requiredSlots = Set.of();
        private Set<String> optionalSlots = Set.of();
        private Function<Map<String, String>, List<ComponentDefinition>> componentsSupplier;
        private List<String> dataModelKeys;

        private Builder(String templateId, String rootComponentId) {
            this.templateId = Objects.requireNonNull(templateId, "templateId");
            this.rootComponentId = Objects.requireNonNull(rootComponentId, "rootComponentId");
        }

        public Builder requiredSlots(String... slots) {
            this.requiredSlots = Set.of(slots);
            return this;
        }

        public Builder optionalSlots(String... slots) {
            this.optionalSlots = Set.of(slots);
            return this;
        }

        /** Component tree that does not depend on slot values. */
        public Builder components(Supplier<List<ComponentDefinition>> supplier) {
            Objects.requireNonNull(supplier, "supplier");
            this.componentsSupplier = slots -> supplier.get();
            return this;
        }

        /** Component tree shaped by slot values (e.g. to add an optional row). */
        public Builder components(Function<Map<String, String>, List<ComponentDefinition>> supplier) {
            this.componentsSupplier = Objects.requireNonNull(supplier, "supplier");
            return this;
        }

        /**
         * Slot keys copied into the data model at {@code "/"} (missing/blank values are
         * skipped). Defaults to required + optional slots when not set.
         */
        public Builder dataModelKeys(String... keys) {
            this.dataModelKeys = List.of(keys);
            return this;
        }

        public A2UiFixedSurfaceSpec build() {
            Objects.requireNonNull(componentsSupplier, "components supplier is required");
            return new A2UiFixedSurfaceSpec(this);
        }
    }
}
