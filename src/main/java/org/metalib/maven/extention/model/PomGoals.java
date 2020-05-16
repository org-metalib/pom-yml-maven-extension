package org.metalib.maven.extention.model;

import java.util.List;

import lombok.Data;

@Data
public class PomGoals {
    List<String> before;
    List<String> after;
}
