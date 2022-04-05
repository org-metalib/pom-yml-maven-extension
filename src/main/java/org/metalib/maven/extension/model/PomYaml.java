package org.metalib.maven.extension.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PomYaml {
    private PomSession session;
    private PomRepositoryInfo repositories;
    private Distribution distribution;
}
