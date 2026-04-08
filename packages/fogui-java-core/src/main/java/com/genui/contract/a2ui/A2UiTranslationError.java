package com.genui.contract.a2ui;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class A2UiTranslationError {
    private String path;
    private String code;
    private String category;
    private String message;
    private Map<String, Object> details;
}
