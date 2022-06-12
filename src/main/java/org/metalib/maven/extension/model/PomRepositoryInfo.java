package org.metalib.maven.extension.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("SameNameButDifferent")
@Builder(toBuilder = true)
public class PomRepositoryInfo {
    List<YmlArtifactRepository> artifacts;
    List<YmlArtifactRepository> plugins;
}
