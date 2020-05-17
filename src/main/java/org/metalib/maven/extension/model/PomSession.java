package org.metalib.maven.extension.model;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PomSession {
    private PomGoals goals;
    private PomProfiles profiles;
    @JsonProperty("user-properties")
    private Properties userProperties;
    @JsonProperty("system-properties")
    private Properties systemProperties;
}
