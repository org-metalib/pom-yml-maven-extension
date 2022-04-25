package org.metalib.maven.extension.property;

import lombok.SneakyThrows;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.inheritance.DefaultInheritanceAssembler;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.locator.DefaultModelLocator;
import org.apache.maven.model.management.DefaultDependencyManagementInjector;
import org.apache.maven.model.management.DefaultPluginManagementInjector;
import org.apache.maven.model.normalization.DefaultModelNormalizer;
import org.apache.maven.model.path.DefaultModelPathTranslator;
import org.apache.maven.model.path.DefaultModelUrlNormalizer;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.DefaultUrlNormalizer;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.superpom.DefaultSuperPomProvider;
import org.apache.maven.model.validation.DefaultModelValidator;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectModelResolver;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class ProjectModelPropertyMappingTest {

    @Test
    public void testProjectMapping() throws ModelBuildingException {
        final var result = BeanPropertyMap.resolve("project", modelBuildingResult().getEffectiveModel());
        assertNotNull(result);
    }

    @SneakyThrows
    static ModelBuildingResult modelBuildingResult() {
        final var session = repositorySystemSession();
        final var buildingRequest =createModelBuildingRequest(session)
                .setActiveProfileIds(List.of())
                .setPomFile(new File("pom.xml"));
        final var modelProcessor = new DefaultModelProcessor()
                .setModelLocator(new DefaultModelLocator())
                .setModelReader(new DefaultModelReader());
        return new DefaultModelBuilder()
                .setModelProcessor(modelProcessor)
                .setProfileSelector(new DefaultProfileSelector())
                .setModelValidator(new DefaultModelValidator())
                .setSuperPomProvider(new DefaultSuperPomProvider()
                        .setModelProcessor(modelProcessor))
                .setModelNormalizer(new DefaultModelNormalizer())
                .setInheritanceAssembler(new DefaultInheritanceAssembler())
                .setModelInterpolator((model1, projectDir, request, problems) -> model1)
                .setModelUrlNormalizer(new DefaultModelUrlNormalizer().setUrlNormalizer(new DefaultUrlNormalizer()))
                .setModelPathTranslator(new DefaultModelPathTranslator().setPathTranslator(new DefaultPathTranslator()))
                .setPluginManagementInjector(new DefaultPluginManagementInjector())
                .setDependencyManagementInjector(new DefaultDependencyManagementInjector())
                .build(buildingRequest);
    }

    static RepositorySystemSession repositorySystemSession() {
        return new DefaultRepositorySystemSession();
    }

    static ModelBuildingRequest createModelBuildingRequest(final RepositorySystemSession session) {
        return new DefaultModelBuildingRequest()
            .setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL )
            .setProcessPlugins( false )
            .setTwoPhaseBuilding( false )
            .setProfiles(List.of())
            .setSystemProperties( toProperties( session.getSystemProperties() ) )
            .setUserProperties( toProperties( session.getUserProperties() ) )
            .setModelCache( DefaultModelCache.newInstance(session))
            .setModelResolver( createProjectModelResolver(session));
    }

    static ProjectModelResolver createProjectModelResolver(final RepositorySystemSession session) {
        return new ProjectModelResolver( session, new RequestTrace(""), new DefaultRepositorySystem(),
                new DefaultRemoteRepositoryManager(), List.of(),
                ProjectBuildingRequest.RepositoryMerging.REQUEST_DOMINANT, null );
    }

    static Properties toProperties(final Map<String,String> map) {
        Properties props = new Properties();
        props.putAll( map );
        return props;
    }

}
