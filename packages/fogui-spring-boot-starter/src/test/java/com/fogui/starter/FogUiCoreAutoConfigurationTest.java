package com.fogui.starter;

import com.fogui.contract.FogUiCanonicalValidator;
import com.fogui.contract.CanonicalOutboundMapper;
import com.fogui.contract.a2ui.A2UiInboundTranslator;
import com.fogui.starter.advisor.CanonicalValidationAdvisor;
import com.fogui.starter.advisor.DeterministicOptionsAdvisor;
import com.fogui.starter.advisor.FogUiAdvisorOrder;
import com.fogui.starter.advisor.FogUiAdvisorsProperties;
import com.fogui.starter.policy.FogUiGenerationPolicyProperties;
import com.fogui.starter.policy.FogUiGenerationPolicyService;
import com.fogui.service.StreamPatchReconciler;
import com.fogui.service.UIResponseParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FogUiCoreAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(FogUiCoreAutoConfiguration.class));

    @Test
    void shouldRegisterCoreBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(UIResponseParser.class);
            assertThat(context).hasSingleBean(FogUiCanonicalValidator.class);
            assertThat(context).hasSingleBean(CanonicalOutboundMapper.class);
            assertThat(context).hasSingleBean(A2UiInboundTranslator.class);
            assertThat(context).hasSingleBean(StreamPatchReconciler.class);
            assertThat(context).hasSingleBean(FogUiGenerationPolicyProperties.class);
            assertThat(context).hasSingleBean(FogUiGenerationPolicyService.class);
            assertThat(context).hasSingleBean(FogUiAdvisorsProperties.class);
            assertThat(context).hasSingleBean(DeterministicOptionsAdvisor.class);
            assertThat(context).hasSingleBean(CanonicalValidationAdvisor.class);
        });
    }

    @Test
    void shouldBindDeterministicPolicyProperties() {
        contextRunner
                .withPropertyValues(
                        "fogui.deterministic.temperature=0.2",
                        "fogui.deterministic.top-p=0.9",
                        "fogui.deterministic.seed=42",
                        "fogui.deterministic.capabilities.seed=false")
                .run(context -> {
                    FogUiGenerationPolicyService service = context.getBean(FogUiGenerationPolicyService.class);
                    var policy = service.resolve("gpt-4.1-nano");

                    assertThat(policy.getTemperature()).isEqualTo(0.2);
                    assertThat(policy.getTopP()).isEqualTo(0.9);
                    assertThat(policy.getSeed()).isNull();
                    assertThat(policy.getSkippedOptions()).contains("seed");
                });
    }

    @Test
    void shouldDisableAllAdvisorsWhenMasterSwitchIsOff() {
        contextRunner
                .withPropertyValues("fogui.advisors.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DeterministicOptionsAdvisor.class);
                    assertThat(context).doesNotHaveBean(CanonicalValidationAdvisor.class);
                });
    }

    @Test
    void shouldDisableCanonicalValidationAdvisorIndependently() {
        contextRunner
                .withPropertyValues("fogui.advisors.canonical-validation.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(DeterministicOptionsAdvisor.class);
                    assertThat(context).doesNotHaveBean(CanonicalValidationAdvisor.class);
                });
    }

    @Test
    void shouldExposeStableAdvisorOrderConstants() {
        assertThat(FogUiAdvisorOrder.DETERMINISTIC_OPTIONS)
                .isLessThan(FogUiAdvisorOrder.CANONICAL_VALIDATION);
    }
}
