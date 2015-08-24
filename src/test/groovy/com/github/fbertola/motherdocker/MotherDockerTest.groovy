package com.github.fbertola.motherdocker

import com.github.fbertola.motherdocker.spock.WithDockerConfig
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerInfo
import com.spotify.docker.client.messages.PortBinding
import groovy.sql.Sql
import spock.lang.IgnoreIf
import spock.lang.Specification

import static MotherDockerTest.isDockerReachable

class MotherDockerTest extends Specification {

    static final def NGINX = 'src/test/resources/docker-compose/original/nginx.yml'

    static final def POSTGRES = 'src/test/resources/docker-compose/original/postgres.yml'

    @IgnoreIf({ !isDockerReachable() })
    @WithDockerConfig(filename = MotherDockerTest.POSTGRES)
    def 'BuildProjectFromFile - should correctly create and start a simple Postgres image'() {
        expect: 'ports are exposed'
        ContainerInfo info= servicesInfo['postgres']
        def ports = info.networkSettings().ports()

        assert info
        assert ports.size() == 1

        and: 'Postgres is reachable'
        assert isPostgresReachable(ports)
    }

    @IgnoreIf({ !isDockerReachable() })
    @WithDockerConfig(filename = MotherDockerTest.NGINX)
    def 'BuildProjectFromFile - should correctly build a simple Nginx image'() {
        expect: 'ports are exposed'
        ContainerInfo info= servicesInfo['nginx']
        def ports = info.networkSettings().ports()

        assert info
        assert ports.size() == 1

        and: 'Nginx is reachable'
        assert isNginxReachable(ports)
    }

    static boolean isNginxReachable(Map<String, List<PortBinding>> portMappings) {
        PortBinding binding = portMappings['80'][0]
        String host = binding.hostIp()
        String port = binding.hostPort()

        def reachable = false
        def socket = new Socket()
        def address = new InetSocketAddress(host, port as Integer)

        socket.withCloseable {
            socket.connect(address, 1000)
            reachable = socket.isConnected()
        }

        return reachable
    }

    static boolean isPostgresReachable(Map<String, List<PortBinding>> portMappings) {
        PortBinding binding = portMappings['5432'][0]
        String host = binding.hostIp()
        String port = binding.hostPort()

        def sql = Sql.newInstance(
                "jdbc:postgresql://${host}:${port}/superduperuser",
                'superduperuser',
                'superdupersecretpassword',
                'org.postgresql.Driver')

        def result = sql.execute('select version();')

        return result.booleanValue()
    }

    static def isDockerReachable() {
        try {
            new DefaultDockerClient('unix:///var/run/docker.sock').withCloseable { DockerClient client ->
                return client.ping() == 'OK'
            }
        } catch (Exception ignored) {
            return false
        }
    }

}
