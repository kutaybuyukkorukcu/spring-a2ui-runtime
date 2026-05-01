package com.fogui.contract.a2ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class A2UiClientError {

    private String code;
    private String surfaceId;
    private String path;
    private String message;
    private Object details;
}