package org.metalib.maven.extension.model;

import org.apache.maven.artifact.repository.MavenArtifactRepository;

import lombok.Data;

@Data
public class YmlArtifactRepository  extends MavenArtifactRepository {
    private String name;
}
