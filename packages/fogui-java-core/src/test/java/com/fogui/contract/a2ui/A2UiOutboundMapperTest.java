package com.fogui.contract.a2ui;

import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class A2UiOutboundMapperTest {

    private final A2UiOutboundMapper mapper = new A2UiOutboundMapper();

    @Test
    void shouldMapCanonicalResponseToA2UiV08Messages() {
        GenerativeUIResponse response = GenerativeUIResponse.builder()
                .content(List.of(ContentBlock.text("hello from a2ui")))
                .build();

        List<A2UiMessage> messages = mapper.toMessages(response);

        assertEquals(2, messages.size());
        assertNotNull(messages.get(0).getSurfaceUpdate());
        assertEquals(A2UiOutboundMapper.DEFAULT_SURFACE_ID, messages.get(0).getSurfaceUpdate().getSurfaceId());
        assertEquals(A2UiOutboundMapper.DEFAULT_ROOT_COMPONENT_ID, messages.get(1).getBeginRendering().getRoot());
        assertEquals(A2UiOutboundMapper.DEFAULT_CATALOG_ID, messages.get(1).getBeginRendering().getCatalogId());

        Map<String, Object> rootComponent = messages.get(0).getSurfaceUpdate().getComponents().stream()
                .filter(component -> A2UiOutboundMapper.DEFAULT_ROOT_COMPONENT_ID.equals(component.getId()))
                .findFirst()
                .orElseThrow()
                .getComponent();
        assertTrue(rootComponent.containsKey("Column"));

        Map<String, Object> textComponent = messages.get(0).getSurfaceUpdate().getComponents().stream()
                .filter(component -> "content-0".equals(component.getId()))
                .findFirst()
                .orElseThrow()
                .getComponent();
        assertEquals(
                Map.of("literalString", "hello from a2ui"),
                ((Map<?, ?>) textComponent.get("Text")).get("text"));
    }

    @Test
    void shouldFlattenNestedCanonicalChildrenIntoAdjacencyList() {
        ContentBlock nestedCard = ContentBlock.component("Card", Map.of("title", "Summary"));
        nestedCard.setChildren(List.of(ContentBlock.text("Revenue grew")));

        GenerativeUIResponse response = GenerativeUIResponse.builder()
                .content(List.of(nestedCard))
                .build();

        List<A2UiMessage> messages = mapper.toMessages(response, "booking", false);

        assertEquals(1, messages.size());
        A2UiMessage.SurfaceUpdate surfaceUpdate = messages.getFirst().getSurfaceUpdate();
        assertEquals("booking", surfaceUpdate.getSurfaceId());

        Map<String, Object> cardComponent = surfaceUpdate.getComponents().stream()
                .filter(component -> "content-0".equals(component.getId()))
                .findFirst()
                .orElseThrow()
                .getComponent();

        Map<?, ?> cardProps = (Map<?, ?>) cardComponent.get("Card");
        assertEquals("content-0-child-0", cardProps.get("child"));
        assertEquals("Summary", cardProps.get("title"));
    }

        @Test
        void shouldUseCallerSelectedCatalogIdWhenProvided() {
                GenerativeUIResponse response = GenerativeUIResponse.builder()
                                .content(List.of(ContentBlock.text("hello from a2ui")))
                                .build();

                List<A2UiMessage> messages = mapper.toMessages(response, A2UiCatalogIds.CANONICAL_V0_8);

                assertEquals(A2UiCatalogIds.CANONICAL_V0_8, messages.get(1).getBeginRendering().getCatalogId());
        }
}