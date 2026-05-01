package com.fogui.contract.a2ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.ThinkingItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class A2UiStreamMessage {

    private String type;
    private List<ThinkingItem> thinking;
    private List<ContentBlock> content;
    private Map<String, Object> usage;
    private String error;
    private String code;
    private Object details;
    private String requestId;
}