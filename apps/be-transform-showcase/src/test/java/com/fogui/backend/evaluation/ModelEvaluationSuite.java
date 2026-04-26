package com.fogui.backend.evaluation;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dedicated AI evaluation suite for CI quality signaling.
 * <p>
 * Intentionally not named *Test to keep it out of required default test lanes.
 */
@Tag("evaluation")
class ModelEvaluationSuite {

    @Test
    void relevancyEvaluatorShouldPassOnDeterministicYesJudge() {
        RelevancyEvaluator evaluator = new RelevancyEvaluator(
                ChatClient.builder(new FixedJudgeChatModel("yes"))
                        .defaultOptions(deterministicJudgeOptions()));

        EvaluationResponse response = evaluator.evaluate(new EvaluationRequest(
                "What is Paris known for?",
                List.of(new Document("Paris is the capital of France and known for the Eiffel Tower.")),
                "Paris is known for the Eiffel Tower."));

        assertTrue(response.isPass());
    }

    @Test
    void relevancyEvaluatorShouldFailOnDeterministicNoJudge() {
        RelevancyEvaluator evaluator = new RelevancyEvaluator(
                ChatClient.builder(new FixedJudgeChatModel("no"))
                        .defaultOptions(deterministicJudgeOptions()));

        EvaluationResponse response = evaluator.evaluate(new EvaluationRequest(
                "What is Paris known for?",
                List.of(new Document("Paris is the capital of France and known for the Eiffel Tower.")),
                "Bananas are yellow."));

        assertFalse(response.isPass());
    }

    @Test
    void factCheckingEvaluatorShouldPassOnDeterministicYesJudge() {
        FactCheckingEvaluator evaluator = new FactCheckingEvaluator(
                ChatClient.builder(new FixedJudgeChatModel("yes")),
                "Return only yes or no.");

        EvaluationResponse response = evaluator.evaluate(new EvaluationRequest(
                "Tell me one fact about Earth.",
                List.of(new Document("Earth orbits the Sun.")),
                "Earth orbits the Sun."));

        assertTrue(response.isPass());
    }

    @Test
    void factCheckingEvaluatorShouldFailOnDeterministicNoJudge() {
        FactCheckingEvaluator evaluator = new FactCheckingEvaluator(
                ChatClient.builder(new FixedJudgeChatModel("no")),
                "Return only yes or no.");

        EvaluationResponse response = evaluator.evaluate(new EvaluationRequest(
                "Tell me one fact about Earth.",
                List.of(new Document("Earth orbits the Sun.")),
                "Earth has two moons."));

        assertFalse(response.isPass());
    }

    private OpenAiChatOptions deterministicJudgeOptions() {
        OpenAiChatOptions options = OpenAiChatOptions.builder().build();
        options.setTemperature(0.0);
        options.setTopP(1.0);
        return options;
    }

    private static final class FixedJudgeChatModel implements ChatModel {

        private final String answer;

        private FixedJudgeChatModel(String answer) {
            this.answer = answer;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            AssistantMessage assistantMessage = new AssistantMessage(answer);
            return new ChatResponse(List.of(new Generation(assistantMessage)));
        }

        @Override
        public ChatOptions getDefaultOptions() {
            OpenAiChatOptions options = OpenAiChatOptions.builder().build();
            options.setTemperature(0.0);
            options.setTopP(1.0);
            return options;
        }
    }
}

