package com.fogui.contract.a2ui;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class A2UiValidationError {

    private String path;
    private String code;
    private String category;
    private String message;
    private Map<String, Object> details;
}