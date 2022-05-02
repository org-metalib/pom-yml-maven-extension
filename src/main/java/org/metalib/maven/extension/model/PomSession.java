package org.metalib.maven.extension.model;

import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PomSession {
    private PomGoals goals;
    private PomProfiles profiles;
    @JsonProperty("user-properties")
    private Properties userProperties;
    @JsonProperty("system-properties")
    private Properties systemProperties;
    @JsonProperty("user-property-sources")
    private List<PomPropertySource> userPropertySources;
}
