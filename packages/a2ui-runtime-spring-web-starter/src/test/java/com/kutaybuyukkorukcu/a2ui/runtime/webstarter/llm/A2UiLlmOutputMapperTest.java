package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiLlmOutputMapperTest {

    private A2UiLlmOutputMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new A2UiLlmOutputMapper(new ObjectMapper());
    }

    @Test
        void shouldMapAllMessageTypesFromStructuredDto() throws Exception {
                String json = """
                                {
                                    "messages": [
                                        {
                                            "surfaceUpdate": {
                                                "surfaceId": "main",
                                                "components": [
                                                    {
                                                        "id": "c1",
                                                        "component": {
                                                            "Text": {
                                                                "text": { "literalString": "Hello" }
                                                            }
                                                        }
                                                    }
                                                ]
                                            }
                                        },
                                        {
                                            "dataModelUpdate": {
                                                "surfaceId": "main",
                                                "path": "/weather",
                                                "contents": [
                                                    { "key": "description", "valueString": "Sunny" }
                                                ]
                                            }
                                        },
                                        {
                                            "beginRendering": {
                                                "surfaceId": "main",
                                                "root": "c1"
                                            }
                                        },
                                        {
                                            "deleteSurface": {
                                                "surfaceId": "legacy"
                                            }
                                        }
                                    ]
                                }
                                """;

                A2UiLlmOutput output = new ObjectMapper().readValue(json, A2UiLlmOutput.class);
                List<A2UiMessage> messages = mapper.map(output);

                assertThat(messages).hasSize(4);

                A2UiMessage.SurfaceUpdate surfaceUpdate = (A2UiMessage.SurfaceUpdate) messages.get(0);
                assertThat(surfaceUpdate.surfaceId()).isEqualTo("main");
                assertThat(surfaceUpdate.components()).hasSize(1);
                assertThat(surfaceUpdate.components().get(0).component()).containsKey("Text");

                A2UiMessage.DataModelUpdate dataModelUpdate = (A2UiMessage.DataModelUpdate) messages.get(1);
                assertThat(dataModelUpdate.surfaceId()).isEqualTo("main");
                assertThat(dataModelUpdate.path()).isEqualTo("/weather");
                assertThat(dataModelUpdate.contents()).hasSize(1);
                assertThat(dataModelUpdate.contents().get(0).key()).isEqualTo("description");
                assertThat(dataModelUpdate.contents().get(0).valueString()).isEqualTo("Sunny");

                A2UiMessage.BeginRendering beginRendering = (A2UiMessage.BeginRendering) messages.get(2);
                assertThat(beginRendering.surfaceId()).isEqualTo("main");
                assertThat(beginRendering.root()).isEqualTo("c1");

                A2UiMessage.DeleteSurface deleteSurface = (A2UiMessage.DeleteSurface) messages.get(3);
                assertThat(deleteSurface.surfaceId()).isEqualTo("legacy");
    }

    @Test
        void shouldReturnEmptyListForNullOutput() {
                assertThat(mapper.map(null)).isEmpty();
    }

    @Test
    void shouldNormalizeMalformedDataEntryWithMultipleValueFields() throws Exception {
        String json = """
                {
                    "messages": [
                        {
                            "dataModelUpdate": {
                                "surfaceId": "main",
                                "path": "/weather",
                                "contents": [
                                    {
                                        "key": "title",
                                        "valueBoolean": false,
                                        "valueNumber": 0,
                                        "valueString": "Weather"
                                    },
                                    {
                                        "key": "details",
                                        "valueMap": [
                                            {
                                                "key": "summary",
                                                "valueString": "Sunny"
                                            }
                                        ],
                                        "valueString": "ignored"
                                    }
                                ]
                            }
                        }
                    ]
                }
                """;

        A2UiLlmOutput output = new ObjectMapper().readValue(json, A2UiLlmOutput.class);
        List<A2UiMessage> messages = mapper.map(output);

        assertThat(messages).hasSize(1);
        A2UiMessage.DataModelUpdate update = (A2UiMessage.DataModelUpdate) messages.get(0);
        assertThat(update.contents()).hasSize(2);
        assertThat(update.contents().get(0).valueString()).isEqualTo("Weather");
        assertThat(update.contents().get(0).valueNumber()).isNull();
        assertThat(update.contents().get(0).valueBoolean()).isNull();
        assertThat(update.contents().get(1).valueMap()).isNotNull();
        assertThat(update.contents().get(1).valueMap()).hasSize(1);
        assertThat(update.contents().get(1).valueMap().get(0).key()).isEqualTo("summary");
    }

    @Test
    void shouldRejectMessageItemWithMultipleEnvelopes() throws Exception {
        String json = """
                {
                    "messages": [
                        {
                            "surfaceUpdate": {
                                "surfaceId": "main",
                                "components": []
                            },
                            "beginRendering": {
                                "surfaceId": "main",
                                "root": "c1"
                            }
                        }
                    ]
                }
                """;

        A2UiLlmOutput output = new ObjectMapper().readValue(json, A2UiLlmOutput.class);

        assertThatThrownBy(() -> mapper.map(output))
                .isInstanceOf(A2UiLlmMappingException.class)
                .hasMessageContaining("exactly one envelope")
                .satisfies(ex -> {
                    A2UiLlmMappingException mappingException = (A2UiLlmMappingException) ex;
                    assertThat(mappingException.getMessageItemIndex()).isEqualTo(0);
                    assertThat(mappingException.getReason()).isEqualTo("multiple_envelopes");
                });
    }

    @Test
    void shouldSkipMessageItemWithoutEnvelope() throws Exception {
        String json = """
                {
                    "messages": [
                        {
                        }
                    ]
                }
                """;

        A2UiLlmOutput output = new ObjectMapper().readValue(json, A2UiLlmOutput.class);
        List<A2UiMessage> messages = mapper.map(output);

        assertThat(messages).isEmpty();
    }

        @Test
        void shouldMapBeginRenderingCatalogIdAndMultipleChoiceVariantAlias() throws Exception {
                String json = """
                                {
                                    "messages": [
                                        {
                                            "surfaceUpdate": {
                                                "surfaceId": "main",
                                                "components": [
                                                    {
                                                        "id": "choice-1",
                                                        "component": {
                                                            "MultipleChoice": {
                                                                "selections": { "path": "/filters/country" },
                                                                "options": [
                                                                    { "label": { "literalString": "TR" }, "value": "tr" }
                                                                ],
                                                                "type": "chips"
                                                            }
                                                        }
                                                    }
                                                ]
                                            }
                                        },
                                        {
                                            "beginRendering": {
                                                "surfaceId": "main",
                                                "root": "choice-1",
                                                "catalogId": "https://a2ui.org/specification/v0_8/standard_catalog_definition.json"
                                            }
                                        }
                                    ]
                                }
                                """;

                A2UiLlmOutput output = new ObjectMapper().readValue(json, A2UiLlmOutput.class);
                List<A2UiMessage> messages = mapper.map(output);

                assertThat(messages).hasSize(2);

                A2UiMessage.SurfaceUpdate surfaceUpdate = (A2UiMessage.SurfaceUpdate) messages.get(0);
                @SuppressWarnings("unchecked")
                var multipleChoice = (java.util.Map<String, Object>) surfaceUpdate.components().get(0).component().get("MultipleChoice");
                assertThat(multipleChoice).containsKey("variant");
                assertThat(multipleChoice.get("variant")).isEqualTo("chips");

                A2UiMessage.BeginRendering beginRendering = (A2UiMessage.BeginRendering) messages.get(1);
                assertThat(beginRendering.catalogId()).isEqualTo("https://a2ui.org/specification/v0_8/standard_catalog_definition.json");
        }
}
