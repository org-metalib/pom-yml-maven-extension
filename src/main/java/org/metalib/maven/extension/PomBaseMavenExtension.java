package org.metalib.maven.extension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.interpolation.MavenBuildTimestamp;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.metalib.maven.extension.git.GitConfig;
import org.metalib.maven.extension.model.Distribution;
import org.metalib.maven.extension.model.PomGoals;
import org.metalib.maven.extension.model.PomProfiles;
import org.metalib.maven.extension.model.PomRepositoryInfo;
import org.metalib.maven.extension.model.PomSession;
import org.metalib.maven.extension.model.PomYaml;
import org.metalib.maven.extension.model.YmlArtifactRepository;
import org.metalib.maven.extension.property.BeanPropertyMap;
import org.metalib.maven.extension.property.PropertyResolver;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.maven.model.interpolation.MavenBuildTimestamp.BUILD_TIMESTAMP_FORMAT_PROPERTY;

@Component( role = AbstractMavenLifecycleParticipant.class, hint = "PomBaseMavenExtension")
public class PomBaseMavenExtension extends AbstractMavenLifecycleParticipant {

    static final String USER_HOME = "user.home";
    static final String USER_HOME_PATH = System.getProperty(USER_HOME);
    static final String POM_YML = "pom.yml";
    static final String POM_XML = "pom.xml";

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

    public void afterProjectsRead( MavenSession session ) throws MavenExecutionException {
        logger.info("pom-yml-maven-extension: project read ...");
        final var pomXmlFile = Optional.of(session).map(MavenSession::getRequest).map(MavenExecutionRequest::getPom).orElse(null);
        if (null == pomXmlFile) {
            logger.error("<pom.xml> file info has not been provided.");
            return;
        }
        final var pomYmlFile = pomYmlFile(pomXmlFile);
        if (null == pomYmlFile) {
            return;
        }
        final var pomYml = loadPomYaml(pomYmlFile);
        Optional.ofNullable(pomYml).map(PomYaml::getDistribution).ifPresent(v -> updateDistribution(session, v));
        updateRepositories(session);

        final var project = session.getCurrentProject();
        if (null != project) {
            final var projectProperties = project.getProperties();
            new PropertyResolver().resolve(propertyRequest(session)).forEach((k,v) -> {
                if (!projectProperties.contains(k)) {
                    projectProperties.put(k,v);
                }
            });
        }
        return;
    }

    private void updateRepositories(@NonNull final MavenSession session) {
        final var project = session.getCurrentProject();
        if (null == project) {
            return;
        }
        final var projectRepoSet = new HashSet<String>();
        final var projectRepositories = project.getRepositories();
        return;
    }

    static Map<String,String> propertyRequest(MavenSession session) {
        final var result = new HashMap<String,String>();
        Optional.of(session).map(MavenSession::getCurrentProject).ifPresent(v -> {
            final var baseDir = v.getBasedir();
            if (null != baseDir) {
                result.put("project.basedir", baseDir.getAbsolutePath());
                result.put("project.baseUri", baseDir.getAbsoluteFile().toPath().toUri().toASCIIString());
            }
            result.putAll(BeanPropertyMap.resolve("project", v.getModel()));
        });
        final var mavenExecutionRequest = Optional.of(session).map(MavenSession::getRequest);
        mavenExecutionRequest.map(MavenExecutionRequest::getSystemProperties)
                .ifPresent(v -> v.forEach((k,vv) -> result.put(k.toString(),vv.toString())));
        mavenExecutionRequest.map(MavenExecutionRequest::getUserProperties)
                .ifPresent(v -> v.forEach((k,vv) -> result.put(k.toString(),vv.toString())));
        final var projectBuildingRequest = Optional.of(session).map(MavenSession::getProjectBuildingRequest);
        projectBuildingRequest.map(ProjectBuildingRequest::getSystemProperties)
                .ifPresent(v -> v.forEach((k,vv) -> result.put(k.toString(),vv.toString())));
        projectBuildingRequest.map(ProjectBuildingRequest::getUserProperties)
                .ifPresent(v -> v.forEach((k,vv) -> result.put(k.toString(),vv.toString())));
        mavenExecutionRequest.ifPresent(v -> {
            final var timestampFormat = result.get(BUILD_TIMESTAMP_FORMAT_PROPERTY);
            final var startTime = v.getStartTime();
            final MavenBuildTimestamp buildStartTime;
            if (null == startTime && null == timestampFormat) {
                buildStartTime = new MavenBuildTimestamp();
            } else if (null == startTime) {
                buildStartTime = new MavenBuildTimestamp(new Date(), timestampFormat);
            } else if (null == timestampFormat) {
                buildStartTime = new MavenBuildTimestamp(startTime);
            } else {
                buildStartTime = new MavenBuildTimestamp(startTime, timestampFormat);
            }
            result.put("maven.build.timestamp", buildStartTime.formattedTimestamp());
        });
        Optional.of(session).map(MavenSession::getCurrentProject).map(MavenProject::getProperties).ifPresent(v -> {
            v.forEach((kk,vv) -> result.put(kk.toString(),vv.toString()));
        });
        return result;
    }

