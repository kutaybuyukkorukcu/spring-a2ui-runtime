package com.genui.contract;

import com.genui.model.genui.GenerativeUIResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Canonical FogUI contract constants and helpers.
 */
public final class FogUiCanonicalContract {

    public static final String CURRENT_CONTRACT_VERSION = "fogui/1.0";
    public static final String METADATA_CONTRACT_VERSION_KEY = "contractVersion";

    private FogUiCanonicalContract() {
    }

    /**
     * Ensures metadata contains the canonical contract version while preserving
     * existing metadata keys.
     */
    public static GenerativeUIResponse ensureContractVersionMetadata(GenerativeUIResponse response) {
        if (response == null) {
            return null;
        }

        Map<String, Object> merged = new HashMap<>();
        if (response.getMetadata() != null) {
            merged.putAll(response.getMetadata());
        }
        merged.put(METADATA_CONTRACT_VERSION_KEY, CURRENT_CONTRACT_VERSION);
        response.setMetadata(merged);
        return response;
    }

    public static String readContractVersion(GenerativeUIResponse response) {
        if (response == null || response.getMetadata() == null) {
            return null;
        }

        Object raw = response.getMetadata().get(METADATA_CONTRACT_VERSION_KEY);
        return raw == null ? null : String.valueOf(raw);
    }
}
