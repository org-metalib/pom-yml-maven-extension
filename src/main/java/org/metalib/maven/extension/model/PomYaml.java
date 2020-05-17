package org.metalib.maven.extension.model;

import lombok.Data;

@Data
public class PomYaml {
    private PomSession session;
    private PomRepositoryInfo repositories;
}
