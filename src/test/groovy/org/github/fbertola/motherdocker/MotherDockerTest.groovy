package org.github.fbertola.motherdocker

import com.spotify.docker.client.DefaultDockerClient
import spock.lang.IgnoreIf
import spock.lang.Specification

import static MotherDockerTest.isDockerReachable

@IgnoreIf({ !isDockerReachable() })
class MotherDockerTest extends Specification {

    static final def ORIG = 'src/test/resources/docker-compose/original'

    def 'BuildProjectFromFile - should correctly create and start a simple busybox image'() {
        setup:
        def client = new DefaultDockerClient("unix:///var/run/docker.sock")

        when:
        def project = MotherDocker.buildProjectFromFile("${ORIG}/busybox.yml", client)
        project.start()
        project.stop()

        then:
        notThrown(Exception.class)

        cleanup:
        client.close()
    }

    def 'BuildProjectFromFile - should correctly create and start a simple postgres image'() {
        setup:
        def client = new DefaultDockerClient("unix:///var/run/docker.sock")

        when:
        def project = MotherDocker.buildProjectFromFile("${ORIG}/postgres.yml", client)
        project.start()
        project.stop()

        then:
        notThrown(Exception.class)

        cleanup:
        client.close()
    }

    public static def isDockerReachable() {
        try {
            def info = new DefaultDockerClient("unix:///var/run/docker.sock").info()
            return info != null
        } catch (Exception ignored) {
            return false
        }
    }

}
