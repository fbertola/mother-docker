package com.github.fbertola.motherdocker.spock

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import groovy.util.logging.Slf4j
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import spock.lang.Specification

import static com.github.fbertola.motherdocker.MotherDocker.buildProjectFromFile

@Slf4j
class MotherDockingInterceptor implements IMethodInterceptor {

    private static final String SERVICES_INFO_PROPERTY_NAME = 'servicesInfo'

    private final WithDockerConfig annotation

    MotherDockingInterceptor(annotation) {
        this.annotation = annotation
    }

    void intercept(final IMethodInvocation invocation) throws Throwable {
        // TODO: certificates
        final DockerClient dockerClient = DefaultDockerClient.builder()
                .uri(annotation.uri())
                .build()

        dockerClient.withCloseable { client ->
            def filename = annotation.filename()
            def project = buildProjectFromFile(filename, client as DefaultDockerClient)

            try {
                project.start()

                createDynamicServicesInfoProperty(invocation, project.getServicesInfo())

                invocation.proceed()
            } catch (Exception e) {
                log.error('Error while starting project', e)
            } finally {
                project.stop()
            }
        }
    }

    private static void createDynamicServicesInfoProperty(IMethodInvocation invocation, Map servicesInfo) {
        def specMeta = getSpec(invocation).metaClass

        specMeta.propertyMissing = { propertyName ->
            if (propertyName.equals(SERVICES_INFO_PROPERTY_NAME)) {
                return servicesInfo
            }
        }

    }

    private static Specification getSpec(IMethodInvocation invocation) {
        invocation.target as Specification
    }

}
