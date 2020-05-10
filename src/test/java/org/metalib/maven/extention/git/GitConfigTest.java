package org.metalib.maven.extention.git;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import lombok.SneakyThrows;
import lombok.val;

public class GitConfigTest {

    @SneakyThrows
    @Test
    public void test() throws IOException {
        val config = new GitConfig(new File(System.getProperty("user.dir"), ".git"));
        assertTrue(config.exists());
        val url = config.extractRemoteUrl();
        assertNotNull(url);
    }
}