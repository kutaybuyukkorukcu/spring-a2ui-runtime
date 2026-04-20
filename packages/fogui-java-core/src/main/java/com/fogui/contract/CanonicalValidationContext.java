package com.fogui.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional contextual inputs for canonical validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalValidationContext {
    private String expectedContractVersion;

    public static CanonicalValidationContext empty() {
        return CanonicalValidationContext.builder().build();
    }
}
