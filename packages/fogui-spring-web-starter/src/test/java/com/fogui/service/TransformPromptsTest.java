package com.fogui.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fogui.webstarter.prompt.TransformPromptContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TransformPrompts")
class TransformPromptsTest {

  @Nested
  @DisplayName("buildTransformPrompt")
  class BuildTransformPrompt {

    @Test
    @DisplayName("should include content wrapped in delimiters")
    void shouldIncludeContentWrappedInDelimiters() {
      String content = "Some user content here";

      String prompt = TransformPrompts.buildTransformPrompt(content, null);

      assertTrue(prompt.contains("---"));
      assertTrue(prompt.contains(content));
    }

    @Test
    @DisplayName("should include context hints when provided")
    void shouldIncludeContextHintsWhenProvided() {
      String content = "User content";
      String contextHints = "This is for a dashboard";

      String prompt = TransformPrompts.buildTransformPrompt(content, contextHints);

      assertTrue(prompt.contains("Additional context:"));
      assertTrue(prompt.contains(contextHints));
    }

    @Test
    @DisplayName("should keep prompt sections in deterministic order")
    void shouldKeepPromptSectionsInDeterministicOrder() {
      String content = "User content";
      String contextHints = "This is for a dashboard";

      String prompt = TransformPrompts.buildTransformPrompt(content, contextHints);

      int contentIndex = prompt.indexOf("---\n" + content + "\n---");
      int contextIndex = prompt.indexOf("Additional context: " + contextHints);
        int reminderIndex = prompt.indexOf("Runtime canonical response rules for A2UI v0.8:");
      int instructionIndex =
          prompt.indexOf("Respond with the JSON structure only. Do not add prose or code fences.");

      assertTrue(contentIndex >= 0);
      assertTrue(contextIndex > contentIndex);
      assertTrue(reminderIndex > contextIndex);
      assertTrue(instructionIndex > reminderIndex);
    }

    @Test
    @DisplayName("should not include context section when hints are null")
    void shouldNotIncludeContextSectionWhenHintsNull() {
      String content = "User content";

      String prompt = TransformPrompts.buildTransformPrompt(content, null);

      assertFalse(prompt.contains("Additional context:"));
    }

    @Test
    @DisplayName("should not include context section when hints are empty")
    void shouldNotIncludeContextSectionWhenHintsEmpty() {
      String content = "User content";

      String prompt = TransformPrompts.buildTransformPrompt(content, "");

      assertFalse(prompt.contains("Additional context:"));
    }

    @Test
    @DisplayName("should end with JSON instruction")
    void shouldEndWithJsonInstruction() {
      String content = "User content";

      String prompt = TransformPrompts.buildTransformPrompt(content, null);

      assertTrue(
          prompt.endsWith(
              "Respond with the JSON structure only. Do not add prose or code fences."));
    }

    @Test
    @DisplayName("should start with transform instruction")
    void shouldStartWithTransformInstruction() {
      String content = "User content";

      String prompt = TransformPrompts.buildTransformPrompt(content, null);

      assertTrue(
        prompt.startsWith(
          "Transform the following content into the runtime's canonical JSON for A2UI v0.8:"));
    }

    @Test
    @DisplayName("should include selected catalog context when provided")
    void shouldIncludeSelectedCatalogContextWhenProvided() {
      String prompt =
        TransformPrompts.buildTransformPrompt(
          new TransformPromptContext(
            "User content",
            "This is for a dashboard",
            "/a2ui/catalogs/canonical/v0.8",
            List.of("/a2ui/catalogs/canonical/v0.8")));

      assertTrue(
        prompt.contains(
          "Selected A2UI catalog: /a2ui/catalogs/canonical/v0.8. Use only componentType values supported by this catalog."));
      assertTrue(
        prompt.contains(
          "Client-supported A2UI catalogs: /a2ui/catalogs/canonical/v0.8"));
    }

    @Test
    @DisplayName("should reinforce canonical type rules in the user prompt")
    void shouldReinforceCanonicalTypeRulesInTheUserPrompt() {
      String content = "Summarize the Q1 report";

      String prompt = TransformPrompts.buildTransformPrompt(content, null);

      assertTrue(prompt.contains("The only valid `type` values are \"text\" and \"component\"."));
      assertTrue(
          prompt.contains(
              "Never return values like \"card\", \"list\", or \"table\" in the top-level \"type\" field."));
      assertTrue(prompt.contains("\"componentType\":\"Card\""));
    }

    @Test
    @DisplayName("should handle multiline content")
    void shouldHandleMultilineContent() {
      String content = "Line 1\nLine 2\nLine 3";

      String prompt = TransformPrompts.buildTransformPrompt(content, null);

      assertTrue(prompt.contains("Line 1"));
      assertTrue(prompt.contains("Line 2"));
      assertTrue(prompt.contains("Line 3"));
    }

    @Test
    @DisplayName("should handle special characters in content")
    void shouldHandleSpecialCharactersInContent() {
      String content = "Price: $100 & 50% off! {data: \"value\"}";

      String prompt = TransformPrompts.buildTransformPrompt(content, null);

      assertTrue(prompt.contains(content));
    }
  }

  @Nested
  @DisplayName("TRANSFORM_SYSTEM_PROMPT")
  class TransformSystemPrompt {

    @Test
    @DisplayName("should not be null or empty")
    void shouldNotBeNullOrEmpty() {
      assertNotNull(TransformPrompts.TRANSFORM_SYSTEM_PROMPT);
      assertFalse(TransformPrompts.TRANSFORM_SYSTEM_PROMPT.isEmpty());
    }

    @Test
    @DisplayName("should contain canonical component guidance")
    void shouldContainCanonicalComponentGuidance() {
      String prompt = TransformPrompts.TRANSFORM_SYSTEM_PROMPT;

      assertTrue(prompt.contains("Spring A2UI runtime"));
      assertTrue(prompt.contains("Card"));
      assertTrue(prompt.contains("List"));
      assertTrue(prompt.contains("Table"));
      assertTrue(prompt.contains("Container"));
      assertTrue(prompt.contains("Chart"));
    }

    @Test
    @DisplayName("should contain output format instructions")
    void shouldContainJsonOutputFormatInstructions() {
      String prompt = TransformPrompts.TRANSFORM_SYSTEM_PROMPT;

      assertTrue(prompt.contains("component") || prompt.contains("Component"));
    }

    @Test
    @DisplayName("should include canonical wire shape examples")
    void shouldIncludeDeterministicContractRequirements() {
      String prompt = TransformPrompts.TRANSFORM_SYSTEM_PROMPT;

      assertTrue(
          prompt.contains(
              "the runtime's canonical JSON response, which will be validated and mapped to A2UI v0.8 messages"));
      assertTrue(prompt.contains("The only valid `type` values are \"text\" and \"component\"."));
      assertTrue(prompt.contains("\"type\": \"component\""));
      assertTrue(prompt.contains("\"componentType\": \"Card\""));
      assertTrue(
          prompt.contains(
              "Never return values like \"card\", \"list\", or \"table\" in the top-level \"type\" field."));
    }
  }
}
