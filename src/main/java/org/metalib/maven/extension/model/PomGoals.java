package org.metalib.maven.extension.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PomGoals {
    @JsonProperty("on-empty")
    List<String> onEmpty;
    List<String> before;
    List<String> after;
}
