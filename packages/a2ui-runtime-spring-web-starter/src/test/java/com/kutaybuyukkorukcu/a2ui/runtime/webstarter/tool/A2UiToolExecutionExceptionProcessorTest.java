package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiToolExecutionExceptionProcessorTest {

    private final ToolExecutionExceptionProcessor processor = DefaultToolExecutionExceptionProcessor.builder()
            .rethrowExceptions(List.of(SurfaceExecutionException.class))
            .alwaysThrow(true)
            .build();

    @Test
    void shouldUnwrapAndRethrowSurfaceExecutionException() {
        SurfaceExecutionException cause = new SurfaceExecutionException(
                "A2UI validation failed",
                SurfaceErrorCodes.A2UI_VALIDATION_FAILED,
                List.of());
        ToolExecutionException wrapped = new ToolExecutionException(
                ToolDefinition.builder().name("generateA2Ui").description("test").inputSchema("{}").build(),
                cause);

        assertThatThrownBy(() -> processor.process(wrapped))
                .isSameAs(cause);
    }

    @Test
    void shouldRethrowOtherToolFailures() {
        IllegalStateException cause = new IllegalStateException("Planner did not produce a rendered surface");
        ToolExecutionException wrapped = new ToolExecutionException(
                ToolDefinition.builder().name("generateA2Ui").description("test").inputSchema("{}").build(),
                cause);

        assertThatThrownBy(() -> processor.process(wrapped))
                .isSameAs(wrapped);
    }
}
