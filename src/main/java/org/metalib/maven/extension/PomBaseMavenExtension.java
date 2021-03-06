package org.metalib.maven.extension;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.metalib.maven.extension.git.GitConfig;
import org.metalib.maven.extension.model.PomGoals;
import org.metalib.maven.extension.model.PomProfiles;
import org.metalib.maven.extension.model.PomRepositoryInfo;
import org.metalib.maven.extension.model.PomSession;
import org.metalib.maven.extension.model.PomYaml;
import org.metalib.maven.extension.model.YmlArtifactRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

@Component( role = AbstractMavenLifecycleParticipant.class, hint = "PomBaseMavenExtension")
public class PomBaseMavenExtension extends AbstractMavenLifecycleParticipant {

    static final String USER_HOME = "user.home";
    static final String USER_HOME_PATH = System.getProperty(USER_HOME);
    static final String POM_YML = "pom.yml";

    static final String DOT = ".";
    static final String TILDA = "~";
    static final String TRUE = "true";
    static final String GIT = "git";
    static final String DOT_GIT = DOT + GIT;
    static final String EMPTY = "";
    static final String SLASH = "/";
    static final String DEFAULT_REPOSITORY_LAYOUT_ID = "default";

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

    @Requirement
    MavenRepositorySystem mavenRepositorySystem;

    @Override
    public void afterSessionStart(final MavenSession session) throws MavenExecutionException {
        logger.info("pom-yml-maven-extension: session starting ...");
        val pomXmlFile = Optional.of(session).map(MavenSession::getRequest).map(MavenExecutionRequest::getPom).orElse(null);
        if (null == pomXmlFile) {
            logger.error("<pom.xml> file info has not been provided.");
            return;
        }
        val pomYmlFile = new File(pomXmlFile.getParent(), POM_YML);
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
        val projectBuildingRequest = session.getProjectBuildingRequest();
        updatePofileIds(projectBuildingRequest, pomSession);
        updateUserProperties(projectBuildingRequest.getUserProperties(), pomSession);
        updateSystemProperties(projectBuildingRequest.getSystemProperties(), pomSession);
        updateScmGitProperties(session, pomSession);

        val repositories = Optional.of(pomYml).map(PomYaml::getRepositories).orElse(null);
        if (null != repositories) {
            updateRepositories(projectBuildingRequest, repositories);
        }
        val requestGoals = Optional.of(session).map(MavenSession::getRequest).map(MavenExecutionRequest::getGoals).orElse(null);
        val pomYmlGoals = Optional.of(pomYml).map(PomYaml::getSession).map(PomSession::getGoals).orElse(null);
        updateGoals(requestGoals, pomYmlGoals);
    }

    private void updateGoals(final List<String> targetGoals, final PomGoals extensionGoals) {
        if (null == targetGoals || null == extensionGoals) {
            return;
        }
        if (null != extensionGoals.getOnEmpty() && targetGoals.isEmpty()) {
            targetGoals.addAll(extensionGoals.getOnEmpty());
            return;
        }
        if (null != extensionGoals.getBefore()) {
            val counter = new Counter();
            extensionGoals.getBefore().forEach(goal -> targetGoals.add(counter.value++, goal));
        }
        if (null != extensionGoals.getAfter()) {
            targetGoals.addAll(extensionGoals.getAfter());
        }
    }

    static class Counter {
        int value;
    }

    private void updatePofileIds(@NonNull final ProjectBuildingRequest request, final PomSession pomSession) throws MavenExecutionException {
        val activeProfileIds = new HashSet<>(request.getActiveProfileIds());
        val activeProfiles = Optional.of(pomSession).map(PomSession::getProfiles).map(PomProfiles::getActive).orElse(null);
        if (null != activeProfiles) {
            val inactiveProfileIds = new HashSet<>(request.getInactiveProfileIds());
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
    }

    private void updateRepositories(@NonNull final ProjectBuildingRequest request,
            final PomRepositoryInfo repositories) throws MavenExecutionException {
        request.setRemoteRepositories(upsert(request.getRemoteRepositories(), repositories.getArtifacts()));
        request.setPluginArtifactRepositories(upsert(request.getPluginArtifactRepositories(), repositories.getPlugins()));
    }

    List<ArtifactRepository> upsert(final List<ArtifactRepository> requestRepositories,
            final List<YmlArtifactRepository> repositories) throws MavenExecutionException {
        if (null == repositories) {
            return Collections.emptyList();
        }
        val remoteRepositories = null == requestRepositories? new ArrayList<ArtifactRepository>() : requestRepositories;
        for (val r : repositories) {
            try {
                remoteRepositories.add(mavenRepositorySystem.createArtifactRepository(r.getId(), r.getUrl(),
                        DEFAULT_REPOSITORY_LAYOUT_ID, r.getSnapshots(), r.getReleases()));
            } catch (Exception e) {
                throw new MavenExecutionException("", e);
            }
        }
        return remoteRepositories;
    }

    @SneakyThrows
    private void updateScmGitProperties(final MavenSession session, final PomSession pomSession) {
        val projectBuildingRequest = session.getProjectBuildingRequest();
        if (null == projectBuildingRequest) {
            return;
        }
        val userProperties = projectBuildingRequest.getUserProperties();
        if (null == userProperties) {
            return;
        }
        if (!(TRUE.equals(userProperties.get(POM_BASE_SCM_GIT_LOAD_GIT_URL_PROPERTY)) ||
              TRUE.equals(Optional.of(pomSession).map(PomSession::getUserProperties).map(v -> v.getProperty(POM_BASE_SCM_GIT_LOAD_GIT_URL_PROPERTY)).orElse(null)))) {
            return;
        }
        val pomXmlFile = Optional.of(session).map(MavenSession::getRequest).map(v -> v.getPom()).orElse(null);
        if (null == pomXmlFile) {
            return;
        }
        val config = new GitConfig(new File(pomXmlFile.getParent(), DOT_GIT));
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
                    name.endsWith(DOT_GIT)? name.substring(0, name.length()-4) : name);
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_EXT_PROPERTY, name.endsWith(DOT_GIT)? GIT : EMPTY);
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

    private void updateUserProperties(final Properties userProperties, final PomSession pomSession) {
        val pomUserProperties = pomSession.getUserProperties();
        if (null == pomUserProperties) {
            return;
        }
        val userPropertyNames = userProperties.keySet();
        for (val name : pomUserProperties.stringPropertyNames()) {
            if (userPropertyNames.contains(name)) {
                logger.info(format("User property <%s> has been set with <%s>", name, userProperties.get(name)));
            } else {
                userProperties.setProperty(name, pomUserProperties.getProperty(name));
            }
        }
    }

    private void updateSystemProperties(final Properties systemProperties, final PomSession pomSession) {
        val pomSystemProperties = pomSession.getSystemProperties();
        if (null == pomSystemProperties) {
            return;
        }
        val systemPropertyNames = systemProperties.stringPropertyNames();
        for (val name : pomSystemProperties.stringPropertyNames()) {
            if (systemPropertyNames.contains(name)) {
                logger.info(format("System property <%s> has been set with <%s>", name, systemProperties.getProperty(name)));
            } else {
                systemProperties.setProperty(name, pomSystemProperties.getProperty(name));
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

    public static ObjectMapper jacksonYaml() {
        return JACKSON_YAML.copy();
    }

    public static String pathToUserHome(String path) {
        val userDirLength = USER_HOME_PATH.length();
        return userDirLength < path.length() && path.startsWith(USER_HOME_PATH)
                ? TILDA + SLASH + path.substring(userDirLength+1) : path;
    }
}
