package org.metalib.maven.extension.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.Site;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Distribution {

    Site site;
    String downloadUrl;
    Relocation relocation;

    DeploymentRepository snapshot;
    DeploymentRepository repository;
}
