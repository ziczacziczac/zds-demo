package com.example.entities;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.arc.All;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextLabel {
  private int id;
  private String text;
  private List<Entity> entities;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Entity{
    private int id;
    private String label;
    @JsonProperty("start_offset")
    private int startOffset;
    @JsonProperty("end_offset")
    private int endOffset;
  }
}
