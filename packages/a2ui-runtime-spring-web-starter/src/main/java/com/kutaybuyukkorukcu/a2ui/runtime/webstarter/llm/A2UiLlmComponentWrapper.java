package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmComponentWrapper(
        @JsonProperty("Text") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmTextComponent text,
        @JsonProperty("Image") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmImageComponent image,
        @JsonProperty("Icon") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmIconComponent icon,
        @JsonProperty("Video") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmVideoComponent video,
        @JsonProperty("AudioPlayer") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmAudioPlayerComponent audioPlayer,
        @JsonProperty("Row") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmRowComponent row,
        @JsonProperty("Column") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmColumnComponent column,
        @JsonProperty("List") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmListComponent list,
        @JsonProperty("Card") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmCardComponent card,
        @JsonProperty("Tabs") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmTabsComponent tabs,
        @JsonProperty("Divider") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmDividerComponent divider,
        @JsonProperty("Modal") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmModalComponent modal,
        @JsonProperty("Button") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmButtonComponent button,
        @JsonProperty("CheckBox") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmCheckBoxComponent checkBox,
        @JsonProperty("TextField") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmTextFieldComponent textField,
        @JsonProperty("DateTimeInput") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmDateTimeInputComponent dateTimeInput,
        @JsonProperty("MultipleChoice") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmMultipleChoiceComponent multipleChoice,
        @JsonProperty("Slider") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmSliderComponent slider
) {
}
