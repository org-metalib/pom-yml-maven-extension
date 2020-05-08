package org.metalib.maven.extention.model;

import java.util.List;

import lombok.Data;

@Data
public class PomProfiles {
    List<String> active;
    List<String> inactive;
}
