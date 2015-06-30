package org.github.fbertola.motherdocker

import com.spotify.docker.client.DefaultDockerClient
import groovy.sql.Sql
import spock.lang.IgnoreIf
import spock.lang.Specification

import static MotherDockerTest.isDockerReachable
import static org.github.fbertola.motherdocker.MotherDocker.buildProjectFromFile

@IgnoreIf({ !isDockerReachable() })
class MotherDockerTest extends Specification {

    static final def ORIG = 'src/test/resources/docker-compose/original'

    def 'BuildProjectFromFile - should correctly create and start a simple busybox image'() {
        setup:
        def client = new DefaultDockerClient("unix:///var/run/docker.sock")
        def project = buildProjectFromFile("${ORIG}/busybox.yml", client)

        when: 'trying to start a basic image'
        project.start()

        then: 'nothing bad happened'
        notThrown(Exception.class)

        cleanup:
        project.stop()
        client.close()
    }

    def 'BuildProjectFromFile - should correctly create and start a simple Postgres image'() {
        setup:
        def client = new DefaultDockerClient("unix:///var/run/docker.sock")
        def project = buildProjectFromFile("${ORIG}/postgres.yml", client)

        when: 'trying to start a basic Postgres image'
        project.start()

        then: 'nothing bad happened'
        notThrown(Exception.class)

        and: 'Postgres is reachable'
        postgresIsReachable()

        cleanup:
        project.stop()
        client.close()
    }

    // FIXME: non utilizzare 'sti cazzo di driver
    private static void postgresIsReachable() {
        def sql = Sql.newInstance(
                "jdbc:pgsql://localhost:5432/superduperuser",
                'superduperuser',
                'superdupersecretpassword',
                'com.impossibl.postgres.jdbc.PGDriver')

        def result = sql.execute('select version();')
        println result
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
