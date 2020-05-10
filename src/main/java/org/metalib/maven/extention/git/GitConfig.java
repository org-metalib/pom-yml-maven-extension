package org.metalib.maven.extention.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Paths;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class GitConfig {

    static final String CONFIG = "config";

    final File gitFile;

    public GitConfig(@NonNull File gitDir) {
        gitFile = new File(gitDir, CONFIG);
    }

    public boolean exists() {
        return gitFile.isFile();
    }

    @SneakyThrows
    public String extractRemoteUrl() {
        try (val reader = new BufferedReader(new FileReader(gitFile))) {
            boolean in = false;
            String line;
            while (null != (line = reader.readLine())) {
                if (in) {
                    val index = line.indexOf('=');
                    if (-1 < index && "url".equals(line.substring(0,index).trim())) {
                        return line.substring(index+1);
                    }
                } else if (line.startsWith("[remote \"origin\"]")) {
                    in = true;
                }
            }
        }
        return null;
    }
}
