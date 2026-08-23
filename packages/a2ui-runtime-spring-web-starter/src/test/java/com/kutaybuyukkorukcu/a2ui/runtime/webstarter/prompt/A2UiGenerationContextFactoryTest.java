package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionAllowList;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
    void pruneButtonOnlyAndFullCatalogProduceDifferentKeys() {
        A2UiGenerationRequest base = request(USER_CONTENT, CONTEXT_HINTS, null);
        A2UiGenerationContext full = factory.build(base);
        A2UiGenerationContext pruned = factory.build(request(USER_CONTENT, CONTEXT_HINTS, Set.of("Button")));

        assertThat(full.key()).isNotEqualTo(pruned.key());
        assertThat(full.key().allowedTypesFingerprint()).isEmpty();
        assertThat(pruned.key().allowedTypesFingerprint()).isEqualTo("Button");
    }

    @Test
    void differentContributorOrderProducesDifferentFingerprints() {
        A2UiGenerationContextFactory firstSecond = new A2UiGenerationContextFactory(
                List.of(new FirstContributor(1), new SecondContributor(2)));
        A2UiGenerationContextFactory secondFirst = new A2UiGenerationContextFactory(
                List.of(new FirstContributor(2), new SecondContributor(1)));

        A2UiGenerationRequest req = request(USER_CONTENT, CONTEXT_HINTS, null);
        assertThat(firstSecond.build(req).key().contributorFingerprint())
                .isEqualTo("FirstContributor,SecondContributor");
        assertThat(secondFirst.build(req).key().contributorFingerprint())
                .isEqualTo("SecondContributor,FirstContributor");
        assertThat(firstSecond.build(req).key()).isNotEqualTo(secondFirst.build(req).key());
    }

    @Test
    void additionalStaticContributionChangesCatalogVersion() {
        A2UiGenerationContextFactory baseline = new A2UiGenerationContextFactory(
                List.of(new CoreCatalogContributor(registry)));
        A2UiGenerationContextFactory withExtra = new A2UiGenerationContextFactory(
                List.of(new CoreCatalogContributor(registry), new DomainContributor()));

        A2UiGenerationRequest req = request(USER_CONTENT, CONTEXT_HINTS, null);
        A2UiGenerationContextKey baselineKey = baseline.build(req).key();
        A2UiGenerationContextKey withExtraKey = withExtra.build(req).key();

        assertThat(baselineKey.catalogVersion()).isNotEqualTo(baselineKey.catalogId());
        assertThat(baselineKey.catalogVersion()).isNotEqualTo(withExtraKey.catalogVersion());
    }

    @Test
    void sameRequestProducesEqualKeys() {
        A2UiGenerationRequest req = request(USER_CONTENT, CONTEXT_HINTS, null);

        assertThat(factory.build(req).key()).isEqualTo(factory.build(req).key());
    }

    @Test
    void secondBuildWithSameLookupSkipsStaticContributors() {
        CountingStaticContributor staticContributor = new CountingStaticContributor();
        CountingDynamicContributor dynamicContributor = new CountingDynamicContributor();
        A2UiGenerationContextFactory caching = new A2UiGenerationContextFactory(List.of(
                staticContributor,
                dynamicContributor));

        A2UiGenerationContext first = caching.build(request(USER_CONTENT, CONTEXT_HINTS, null));
        A2UiGenerationContext second = caching.build(request(USER_CONTENT, CONTEXT_HINTS, null));

        assertThat(staticContributor.applied.get()).isEqualTo(1);
        assertThat(dynamicContributor.applied.get()).isEqualTo(2);
        assertThat(second.staticPrefix()).isEqualTo(first.staticPrefix()).contains("COUNTED");
        assertThat(second.dynamicSuffix()).contains("DYNAMIC").contains(USER_CONTENT);
        assertThat(second.key()).isEqualTo(first.key());
    }

    @Test
    void differentAllowedTypesMissTheStaticCache() {
        CountingStaticContributor staticContributor = new CountingStaticContributor();
        A2UiGenerationContextFactory caching = new A2UiGenerationContextFactory(List.of(staticContributor));

        caching.build(request(USER_CONTENT, CONTEXT_HINTS, null));
        caching.build(request(USER_CONTENT, CONTEXT_HINTS, Set.of("Button")));

        assertThat(staticContributor.applied.get()).isEqualTo(2);
    }

    @Test
    void differentUserContentReusesStaticPrefix() {
        CountingStaticContributor staticContributor = new CountingStaticContributor();
        A2UiGenerationContextFactory caching = new A2UiGenerationContextFactory(List.of(staticContributor));

        A2UiGenerationContext first = caching.build(request(USER_CONTENT, CONTEXT_HINTS, null));
        A2UiGenerationContext second = caching.build(request("another prompt", CONTEXT_HINTS, null));

        assertThat(staticContributor.applied.get()).isEqualTo(1);
        assertThat(second.staticPrefix()).isEqualTo(first.staticPrefix());
        assertThat(second.dynamicSuffix()).contains("another prompt");
        assertThat(second.dynamicSuffix()).doesNotContain(USER_CONTENT);
        assertThat(first.dynamicSuffix()).contains(USER_CONTENT);
    }

    @Test
    void cacheHitStillAppendsActionAllowListToDynamicSuffix() {
        A2UiActionAllowList allowList = A2UiActionAllowList.fromHandlers(List.of(
                new NamedActionHandler(Set.of("submit_change"))));
        A2UiGenerationContextFactory caching = new A2UiGenerationContextFactory(List.of(
                new CoreCatalogContributor(registry),
                new ActionContributor(allowList)));

        caching.build(request(USER_CONTENT, CONTEXT_HINTS, null));
        A2UiGenerationContext second = caching.build(request("second prompt", CONTEXT_HINTS, null));

        assertThat(second.dynamicSuffix()).contains("Registered actions:");
        assertThat(second.dynamicSuffix()).contains("submit_change");
        assertThat(second.staticPrefix()).doesNotContain("Registered actions:");
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

    private static final class CountingStaticContributor implements A2UiGenerationContextContributor {
        private final AtomicInteger applied = new AtomicInteger();

        @Override
        public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
            applied.incrementAndGet();
            context.appendStatic("COUNTED");
        }
    }

    private static final class CountingDynamicContributor implements A2UiGenerationContextContributor {
        private final AtomicInteger applied = new AtomicInteger();

        @Override
        public boolean contributesStatic() {
            return false;
        }

        @Override
        public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
            applied.incrementAndGet();
            context.appendDynamic("DYNAMIC\n\n");
        }
    }

    private static final class DomainContributor implements A2UiGenerationContextContributor {
        @Override
        public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
            context.appendStatic("DOMAIN:");
        }
    }

    private static final class FirstContributor implements A2UiGenerationContextContributor, Ordered {
        private final int order;

        private FirstContributor(int order) {
            this.order = order;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
            context.appendStatic("FIRST");
        }
    }

    private static final class SecondContributor implements A2UiGenerationContextContributor, Ordered {
        private final int order;

        private SecondContributor(int order) {
            this.order = order;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
            context.appendStatic("SECOND");
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
