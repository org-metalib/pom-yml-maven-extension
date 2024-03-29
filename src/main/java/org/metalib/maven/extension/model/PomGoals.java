package org.metalib.maven.extension.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PomGoals {
    @JsonProperty("on-empty")
    List<String> onEmpty;
    List<String> before;
    List<String> after;
}
