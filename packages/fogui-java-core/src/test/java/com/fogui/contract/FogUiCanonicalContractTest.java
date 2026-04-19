package com.fogui.contract;

import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FogUiCanonicalContractTest {

    @Test
    void shouldStampContractVersionIntoMetadata() {
        GenerativeUIResponse response = GenerativeUIResponse.builder()
                .thinking(List.of())
                .content(List.of(ContentBlock.text("hello")))
                .metadata(Map.of("sourceProtocol", "transform"))
                .build();

        FogUiCanonicalContract.ensureContractVersionMetadata(response);

        assertNotNull(response.getMetadata());
        assertEquals(
                FogUiCanonicalContract.CURRENT_CONTRACT_VERSION,
                response.getMetadata().get(FogUiCanonicalContract.METADATA_CONTRACT_VERSION_KEY));
        assertEquals("transform", response.getMetadata().get("sourceProtocol"));
    }
}
