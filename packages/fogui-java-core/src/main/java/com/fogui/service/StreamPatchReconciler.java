package com.fogui.service;

import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import com.fogui.model.fogui.ThinkingItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reconciles partial streaming snapshots into a stable canonical response.
 */
public class StreamPatchReconciler {

    public GenerativeUIResponse reconcile(GenerativeUIResponse previous, GenerativeUIResponse incoming) {
        if (incoming == null) {
            return previous;
        }

        if (previous == null) {
            return normalize(incoming);
        }

        GenerativeUIResponse merged = GenerativeUIResponse.builder().build();
        merged.setThinking(chooseThinking(previous.getThinking(), incoming.getThinking()));
        merged.setContent(chooseContent(previous.getContent(), incoming.getContent()));
        merged.setMetadata(chooseMetadata(previous.getMetadata(), incoming.getMetadata()));
        return merged;
    }

    private List<ThinkingItem> chooseThinking(List<ThinkingItem> previous, List<ThinkingItem> incoming) {
        if (incoming != null && !incoming.isEmpty()) {
            return new ArrayList<>(incoming);
        }
        if (previous != null) {
            return new ArrayList<>(previous);
        }
        return new ArrayList<>();
    }

    private List<ContentBlock> chooseContent(List<ContentBlock> previous, List<ContentBlock> incoming) {
        if (incoming != null && !incoming.isEmpty()) {
            return new ArrayList<>(incoming);
        }
        if (previous != null) {
            return new ArrayList<>(previous);
        }
        return new ArrayList<>();
    }

    private Map<String, Object> chooseMetadata(Map<String, Object> previous, Map<String, Object> incoming) {
        boolean hasPrevious = previous != null && !previous.isEmpty();
        boolean hasIncoming = incoming != null && !incoming.isEmpty();

        if (!hasPrevious && !hasIncoming) {
            return null;
        }

        Map<String, Object> merged = new HashMap<>();

        if (hasPrevious) {
            merged.putAll(previous);
        }
        if (hasIncoming) {
            merged.putAll(incoming);
        }
        return merged;
    }

    private GenerativeUIResponse normalize(GenerativeUIResponse source) {
        GenerativeUIResponse normalized = GenerativeUIResponse.builder().build();
        normalized.setThinking(source.getThinking() == null ? new ArrayList<>() : new ArrayList<>(source.getThinking()));
        normalized.setContent(source.getContent() == null ? new ArrayList<>() : new ArrayList<>(source.getContent()));
        normalized.setMetadata(source.getMetadata() == null ? null : new HashMap<>(source.getMetadata()));
        return normalized;
    }
}
