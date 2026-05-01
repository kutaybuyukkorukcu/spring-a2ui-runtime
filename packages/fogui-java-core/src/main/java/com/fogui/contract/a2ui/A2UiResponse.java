package com.fogui.contract.a2ui;

import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.ThinkingItem;
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
public class A2UiResponse {

    @Builder.Default
    private List<ThinkingItem> thinking = new ArrayList<>();

    @Builder.Default
    private List<ContentBlock> content = new ArrayList<>();
}