package org.github.fbertola.motherdocker

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.ImageNotFoundException
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import groovy.util.logging.Slf4j
import org.github.fbertola.motherdocker.exceptions.ServiceException

import java.nio.file.FileSystems

import static com.spotify.docker.client.DockerClient.BuildParameter.QUIET

@Slf4j
class MotherDockingService {

    private static final Integer SECONDS_BEFORE_KILLING = 5

    private String name = ''
    private String containerId = ''
    private DockerClient client = null
    private Map<String, Object> options = [:]

    MotherDockingService(name, client, options) {
        if (!isValidIdentifier(name)) {
            throw new ServiceException("Invalid service name '$name' - only alpha-numerical chars are allowed")
        }

        if ('image' in options && 'build' in options) {
            throw new ServiceException("Service $name has both an inspectImage and build path specified. A service can either be built to inspectImage or use an existing inspectImage, not both.")
        }

        if (!('image' in options) && !('build' in options)) {
            throw new ServiceException("Service $name has neither an inspectImage nor a build path specified. Exactly one must be provided.")
        }

        this.name = name
        this.client = client
        this.options = options
    }

    def getName() {
        return name
    }

    def start() {
        log.info('Starting service \'{}\'', name)

        ensureImageExists()

        createAndRunContainerForImage()

        // TODO: wait for something

        log.info('Done! Service \'{}\' successfully started', name)
    }

    def stop() {
        log.info('Stopping service \'{}\'', name)

        stopAndRemoveContainer()

        log.info('Done! Service \'{}\' successfully stopped', name)
    }

    private void stopAndRemoveContainer() {
        try {
            log.info('Stopping container {}', containerId)
            client.stopContainer(containerId, SECONDS_BEFORE_KILLING)

            log.info('Removing container {}', containerId)
            client.removeContainer(containerId, true)
        } catch (Exception ex) {
            throw new ServiceException(ex)
        }
    }

    private void createAndRunContainerForImage() {
        try {
            log.info('Creating container for service \'{}\'', name)
            def createdContainer = client.createContainer(getContainerConfig())

            containerId = createdContainer.id()
            log.info('Container created: {}', containerId)

            log.info('Starting container {}', containerId)
            client.startContainer(containerId)
        } catch (Exception ex) {
            throw new ServiceException(ex)
        }
    }

    private void ensureImageExists() {
        if (inspectImage()) {
            return
        }

        def imageName = imageName()

        log.info('Image \'{}\' does not exists in the local repository', imageName)

        if (canBeBuilt()) {
            buildImage(imageName)
        } else {
            pullImage(imageName)
        }

        log.info('Done')
    }

    private void pullImage(String imageName) {
        try {
            log.info('Pulling image \'{}\'', imageName)
            client.pull(imageName)
        } catch (Exception ex) {
            throw new ServiceException(ex)
        }
    }

    private void buildImage(String imageName) {
        def buildPath = options['build'] as String

        // FIXME: see better how images are created
        //if (!buildPath.endsWith())

        def path = FileSystems.default.getPath(buildPath)

        log.info('Building image \'{}\'', imageName)
        def imageId = client.build(path, imageName, QUIET)

        log.info('Pushing image \'{}\'', imageName)
        client.push(imageId)
    }

    private def inspectImage() {
        def imageName = imageName()

        try {
            return client.inspectImage(imageName)
        } catch (ImageNotFoundException ignored) {
            return null
        } catch (Exception ex) {
            throw new ServiceException(ex)
        }
    }

    private def createHostConfig() {
        return HostConfig.builder()
                .dns((options['dns'] ?: []) as List)
                .binds((options['binds'] ?: []) as List)
                .dnsSearch((options['dns_search'] ?: []) as List)
                .links((options['links'] ?: []) as List)
                .portBindings(createPortBindings() as Map)
                .privileged((options['privileged'] ?: false) as Boolean)
                .volumesFrom((options['volumes_from'] ?: []) as List)
                .build()
    }

    private def getContainerConfig() {
        def hostConfig = createHostConfig()

        def containerConfig = ContainerConfig.builder()
                .attachStdin(true)
                .attachStderr(true)
                .attachStdout(true)
                .cmd((options['cmd'] ?: []) as List)
                .cpuset(options['cpuset'] as String)
                .cpuShares((options['cpu_shares'] ?: 0) as Long)
                .domainname(options['domainname'] as String)
                .entrypoint((options['entrypoint'] ?: []) as List)
                .env((options['environment'] ?: []) as List)
                .exposedPorts((options['expose'] ?: []) as Set)
                .hostConfig(hostConfig)
                .hostname(options['hostname'] as String)
                .image(imageName())
                .labels((options['labels'] ?: [:]) as Map)
                .macAddress(options['mac_address'] as String)
                .memory((options['mem_limit'] ?: 0) as Long)
                .tty('tty' in options)

        if ('hostname' in options && !('domainname' in options) && '.' in options['hostname']) {
            def parts = (options['hostname'] as String).split('.')
            containerConfig.hostname(parts[0])
            containerConfig.domainname(parts[2])
        }

        return containerConfig.build()
    }

    private def createPortBindings() {
        if ('ports' in options) {
            log.warn('Usage of \'ports\' options is discouraged, consider not using it')
        }

        def bindings = [:]

        (options['ports'] ?: []).each { String port ->
            if (port.contains(':')) {
                def (host, container) = port.split(':')
                bindings[container] = [PortBinding.of('0.0.0.0', host as String)]
            } else {
                bindings[port] = [PortBinding.of('0.0.0.0', port)]
            }
        }

        return bindings
    }

    private String imageName() {
        if (canBeBuilt()) {
            return fullName()
        } else {
            return options['image']
        }
    }

    private def canBeBuilt() {
        return options['build'] != null
    }

    private def fullName() {
        return "MotherDockingBuilt_${name}"
    }

    private static def isValidIdentifier(identifier) {
        return identifier =~ /^[a-zA-Z0-9]+$/
    }
}
