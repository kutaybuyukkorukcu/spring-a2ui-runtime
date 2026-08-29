package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionAllowList;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiGenerationContextFactoryTest {

    private static final String USER_CONTENT = "show the sales dashboard for Q3";
    private static final String CONTEXT_HINTS = "Intent: dashboard";

    private final A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
    private final A2UiGenerationContextFactory factory = new A2UiGenerationContextFactory(
            List.of(new CoreCatalogContributor(registry)));

    @Test
    void staticPrefixContainsDigestAndRulesButNotUserContent() {
        A2UiGenerationContext context = factory.build(request(USER_CONTENT, CONTEXT_HINTS, null));

        assertThat(context.staticPrefix()).contains("required:");
        assertThat(context.staticPrefix()).contains("Catalog rules:");
        assertThat(context.staticPrefix()).doesNotContain(USER_CONTENT);
    }

    @Test
    void dynamicSuffixContainsUserContentAndHintsWithoutDigest() {
        A2UiGenerationContext context = factory.build(request(USER_CONTENT, CONTEXT_HINTS, null));

        assertThat(context.dynamicSuffix()).contains(USER_CONTENT);
        assertThat(context.dynamicSuffix()).contains("Context: " + CONTEXT_HINTS);
        assertThat(context.dynamicSuffix()).doesNotContain("required:");
    }

    @Test
    void pruneLimitsDigestToAllowedComponentHeaders() {
        A2UiGenerationContext context = factory.build(request(USER_CONTENT, CONTEXT_HINTS, Set.of("Button")));

        assertThat(context.staticPrefix()).contains("\nButton\n  required:");
        assertThat(context.staticPrefix()).doesNotContain("\nText\n  required:");
    }

    @Test
    void hostContributorStaticLineAppearsAfterCoreDigest() {
        A2UiGenerationContextFactory withHost = new A2UiGenerationContextFactory(List.of(
                new CoreCatalogContributor(registry),
                new DomainContributor()));

        A2UiGenerationContext context = withHost.build(request(USER_CONTENT, CONTEXT_HINTS, null));

        assertThat(context.staticPrefix()).contains("required:");
        assertThat(context.staticPrefix()).contains("DOMAIN:");
        assertThat(context.staticPrefix().indexOf("DOMAIN:"))
                .isGreaterThan(context.staticPrefix().indexOf("required:"));
    }

    @Test
    void factorySortsContributorsByOrderedSoCoreRunsFirst() {
        A2UiGenerationContextFactory mixedOrder = new A2UiGenerationContextFactory(List.of(
                new DomainContributor(),
                new CoreCatalogContributor(registry)));

        A2UiGenerationContext context = mixedOrder.build(request(USER_CONTENT, CONTEXT_HINTS, null));

        assertThat(context.staticPrefix().indexOf("required:"))
                .isLessThan(context.staticPrefix().indexOf("DOMAIN:"));
    }

    @Test
    void actionContributorPutsRegisteredActionsOnlyInDynamicSuffix() {
        A2UiActionAllowList allowList = A2UiActionAllowList.fromHandlers(List.of(
                new NamedActionHandler(Set.of("submit_change", "approve"))));
        A2UiGenerationContextFactory withActions = new A2UiGenerationContextFactory(List.of(
                new CoreCatalogContributor(registry),
                new ActionContributor(allowList)));

        A2UiGenerationContext context = withActions.build(request(USER_CONTENT, CONTEXT_HINTS, null));

        assertThat(context.dynamicSuffix()).contains("Registered actions:");
        assertThat(context.dynamicSuffix()).contains("submit_change");
        assertThat(context.dynamicSuffix()).contains(USER_CONTENT);
        assertThat(context.dynamicSuffix().indexOf("Registered actions:"))
                .isLessThan(context.dynamicSuffix().indexOf(USER_CONTENT));
        assertThat(context.staticPrefix()).doesNotContain("Registered actions:");
    }

    @Test
    void policyContributorPutsHiddenTypesOnlyInDynamicSuffix() {
        A2UiSurfacePolicy hideBalance = () -> Set.of("AccountBalance");
        A2UiGenerationContextFactory withPolicy = new A2UiGenerationContextFactory(List.of(
                new CoreCatalogContributor(registry),
                new PolicyContributor(hideBalance)));

        A2UiGenerationContext context = withPolicy.build(request(USER_CONTENT, CONTEXT_HINTS, null));

        assertThat(context.dynamicSuffix()).contains("Hidden component types:");
        assertThat(context.dynamicSuffix()).contains("AccountBalance");
        assertThat(context.dynamicSuffix()).contains("Do not emit these component types.");
        assertThat(context.staticPrefix()).doesNotContain("Hidden component types:");
        assertThat(context.staticPrefix()).doesNotContain("AccountBalance");
    }

    @Test
    void policyContributorAddsNothingWhenHiddenTypesEmpty() {
        A2UiGenerationContextFactory withPolicy = new A2UiGenerationContextFactory(List.of(
                new CoreCatalogContributor(registry),
                new PolicyContributor(A2UiSurfacePolicy.none())));

        A2UiGenerationContext context = withPolicy.build(request(USER_CONTENT, CONTEXT_HINTS, null));

        assertThat(context.dynamicSuffix()).doesNotContain("Hidden component types:");
        assertThat(context.staticPrefix()).doesNotContain("Hidden component types:");
    }

    private static A2UiGenerationRequest request(String content, String hints, Set<String> allowedTypes) {
        return new A2UiGenerationRequest(
                A2UiCatalogIds.BASIC_V0_9,
                allowedTypes,
                content,
                hints,
                List.of(),
                null,
                "dynamic");
    }

    private static final class DomainContributor implements A2UiGenerationContextContributor {
        @Override
        public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
            context.appendStatic("DOMAIN:");
        }
    }

    private static final class NamedActionHandler implements A2UiActionHandler {
        private final Set<String> names;

        private NamedActionHandler(Set<String> names) {
            this.names = names;
        }

        @Override
        public Set<String> actionNames() {
            return names;
        }

        @Override
        public boolean supports(A2UiUserAction userAction) {
            return false;
        }

        @Override
        public List<A2UiMessage> handle(A2UiUserAction userAction, String requestId) {
            return List.of();
        }
    }
}
