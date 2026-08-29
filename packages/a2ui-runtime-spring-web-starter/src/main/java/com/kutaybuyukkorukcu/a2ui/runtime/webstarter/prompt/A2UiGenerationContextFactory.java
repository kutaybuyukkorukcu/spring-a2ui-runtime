package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

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
}
