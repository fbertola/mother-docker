package com.github.fbertola.motherdocker.spock

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerCertificates
import com.spotify.docker.client.DockerClient
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import spock.lang.Specification

import java.lang.annotation.Annotation

import static com.github.fbertola.motherdocker.MotherDocker.buildProjectFromFile

class MotherDockingInterceptor implements IMethodInterceptor {

    private static final String PORT_MAPPINGS_PROPERTY_NAME = 'portMappings'

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

                createDynamicPortMappingsProperty(invocation, project.getPortMappings())

                invocation.proceed()
            } finally {
                project.stop()
            }
        }
    }

    private static void createDynamicPortMappingsProperty(IMethodInvocation invocation, Map portMappings) {
        def specMeta = getSpec( invocation ).metaClass

        specMeta.propertyMissing = { propertyName ->
            if (propertyName.equals(PORT_MAPPINGS_PROPERTY_NAME)){
                return portMappings
            }
        }

    }

    private static Specification getSpec( IMethodInvocation invocation )    {
        invocation.target as Specification
    }

}
