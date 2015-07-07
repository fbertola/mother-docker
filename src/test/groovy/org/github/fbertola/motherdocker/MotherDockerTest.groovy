package org.github.fbertola.motherdocker

import com.spotify.docker.client.DefaultDockerClient
import groovy.sql.Sql
import org.codehaus.groovy.runtime.IOGroovyMethods
import spock.lang.IgnoreIf
import spock.lang.Specification

import static org.github.fbertola.motherdocker.MotherDocker.buildProjectFromFile
import static org.github.fbertola.motherdocker.MotherDockerTest.isDockerReachable

class MotherDockerTest extends Specification {

    static final def ORIG = 'src/test/resources/docker-compose/original'

    @IgnoreIf({ !isDockerReachable() })
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

    @IgnoreIf({ !isDockerReachable() })
    def 'BuildProjectFromFile - should correctly create and start a simple Postgres image'() {
        setup:
        def client = new DefaultDockerClient("unix:///var/run/docker.sock")
        def project = buildProjectFromFile("${ORIG}/postgres.yml", client)

        when: 'trying to start a basic Postgres image'
        project.start()

        then: 'nothing bad happened'
        notThrown(Exception.class)

        and: 'ports are exposed'
        println project.getPortMappings()

        and: 'Postgres is reachable'
        assert isPostgresReachable()

        cleanup:
        project.stop()
        client.close()
    }

    @IgnoreIf({ !isDockerReachable() })
    def 'BuildProjectFromFile - should correctly build a simple Nginx image'() {
        setup:

        def client = new DefaultDockerClient("unix:///var/run/docker.sock")
        def project = buildProjectFromFile("${ORIG}/nginx.yml", client)

        when: 'trying to start a basic nginx image'
        project.start()

        then: 'nothing bad happened'
        notThrown(Exception.class)

        and: 'ports are exposed'
        println project.getPortMappings()

        and: 'Nginx is reachable'
        assert isNginxReachable()

        cleanup:
        project.stop()
        client.close()
    }

    private static boolean isNginxReachable() {
        def reachable = false
        def socket = new Socket()
        def address = new InetSocketAddress('127.0.0.1', 1234)

        socket.withCloseable {
            socket.connect(address, 1000)
            reachable = socket.isConnected()
        }

        return reachable
    }

    private static boolean isPostgresReachable() {
        def sql = Sql.newInstance(
                "jdbc:postgresql://localhost:1234/superduperuser",
                'superduperuser',
                'superdupersecretpassword',
                'org.postgresql.Driver')

        def result = sql.execute('select version();')

        return result.booleanValue()
    }

    public static def isDockerReachable() {
        try {
            def ping = new DefaultDockerClient("unix:///var/run/docker.sock").ping()
            return ping == 'OK'
        } catch (Exception ignored) {
            return false
        }
    }

}
