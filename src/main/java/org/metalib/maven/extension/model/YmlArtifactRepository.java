package org.metalib.maven.extension.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.maven.artifact.repository.MavenArtifactRepository;

import lombok.Data;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class YmlArtifactRepository  extends MavenArtifactRepository {
    private String name;
}
