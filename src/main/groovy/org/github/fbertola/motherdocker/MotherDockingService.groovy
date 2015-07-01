package org.github.fbertola.motherdocker

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.ImageNotFoundException
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import groovy.util.logging.Slf4j
import org.github.fbertola.motherdocker.exceptions.ServiceException

import java.nio.file.FileSystems

import static com.spotify.docker.client.DockerClient.BuildParameter.FORCE_RM
import static com.spotify.docker.client.DockerClient.ExecParameter.STDERR
import static com.spotify.docker.client.DockerClient.ExecParameter.STDOUT
import static com.spotify.docker.client.DockerClient.ExecStartParameter.DETACH
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.github.fbertola.motherdocker.utils.DockerUtils.*
import static org.github.fbertola.motherdocker.utils.StringUtils.ensureJavaString

@Slf4j
class MotherDockingService {

    private static final Long SECONDS_BEFORE_KILLING = 5
    private static final Long RETRYING_PAUSE_TIME = 1000
    private static final Long MAX_WAIT_TIME = 5000

    private String name = ''
    private String containerId = ''
    private DockerClient client = null
    private Map<String, Object> options = [:]

    private Map<String, Object> systemOptions = [:]

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

        systemOptions['killTime'] = System.getProperty('motherdocker.container.kill.time') ?: SECONDS_BEFORE_KILLING
        systemOptions['retryAfter'] = System.getProperty('motherdocker.exec.retry.pause') ?: RETRYING_PAUSE_TIME
        systemOptions['maxWaitTime'] = System.getProperty('motherdocker.wait.max') ?: MAX_WAIT_TIME
    }

    def getName() {
        return name
    }

    def start() {
        log.info('Starting service \'{}\'', name)

        ensureImageExists()

        createAndRunContainer()

        executeWaitStrategies()

        log.info('Done! Service \'{}\' successfully started', name)
    }

    def stop() {
        log.info('Stopping service \'{}\'', name)

        stopAndRemoveContainer()

        log.info('Done! Service \'{}\' successfully stopped', name)
    }

    def getPortMappings() {
        log.info('Inspecting container {} for service \'{}\'', containerId, name)

        try {
            def info = client.inspectContainer(containerId)
            def portMappings = info.networkSettings().portMapping()

            log.info('Service \'{}\' port mappings: {}', name, portMappings)

            return portMappings
        } catch (Exception ex) {
            throw new ServiceException(ex)
        }
    }

    private def executeWaitStrategies() {
        if ('wait' in options) {
            def waitOptions = options['wait']
            def waitTime = waitOptions['time'] as Long
            def messageLog = waitOptions['log_message'] as String
            def exec = waitOptions['exec'] as String[]

            if (exec) {
                log.info('Waiting for \'{}\' command to successfully terminate', exec)
                return executeExecWaitStrategy(exec, waitTime)
            }

            if (messageLog) {
                log.info('Waiting for \'{}\' to show up in the logs', messageLog)
                return executeMessageLogWaitStrategy(messageLog, waitTime)
            }

            log.info('Waiting for {} milliseconds', waitTime)
            return executeTimeWaitStrategy(waitTime)
        }
    }

    private def executeExecWaitStrategy(String[] exec, Long waitTime = null) {
        try {
            def retryAfter = systemOptions['retryAfter'] as Long
            def maxWaitTime = systemOptions['maxWaitTime'] as Long
            def execId = client.execCreate(containerId, exec, STDOUT, STDERR)

            client.execStart(execId, DETACH)

            waitForExecFuture(client, execId, retryAfter, MILLISECONDS).get(waitTime ?: maxWaitTime, MILLISECONDS)
        } catch (Exception ignored) {
            throw new ServiceException('The process is taking too much time, waiting aborted!')
        }
    }

    private def executeMessageLogWaitStrategy(String message, Long waitTime = null) {
        try {
            def maxWaitTime = systemOptions['maxWaitTime'] as Long
            def logStream = client.logs(
                    containerId,
                    DockerClient.LogsParameter.STDOUT,
                    DockerClient.LogsParameter.STDERR,
                    DockerClient.LogsParameter.FOLLOW)

            waitForLogMessageFuture(logStream, message).get(waitTime ?: maxWaitTime, MILLISECONDS)
        } catch (Exception ignored) {
            throw new ServiceException('The process is taking too much time, waiting aborted!')
        }
    }

    private static def executeTimeWaitStrategy(waitTime) {
        try {
            Thread.sleep(waitTime as Long) // ¯\_(ツ)_/¯
        } catch (Exception ex) {
            throw new ServiceException(ex)
        }
    }

    private void stopAndRemoveContainer() {
        try {
            log.info('Stopping container {}', containerId)
            client.stopContainer(containerId, systemOptions['killTime'] as Integer)

            log.info('Removing container {}', containerId)
            client.removeContainer(containerId, true)
        } catch (Exception ex) {
            throw new ServiceException(ex)
        }
    }

    private void createAndRunContainer() {
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

        if (canBeBuilt()) {
            buildImage(imageName)
        } else {
            pullImage(imageName)
        }

        log.info('Done, now image \'{}\' exists')
    }

    private void pullImage(String imageName) {
        try {
            log.info('Pulling image \'{}\'', imageName)
            client.pull(imageName, progressHandler())
        } catch (Exception ex) {
            throw new ServiceException(ex)
        }
    }

    private void buildImage(String imageName) {
        def progressHandler = progressHandler()
        def buildPath = options['build'] as String
        def path = FileSystems.default.getPath(buildPath)

        try {
            log.info('Building image \'{}\'', imageName)

            def imageId = client.build(path, imageName, progressHandler, FORCE_RM)

            log.info('Image \'{}\' successfully builted: {}', imageName, imageId)
        } catch (Exception ex) {
            throw new ServiceException(ex)
        }
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
        def builder = HostConfig.builder()

        if ('dns' in options)
            builder.dns(ensureJavaString((options['dns']) as List))
        if ('binds' in options)
            builder.binds(ensureJavaString((options['binds']) as List))
        if ('dns_search' in options)
            builder.dnsSearch(ensureJavaString((options['dns_search']) as List))
        if ('links' in options)
            builder.links(ensureJavaString((options['links']) as List))
        if ('ports' in options)
            builder.portBindings(createPortBindings() as Map)
        if ('volumes_from' in options)
            builder.volumesFrom(ensureJavaString((options['volumes_from']) as List))

        builder.privileged((options['privileged'] ?: false) as Boolean)

        return builder.build()
    }

    private def getContainerConfig() {

        def builder = ContainerConfig.builder()
        if ('command' in options)
            builder.cmd(ensureJavaString((options['command']) as List))
        if ('entrypoint' in options)
            builder.entrypoint(ensureJavaString((options['entrypoint']) as List))
        if ('environment' in options)
            builder.env(ensureJavaString((options['environment']) as List))
        if ('expose' in options)
            builder.exposedPorts(ensureJavaString((options['expose']) as Set))
        if ('labels' in options)
            builder.labels(ensureJavaString((options['labels']) as Map))

        builder.attachStdin(true)
                .attachStderr(true)
                .attachStdout(true)
                .cpuset(options['cpuset'] as String)
                .cpuShares(options['cpu_shares'] as Long)
                .domainname(options['domainname'] as String)
                .hostConfig(createHostConfig())
                .hostname(options['hostname'] as String)
                .image(imageName())
                .macAddress(options['mac_address'] as String)
                .memory(options['mem_limit'] as Long)
                .tty('tty' in options)


        if ('hostname' in options && !('domainname' in options) && '.' in options['hostname']) {
            def parts = (options['hostname'] as String).split('.')
            builder.hostname(parts[0]).domainname(parts[2])
        }

        return builder.build()
    }

    private def createPortBindings() {
        def bindings = [:]

        (options['ports'] ?: []).each { String port ->
            if (port.count(':') == 0) {
                bindings[port] = [PortBinding.of('0.0.0.0', port)]
            } else if (port.count(':') == 1) {
                def (String host, String container) = port.split(':')
                bindings[container] = [PortBinding.of('0.0.0.0', host)]
            } else if (port.count(':') == 2) {
                def (String ip, String host, String container) = port.split(':')
                bindings[container] = [PortBinding.of(ip, host)]
            } else {
                throw new ServiceException("Invalid port binding: ${port}")
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
        return "motherdocker_${name}"
    }

    private static def isValidIdentifier(identifier) {
        return identifier =~ /^[a-z0-9-_.]+$/
    }

}