    @SneakyThrows
    private void updateDistribution(@NonNull MavenSession session, @NonNull final Distribution distribution) {
        final var project = session.getCurrentProject();
        if (null == project) {
            return;
        }
        var target = project.getDistributionManagement();
        if (null == target) {
            target = new DistributionManagement();
            project.setDistributionManagement(target);
        }
        if (null == target.getDownloadUrl()) {
            target.setDownloadUrl(distribution.getDownloadUrl());
        }
        final var relocationSource = distribution.getRelocation();
        if (null != relocationSource && null == target.getRelocation()) {
            target.setRelocation(relocationSource);
        }
        final var repositorySource = distribution.getRepository();
        if (null != repositorySource && null == target.getRepository()) {
            target.setRepository(repositorySource);
        }
        final var snapshotSource = distribution.getSnapshot();
        if (null != snapshotSource && null == target.getSnapshotRepository()) {
            target.setSnapshotRepository(repositorySource);
        }
        final var siteSource = distribution.getSite();
        if (null != siteSource && null == target.getSite()) {
            target.setSite(siteSource);
        }
    }

    static File pomYmlFile(File pomXmlFile) {
        if (null == pomXmlFile || !pomXmlFile.isFile()) {
            return null;
        }
        var dir = pomXmlFile.getParentFile();
        do {
            final var pomYmlFile = new File(dir, POM_YML);
            if (pomYmlFile.isFile()) {
                return pomYmlFile;
            }
            dir = dir.getParentFile();
            // TODO: It's better to target parent pom location rather than to rely on dir hierarchy
        } while (null != dir && new File(dir, POM_XML).isFile());
        return null;
    }

