package com.fogui.contract.a2ui;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class A2UiActionResponse {

    private boolean accepted;
    private String eventType;
    private String requestId;
    private String routeKey;
    private String actionName;
    private String surfaceId;
    private String sourceComponentId;
    private Integer messageCount;
    private String errorCode;

    @Builder.Default
    private List<A2UiMessage> messages = new ArrayList<>();
}