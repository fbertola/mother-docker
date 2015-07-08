package com.github.fbertola.motherdocker

import com.github.fbertola.motherdocker.spock.WithDockerConfig
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.PortBinding
import groovy.sql.Sql
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Specification

import static MotherDockerTest.isDockerReachable

class MotherDockerTest extends Specification {

    static final def NGINX = 'src/test/resources/docker-compose/original/nginx.yml'

    static final def POSTGRES = 'src/test/resources/docker-compose/original/postgres.yml'

    @Shared
    Map<String, List<PortBinding>> portMappings = [:]

    @IgnoreIf({ !isDockerReachable() })
    @WithDockerConfig(filename = MotherDockerTest.POSTGRES, callback = { portMappings.clear(); portMappings << it })
    def 'BuildProjectFromFile - should correctly create and start a simple Postgres image'() {
        expect: 'ports are exposed'
        def bindings = portMappings['5432']

        assert bindings
        assert bindings.size() == 1
        assert bindings[0].hostPort() == '1234'

        and: 'Postgres is reachable'
        assert isPostgresReachable()
    }

    @IgnoreIf({ !isDockerReachable() })
    @WithDockerConfig(filename = MotherDockerTest.NGINX, callback = { portMappings.clear(); portMappings << it })
    def 'BuildProjectFromFile - should correctly build a simple Nginx image'() {
        expect: 'ports are exposed'
        def bindings = portMappings['80']

        assert bindings
        assert bindings.size() == 1
        assert bindings[0].hostPort() == '1234'

        and: 'Nginx is reachable'
        assert isNginxReachable()
    }

    private boolean isNginxReachable() {
        def binding = portMappings['80'][0]
        def host = binding.hostIp()
        def port = binding.hostPort()

        def reachable = false
        def socket = new Socket()
        def address = new InetSocketAddress(host, port as Integer)

        socket.withCloseable {
            socket.connect(address, 1000)
            reachable = socket.isConnected()
        }

        return reachable
    }

    private boolean isPostgresReachable() {
        def binding = portMappings['5432'][0]
        def host = binding.hostIp()
        def port = binding.hostPort()

        def sql = Sql.newInstance(
                "jdbc:postgresql://${host}:${port}/superduperuser",
                'superduperuser',
                'superdupersecretpassword',
                'org.postgresql.Driver')

        def result = sql.execute('select version();')

        return result.booleanValue()
    }

    private static def isDockerReachable() {
        try {
            new DefaultDockerClient("unix:///var/run/docker.sock").withCloseable { DockerClient client ->
                return client.ping() == 'OK'
            }
        } catch (Exception ignored) {
            return false
        }
    }

}