    @Override
    public void afterSessionStart(final MavenSession session) throws MavenExecutionException {
        logger.info("pom-yml-maven-extension: session starting ...");
        final var pomXmlFile = Optional.of(session).map(MavenSession::getRequest).map(MavenExecutionRequest::getPom).orElse(null);
        if (null == pomXmlFile) {
            logger.error("<pom.xml> file info has not been provided.");
            return;
        }
        final var pomYmlFile = pomYmlFile(pomXmlFile);
        if (null == pomYmlFile) {
            return;
        }
        final var pomYml = loadPomYaml(pomYmlFile);
        if (null == pomYml) {
            return;
        }
        var pomSession =  Optional.of(pomYml).map(PomYaml::getSession).orElse(null);
        if (null == pomSession) {
            if (logger.isErrorEnabled()) {
                final var pomYmlFilePath = pathToUserHome(pomYmlFile.getAbsolutePath());
                logger.error(format("<%s> not found.", pomYmlFilePath));
            }
            return;
        } else {
            final var projectBuildingRequest = session.getProjectBuildingRequest();
            updateProfileIds(projectBuildingRequest, pomSession);
            updateUserProperties(projectBuildingRequest.getUserProperties(), pomSession);
            updateSystemProperties(projectBuildingRequest.getSystemProperties(), pomSession);
            updateScmGitProperties(session, pomSession);

            Optional.of(pomYml).map(PomYaml::getRepositories).ifPresent(v -> updateRepositories(projectBuildingRequest, v));
        }
        if (logger.isInfoEnabled()) {
            final var pomYmlFilePath = pathToUserHome(pomYmlFile.getAbsolutePath());
            logger.info(format("<%s> found.", pomYmlFilePath));
            if (logger.isDebugEnabled()) {
                try {
                    logger.debug(jacksonYaml().writeValueAsString(pomYml));
                } catch (JsonProcessingException e) {
                    throw new MavenExecutionException(format("Error writing <%s>", pomYmlFilePath), e);
                }
            }
        }
        final var requestGoals = Optional.of(session).map(MavenSession::getRequest).map(MavenExecutionRequest::getGoals).orElse(null);
        final var pomYmlGoals = Optional.of(pomYml).map(PomYaml::getSession).map(PomSession::getGoals).orElse(null);
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
            final var counter = new Counter();
            extensionGoals.getBefore().forEach(goal -> targetGoals.add(counter.value++, goal));
        }
        if (null != extensionGoals.getAfter()) {
            targetGoals.addAll(extensionGoals.getAfter());
        }
    }

    static class Counter {
        int value;
    }

    private void updateProfileIds(@NonNull final ProjectBuildingRequest request, final PomSession pomSession) throws MavenExecutionException {
        final var activeProfileIds = new HashSet<>(request.getActiveProfileIds());
        final var activeProfiles = Optional.of(pomSession).map(PomSession::getProfiles).map(PomProfiles::getActive).orElse(null);
        if (null != activeProfiles) {
            final var inactiveProfileIds = new HashSet<>(request.getInactiveProfileIds());
            final var profileIds = activeProfiles.stream()
                    .filter(v -> !inactiveProfileIds.contains(v))
                    .collect(Collectors.toCollection(TreeSet::new));
            profileIds.addAll(activeProfileIds);
            request.setActiveProfileIds(Arrays.asList(profileIds.toArray(new String[0])));
        }

        final var inactiveProfiles = Optional.of(pomSession).map(PomSession::getProfiles).map(PomProfiles::getInactive).orElse(null);
        if (null != inactiveProfiles) {
            final var profileIds = inactiveProfiles.stream()
                    .filter(v -> !activeProfileIds.contains(v))
                    .collect(Collectors.toCollection(TreeSet::new));
            profileIds.addAll(inactiveProfiles);
            request.setInactiveProfileIds(Arrays.asList(profileIds.toArray(new String[0])));
        }
    }

    @SneakyThrows
    private void updateRepositories(@NonNull final ProjectBuildingRequest request, final PomRepositoryInfo repositories) {
        request.setRemoteRepositories(upsert(request.getRemoteRepositories(), repositories.getArtifacts()));
        request.setPluginArtifactRepositories(upsert(request.getPluginArtifactRepositories(), repositories.getPlugins()));
    }

    List<ArtifactRepository> upsert(final List<ArtifactRepository> requestRepositories,
            final List<YmlArtifactRepository> repositories) throws MavenExecutionException {
        if (null == repositories) {
            return Collections.emptyList();
        }
        final var remoteRepositories = null == requestRepositories? new ArrayList<ArtifactRepository>() : requestRepositories;
        for (final var r : repositories) {
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
        final var projectBuildingRequest = session.getProjectBuildingRequest();
        if (null == projectBuildingRequest) {
            return;
        }
        final var userProperties = projectBuildingRequest.getUserProperties();
        if (null == userProperties) {
            return;
        }
        if (!(TRUE.equals(userProperties.get(POM_BASE_SCM_GIT_LOAD_GIT_URL_PROPERTY)) ||
              TRUE.equals(Optional.of(pomSession).map(PomSession::getUserProperties).map(v -> v.getProperty(POM_BASE_SCM_GIT_LOAD_GIT_URL_PROPERTY)).orElse(null)))) {
            return;
        }
        final var pomXmlFile = Optional.of(session).map(MavenSession::getRequest).map(v -> v.getPom()).orElse(null);
        if (null == pomXmlFile) {
            return;
        }
        final var config = new GitConfig(new File(pomXmlFile.getParent(), DOT_GIT));
        if (!config.exists()) {
            return;
        }
        final var remoteUrl = config.extractRemoteUrl();
        if (null == remoteUrl) {
            logger.warn("<.git> subdirectory exists but remote git repository has been not found.");
        } else {
            final var uri = new URI(remoteUrl.trim());
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_PROPERTY, uri.toASCIIString());
            final var path = extractPathPart(uri.getPath());
            final var name = extractNamePart(uri.getPath());
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_PATH_PROPERTY, path.startsWith(SLASH) ? path.substring(1) : path);
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_NAME_PROPERTY,
                    name.endsWith(DOT_GIT)? name.substring(0, name.length()-4) : name);
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_EXT_PROPERTY, name.endsWith(DOT_GIT)? GIT : EMPTY);
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_HOST_PROPERTY, uri.getHost());
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_SCHEMA_PROPERTY, uri.getScheme());
            userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_PORT_PROPERTY, EMPTY + uri.getPort());
            final var userInfo = uri.getUserInfo();
            if (null != userInfo) {
                userProperties.setProperty(POM_BASE_SCM_GIT_GIT_URL_USER_PROPERTY, userInfo);
            }
        }
    }

    private static String extractPathPart(String fullPath) {
        final var pathIndex = fullPath.lastIndexOf('/');
        return 0 < pathIndex?  fullPath.substring(0, pathIndex) : EMPTY;
    }

    private static String extractNamePart(String fullPath) {
        final var pathIndex = fullPath.lastIndexOf('/');
        return 0 < pathIndex? fullPath.substring(pathIndex+1) : fullPath;
    }

    private void updateUserProperties(final Properties userProperties, final PomSession pomSession) {
        final var pomUserProperties = pomSession.getUserProperties();
        if (null == pomUserProperties) {
            return;
        }
        final var userPropertyNames = userProperties.keySet();
        for (final var name : pomUserProperties.stringPropertyNames()) {
            if (userPropertyNames.contains(name)) {
                logger.info(format("User property <%s> has been set with <%s>", name, userProperties.get(name)));
            } else {
                userProperties.setProperty(name, pomUserProperties.getProperty(name));
            }
        }
    }

    private void updateSystemProperties(final Properties systemProperties, final PomSession pomSession) {
        final var pomSystemProperties = pomSession.getSystemProperties();
        if (null == pomSystemProperties) {
            return;
        }
        final var systemPropertyNames = systemProperties.stringPropertyNames();
        for (final var name : pomSystemProperties.stringPropertyNames()) {
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
        final var userDirLength = USER_HOME_PATH.length();
        return userDirLength < path.length() && path.startsWith(USER_HOME_PATH)
                ? TILDA + SLASH + path.substring(userDirLength+1) : path;
    }
}
