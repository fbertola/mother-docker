package com.github.fbertola.motherdocker.spock

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import org.spockframework.runtime.extension.ExtensionException
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

import static com.github.fbertola.motherdocker.MotherDocker.buildProjectFromFile

class MotherDockingInterceptor implements IMethodInterceptor {

    private WithDockerConfig annotation

    MotherDockingInterceptor(annotation) {
        this.annotation = annotation
    }

    void intercept(final IMethodInvocation invocation) throws Throwable {
        new DefaultDockerClient("unix:///var/run/docker.sock").withCloseable { DockerClient client ->
            def filename = annotation.filename()
            def project = buildProjectFromFile(filename, client)

            try {
                project.start()

                def portMappings = project.getPortMappings()
                def callback = createCallback(annotation.callback(), invocation)

                callback(portMappings)
                invocation.proceed()
            } finally {
                project.stop()
            }
        }
    }

    private static Closure createCallback(Class<? extends Closure> clazz, IMethodInvocation invocation) {
        try {
            def delegate = invocation.sharedInstance
            return clazz.newInstance(delegate, delegate)
        } catch (Exception e) {
            throw new ExtensionException('Failed to instantiate @WithDockerConfig callback', e)
        }
    }

}
