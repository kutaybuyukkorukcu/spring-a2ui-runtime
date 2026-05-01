package com.fogui.contract.a2ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class A2UiUserAction {

    private String name;
    private String surfaceId;
    private String sourceComponentId;
    private String timestamp;

    @Builder.Default
    private Map<String, Object> context = new LinkedHashMap<>();
}