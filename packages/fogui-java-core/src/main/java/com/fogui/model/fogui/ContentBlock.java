package com.fogui.model.fogui;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentBlock {

  @JsonProperty("type")
  @Builder.Default
  private String type = "text";

  @JsonProperty("value")
  private Object value;

  @JsonProperty("componentType")
  private String componentType;

  @JsonProperty("props")
  private Object props;

  @JsonProperty("children")
  private List<ContentBlock> children;

  public static ContentBlock text(String value) {
    return ContentBlock.builder().type("text").value(value).build();
  }

  public static ContentBlock component(String componentType, Object props) {
    return ContentBlock.builder()
        .type("component")
        .componentType(componentType)
        .props(props)
        .build();
  }
}
