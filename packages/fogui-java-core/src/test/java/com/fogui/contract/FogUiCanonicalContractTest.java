package com.fogui.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FogUiCanonicalContractTest {

  @Test
  void shouldStampContractVersionIntoMetadata() {
    GenerativeUIResponse response =
        GenerativeUIResponse.builder()
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
