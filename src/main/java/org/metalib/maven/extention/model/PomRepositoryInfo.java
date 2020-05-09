package org.metalib.maven.extention.model;

import java.util.List;

import lombok.Data;

@Data
public class PomRepositoryInfo {
    List<YmlArtifactRepository> artifacts;
    List<YmlArtifactRepository> plugins;
}
