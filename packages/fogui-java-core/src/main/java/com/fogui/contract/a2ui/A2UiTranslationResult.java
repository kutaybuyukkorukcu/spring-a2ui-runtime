package com.fogui.contract.a2ui;

import com.fogui.model.fogui.GenerativeUIResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class A2UiTranslationResult {
    @Builder.Default
    private GenerativeUIResponse response = new GenerativeUIResponse();

    @Builder.Default
    private List<A2UiTranslationError> errors = new ArrayList<>();
}
