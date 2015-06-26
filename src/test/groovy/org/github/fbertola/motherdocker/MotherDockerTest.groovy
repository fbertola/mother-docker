package org.github.fbertola.motherdocker

import com.spotify.docker.client.DefaultDockerClient
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.util.environment.OperatingSystem

import static MotherDockerTest.isDockerReachable

class MotherDockerTest extends Specification {

    static final def ORIG = 'src/test/resources/docker-compose/original'

    static final def LOCAL_DOCKER_URI = 'http://127.0.0.1:2375'

    @IgnoreIf({ !OperatingSystem.current.linux || !isDockerReachable() })
    def 'BuildProjectFromFile - should correctly create and start a simple golang image'() {
        setup:
        def client = DefaultDockerClient.builder().uri(LOCAL_DOCKER_URI).build()

        when:
        def project = MotherDocker.buildProjectFromFile("${ORIG}/golang.yml", client)
        project.start()
        project.stop()

        then:
        notThrown(Exception.class)
    }

    public static def isDockerReachable() {
        try {
            def info = DefaultDockerClient.builder().uri(LOCAL_DOCKER_URI).build().info()
            return info != null
        } catch (Exception ignored) {
            return false
        }
    }

}
