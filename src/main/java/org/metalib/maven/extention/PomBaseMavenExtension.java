package org.metalib.maven.extention;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.metalib.maven.extention.git.GitConfig;
import org.metalib.maven.extention.model.PomProfiles;
import org.metalib.maven.extention.model.PomSession;
import org.metalib.maven.extention.model.PomYaml;

import lombok.SneakyThrows;
import lombok.val;

@Component( role = AbstractMavenLifecycleParticipant.class, hint = "Extension1")
public class PomBaseMavenExtension extends AbstractMavenLifecycleParticipant {

    static final String USER_HOME = "user.home";
    static final String USER_HOME_PATH = System.getProperty(USER_HOME);
    static final String USER_DIR = System.getProperty("user.dir");
    static final String POM_YML = "pom.yml";

    static final String TILDA = "~";
    static final String TRUE = "true";
    static final String EMPTY = "";
    static final String SLASH = "/";

    static final String POM_BASE_SCM_GIT_LOAD_GIT_URL_PROPERTY = "pom-yaml.scm.git.load-git-url";
    static final String POM_BASE_SCM_GIT_GIT_URL_PROPERTY = "pom-yaml.scm.git.git-url";
    static final String POM_BASE_SCM_GIT_GIT_URL_PATH_PROPERTY = "pom-yaml.scm.git.git-url.path";
    static final String POM_BASE_SCM_GIT_GIT_URL_NAME_PROPERTY = "pom-yaml.scm.git.git-url.name";
    static final String POM_BASE_SCM_GIT_GIT_URL_EXT_PROPERTY = "pom-yaml.scm.git.git-url.ext";
    static final String POM_BASE_SCM_GIT_GIT_URL_HOST_PROPERTY = "pom-yaml.scm.git.git-url.host";
    static final String POM_BASE_SCM_GIT_GIT_URL_SCHEMA_PROPERTY = "pom-yaml.scm.git.git-url.schema";
    static final String POM_BASE_SCM_GIT_GIT_URL_PORT_PROPERTY = "pom-yaml.scm.git.git-url.port";
    static final String POM_BASE_SCM_GIT_GIT_URL_USER_PROPERTY = "pom-yaml.scm.git.git-url.user";

    public static final ObjectMapper JACKSON_JSON = new ObjectMapper().registerModule(new Jdk8Module());
    public static final ObjectMapper JACKSON_YAML = new ObjectMapper(new YAMLFactory()).registerModule(new Jdk8Module());

    @Requirement
    Logger logger;

    @Override
    public void afterSessionStart(final MavenSession session) throws MavenExecutionException {
        val request = session.getProjectBuildingRequest();
        val pomYmlFile = new File(USER_DIR, POM_YML);
        val pomYml = loadPomYaml(pomYmlFile);
        PomSession pomSession =  Optional.ofNullable(pomYml).map(PomYaml::getSession).orElse(null);
        if (null == pomSession) {
            if (logger.isErrorEnabled()) {
                val pomYmlFilePath = pathToUserHome(pomYmlFile.getAbsolutePath());
                logger.error(format("<%s> not found.", pomYmlFilePath));
            }
            return;
        }
        if (logger.isInfoEnabled()) {
            val pomYmlFilePath = pathToUserHome(pomYmlFile.getAbsolutePath());
            logger.info(format("<%s> found.", pomYmlFilePath));
            if (logger.isDebugEnabled()) {
                try {
                    logger.debug(jacksonYaml().writeValueAsString(pomYml));
                } catch (JsonProcessingException e) {
                    throw new MavenExecutionException(format("Error writing <%s>", pomYmlFilePath), e);
                }
            }
        }
        val inactiveProfileIds = new HashSet<>(request.getInactiveProfileIds());
        val activeProfileIds = new HashSet<>(request.getActiveProfileIds());
        val activeProfiles = Optional.of(pomSession).map(PomSession::getProfiles).map(PomProfiles::getActive).orElse(null);
        if (null != activeProfiles) {
            val profileIds = activeProfiles.stream()
                    .filter(v -> !inactiveProfileIds.contains(v))
                    .collect(Collectors.toCollection(TreeSet::new));
            profileIds.addAll(activeProfileIds);
            request.setActiveProfileIds(Arrays.asList(profileIds.toArray(new String[0])));
        }
        val inactiveProfiles = Optional.of(pomSession).map(PomSession::getProfiles).map(PomProfiles::getInactive).orElse(null);
        if (null != inactiveProfiles) {
            val profileIds = inactiveProfiles.stream()
                    .filter(v -> !activeProfileIds.contains(v))
                    .collect(Collectors.toCollection(TreeSet::new));
            profileIds.addAll(inactiveProfiles);
            request.setInactiveProfileIds(Arrays.asList(profileIds.toArray(new String[0])));
        }
        updateUserProperties(session, pomSession);
        updateSystemProperties(session, pomSession);
        updateScmSection(session);
    }

