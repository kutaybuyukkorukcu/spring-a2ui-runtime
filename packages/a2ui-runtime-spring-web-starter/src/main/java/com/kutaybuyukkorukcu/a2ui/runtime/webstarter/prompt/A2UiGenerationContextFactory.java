package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;

public final class A2UiGenerationContextFactory {

    private final List<A2UiGenerationContextContributor> contributors;

    public A2UiGenerationContextFactory(List<A2UiGenerationContextContributor> contributors) {
        if (contributors == null || contributors.isEmpty()) {
            this.contributors = List.of();
        } else {
            List<A2UiGenerationContextContributor> ordered = new ArrayList<>(contributors);
            AnnotationAwareOrderComparator.sort(ordered);
            this.contributors = List.copyOf(ordered);
        }
    }

    public A2UiGenerationContext build(A2UiGenerationRequest request) {
        A2UiGenerationContext.Builder builder = new A2UiGenerationContext.Builder();
        for (A2UiGenerationContextContributor contributor : contributors) {
            contributor.contribute(request, builder);
        }
        builder.key(createKey(request, builder));
        appendDynamicSuffix(request, builder);
        return builder.build();
    }

    private void appendDynamicSuffix(A2UiGenerationRequest request, A2UiGenerationContext.Builder builder) {
        StringJoiner suffix = new StringJoiner("\n\n");
        if (request.content() != null && !request.content().isBlank()) {
            suffix.add(request.content());
        }
        if (request.contextHints() != null && !request.contextHints().isBlank()) {
            suffix.add("Context: " + request.contextHints());
        }
        builder.appendDynamic(suffix.toString());
    }

    private A2UiGenerationContextKey createKey(A2UiGenerationRequest request, A2UiGenerationContext.Builder builder) {
        String catalogId = request.catalogId();
        String model = request.model() == null ? "" : request.model();
        String catalogVersion = Integer.toHexString(builder.staticPrefix().hashCode());
        return new A2UiGenerationContextKey(
                catalogId,
                catalogVersion,
                model,
                request.generationMode(),
                contributorFingerprint(),
                allowedTypesFingerprint(request.allowedTypes()));
    }

    private static String allowedTypesFingerprint(Set<String> allowedTypes) {
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return "";
        }
        return allowedTypes.stream().sorted().collect(Collectors.joining(","));
    }

    private String contributorFingerprint() {
        return contributors.stream()
                .map(contributor -> contributor.getClass().getSimpleName())
                .collect(Collectors.joining(","));
    }
}
