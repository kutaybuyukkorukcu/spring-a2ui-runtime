package com.fogui.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.model.fogui.GenerativeUIResponse;

import java.util.Map;

/**
 * Minimal outbound mapper from canonical contract to renderer-consumable map shape.
 * Current v1 behavior is intentionally identity-preserving.
 */
public class CanonicalOutboundMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenerativeUIResponse toRendererContract(GenerativeUIResponse response) {
        return response;
    }

    public Map<String, Object> toRendererPayload(GenerativeUIResponse response) {
        return objectMapper.convertValue(response, Map.class);
    }
}