    @SneakyThrows
    private void updateScmSection(final MavenSession session) {
        val userProperties = session.getUserProperties();
        if (!TRUE.equals(userProperties.get(POM_BASE_SCM_GIT_LOAD_GIT_URL_PROPERTY))) {
            return;
        }
        val config = new GitConfig();
        if (!config.exists()) {
            return;
        }
        val remoteUrl = config.extractRemoteUrl();
        if (null == remoteUrl) {
            logger.warn("<.git> subdirectory exists but remote git repository has been not found.");
        } else {
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_PROPERTY, remoteUrl);
            val uri = new URI(remoteUrl.trim());
            val path = extractPathPart(uri.getPath());
            val name = extractNamePart(uri.getPath());
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_PATH_PROPERTY, path.startsWith(SLASH) ? path.substring(1) : path);
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_NAME_PROPERTY,
                    name.endsWith(".git")? name.substring(0, name.length()-4) : name);
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_EXT_PROPERTY, name.endsWith(".git")? "git" : EMPTY);
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_HOST_PROPERTY, uri.getHost());
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_SCHEMA_PROPERTY, uri.getScheme());
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_PORT_PROPERTY, EMPTY + uri.getPort());
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_USER_PROPERTY, uri.getUserInfo());
        }
    }

    private static String extractPathPart(String fullPath) {
        val pathIndex = fullPath.lastIndexOf('/');
        return 0 < pathIndex?  fullPath.substring(0, pathIndex) : EMPTY;
    }

    private static String extractNamePart(String fullPath) {
        val pathIndex = fullPath.lastIndexOf('/');
        return 0 < pathIndex? fullPath.substring(pathIndex+1) : fullPath;
    }

    private void updateUserProperties(final MavenSession session, final PomSession pomSession) {
        val userProperties = session.getUserProperties();
        val userPropertyNames = userProperties.stringPropertyNames();
        val pomUserProperties = pomSession.getUserProperties();
        if (null != pomUserProperties) {
            for (val name : pomUserProperties.stringPropertyNames()) {
                if (userPropertyNames.contains(name)) {
                    logger.info(format("User property <%s> has been set with <%s>", name, userProperties.getProperty(name)));
                } else {
                    userProperties.setProperty(name, pomUserProperties.getProperty(name));
                }
            }
        }
    }

    private void updateSystemProperties(final MavenSession session, final PomSession pomSession) {
        val systemProperties = session.getSystemProperties();
        val systemPropertyNames = systemProperties.stringPropertyNames();
        val pomSystemProperties = pomSession.getSystemProperties();
        if (null != pomSystemProperties) {
            for (val name : pomSystemProperties.stringPropertyNames()) {
                if (systemPropertyNames.contains(name)) {
                    logger.info(format("System property <%s> has been set with <%s>", name, systemProperties.getProperty(name)));
                } else {
                    systemProperties.setProperty(name, pomSystemProperties.getProperty(name));
                }
            }
        }
    }

    private PomYaml loadPomYaml(final File pomYmlFile) throws MavenExecutionException {
        try {
            return pomYmlFile.isFile()? jacksonYaml().readValue(pomYmlFile, PomYaml.class) : null;
        } catch (IOException e) {
            throw new MavenExecutionException(format("Error reading <%s>", pomYmlFile.getAbsolutePath()), e);
        }
    }

    /**
     * @return default {@code ObjectMapper} instance to operate with yaml format
     */
    public static ObjectMapper jacksonYaml() {
        return JACKSON_YAML.copy();
    }

    public static String pathToUserHome(String path) {
        val userDirLength = USER_HOME_PATH.length();
        return userDirLength < path.length() && path.startsWith(USER_HOME_PATH)
                ? TILDA + SLASH + path.substring(userDirLength+1) : path;
    }
}
