package com.fogui.contract.a2ui;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class A2UiValidationContext {

    private String requestedVersion;

    public static A2UiValidationContext empty() {
        return new A2UiValidationContext();
    }

    public static A2UiValidationContext forVersion(String requestedVersion) {
        return A2UiValidationContext.builder()
                .requestedVersion(requestedVersion)
                .build();
    }
}