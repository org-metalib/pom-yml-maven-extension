package org.metalib.maven.extention.model;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PomSession {
    PomProfiles profiles;
    @JsonProperty("user-properties")
    Properties userProperties;
    @JsonProperty("system-properties")
    Properties systemProperties;
}
