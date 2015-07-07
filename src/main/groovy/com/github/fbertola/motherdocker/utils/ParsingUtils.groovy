package com.github.fbertola.motherdocker.utils

import com.github.fbertola.motherdocker.exceptions.ParserException
import org.yaml.snakeyaml.Yaml

import java.nio.file.FileSystems

import static PathUtils.expandUser
import static PathUtils.expandVars
import static java.lang.System.getenv
import static org.apache.commons.io.FileUtils.readLines
import static org.apache.commons.lang3.StringUtils.isNotBlank
import static org.apache.commons.lang3.StringUtils.strip

class ParsingUtils {

    public static final def DOCKER_CONFIG_KEYS = [
            'cap_add',
            'cap_drop',
            'cpu_shares',
            'cpuset',
            'command',
            'detach',
            'devices',
            'dns',
            'dns_search',
            'domainname',
            'entrypoint',
            'env_file',
            'environment',
            'exec',
            'extra_hosts',
            'read_only',
            'hostname',
            'image',
            'labels',
            'links',
            'log_driver',
            'log_message',
            'mac_address',
            'mem_limit',
            'net',
            'pid',
            'ports',
            'privileged',
            'publish_all',
            'restart',
            'security_opt',
            'stdin_open',
            'time',
            'tty',
            'user',
            'volumes',
            'volumesFrom',
            'working_dir'
    ]

    public static final def ALLOWED_KEYS = DOCKER_CONFIG_KEYS + [
            'build',
            'dockerfile',
            'expose',
            'externalLinks',
            'name',
            'wait'
    ]

    public static final def DOCKER_CONFIG_HINTS = [
            'cpu_share' : 'cpu_shares',
            'add_host'  : 'extra_hosts',
            'hosts'     : 'extra_hosts',
            'extra_host': 'extra_hosts',
            'device'    : 'devices',
            'link'      : 'links',
            'port'      : 'ports',
            'privilege' : 'privileged',
            'priviliged': 'privileged',
            'privilige' : 'privileged',
            'volume'    : 'volumes',
            'workdir'   : 'working_dir'
    ]

    static def loadYaml(String yamlFile) {
        def file = new File(yamlFile)

        if (!file.exists()) {
            throw new ParserException("Cannot buildProjectFromFile file: ${yamlFile}")
        }

        return new Yaml().load(new FileReader(file))
    }

    static def processContainerOptions(serviceDictionary, workingDir) {
        serviceDictionary.each { k, v ->
            if (!(k in ALLOWED_KEYS)) {
                def msg = "Unsupported config option for ${serviceDictionary['name']} service: '$k'"

                if (k in DOCKER_CONFIG_HINTS) {
                    msg += " (did you mean '${DOCKER_CONFIG_HINTS[k as String]}'?)"
                }

                throw new ParserException(msg)
            }
        }

        serviceDictionary = serviceDictionary.clone()

        if ('volumes' in serviceDictionary) {
            serviceDictionary['volumes'] = resolveHostPaths(serviceDictionary['volumes'], workingDir)
        }

        if ('build' in serviceDictionary) {
            serviceDictionary['build'] = resolveBuildPath(serviceDictionary['build'], workingDir)
        }

        if ('labels' in serviceDictionary) {
            serviceDictionary['labels'] = parseLabels(serviceDictionary['labels'])
        }

        if ('wait' in serviceDictionary) {
            serviceDictionary['wait'] = validateWaitStrategies(serviceDictionary['wait'] as Map)
        }

        return serviceDictionary
    }

    static def validateWaitStrategies(Map dict) {
        def waitStrategies = ['exec', 'log_message', 'time']

        if (!dict) {
            throw new ParserException('No waiting strategies provided')
        }

        def d = dict.clone()

        d.each { k, v ->
            if (!(k in waitStrategies)) {
                throw new ParserException("Unsupported waiting strategies '${k}'. Available ones: ${waitStrategies}")
            }
        }

        if ('exec' in d && 'log_message' in d) {
            throw new ParserException('\'exec\' and \'log_message\' options cannot be mixed together')
        }

        def time = d['time']

        if (time != null && (time as Long) <= 0) {
            throw new ParserException("Wait time of ${time} is not valid")
        }

        if ('exec' in d && !(d instanceof Collection)) {
            d['exec'] = [d['exec']]
        }

        return d
    }

    static def resolveBuildPath(buildPath, workingDir) {
        if (workingDir == null) {
            throw new ParserException('No workingDir passed to resolveBuildPath()')
        }

        return resolvePath(workingDir as String, buildPath as String)
    }

    static def resolveHostPaths(volumes, workingDir) {
        if (workingDir == null) {
            throw new ParserException('No workingDir passed to resolveHostPaths()')
        }

        return volumes.collect { v -> resolveHostPath(v, workingDir) }
    }

    static def resolveHostPath(volume, workingDir) {
        def (containerPath, hostPath) = splitPathMapping(volume as String)

        if (hostPath != null) {
            hostPath = expandUser(hostPath as String)
            hostPath = expandVars(hostPath as String)

            return "${resolvePath(workingDir as String, hostPath as String)}:$containerPath"
        } else {
            return containerPath
        }
    }

    static def mergeServiceDictionaries(base, override) {
        def dict = base.clone() as Map

        if ('environment' in base || 'environment' in override) {
            dict['environment'] = mergeLabels(
                    base.get('environment'),
                    override.get('environment'),
            )
        }

        def pathMappingKeys = ['volumes', 'devices']

        pathMappingKeys.each { key ->
            if (key in base || key in override) {
                dict[key] = mergePathMappings(base[key], override[key])
            }
        }

        if ('labels' in base || 'labels' in override) {
            dict['labels'] = mergeLabels(
                    base['labels'],
                    override['labels']
            )
        }

        if ('image' in override && 'build' in dict) {
            dict.remove('build')
        }

        if ('build' in override && 'image' in dict) {
            dict.remove('image')
        }

        def listKeys = [
                'ports',
                'expose',
                'externalLinks'
        ]

        listKeys.each { key ->
            if (key in base || key in override) {
                dict[key] = (base[key] ?: []) + (override[key] ?: [])
            }
        }

        def listOrStringKeys = ['dns', 'dns_search']

        listOrStringKeys.each { key ->
            if (key in base || key in override) {
                dict[key] = ([base[key]] + [override[key]]).flatten()
            }
        }

        def alreadyMergedKeys = ['environment', 'labels'] + pathMappingKeys + listKeys + listOrStringKeys
        def remainingKeys = ALLOWED_KEYS.toSet() - alreadyMergedKeys.toSet()

        remainingKeys.each { k ->
            if (k in override) {
                dict[k] = override[k]
            }
        }

        return dict
    }

    static def processExtendsOptions(serviceName, extendsOptions) {
        def errorPrefix = "Invalid 'extends' configuration for $serviceName:"

        if (!(extendsOptions instanceof Map)) {
            throw new ParserException("$errorPrefix must be a dictionary")
        }

        if (!extendsOptions['service']) {
            throw new ParserException("$errorPrefix you need to specify a service, e.g. 'service: web'")
        }

        extendsOptions.each { k, _ ->
            if (!['file', 'service'].contains(k)) {
                throw new ParserException("$errorPrefix unsupported configuration option '$k'")
            }
        }

        return extendsOptions
    }

    static def resolveEnvironment(Map serviceDictionary, workingDir) {
        serviceDictionary = serviceDictionary.clone() as Map

        if (!('environment' in serviceDictionary) && !('env_file' in serviceDictionary)) {
            return serviceDictionary
        }

        def env = [:]

        if ('env_file' in serviceDictionary) {
            getEnvFiles(serviceDictionary, workingDir).each { f ->
                env << envVarsFromFile(f)
            }

            serviceDictionary.remove('env_file')
        }

        env << parseLabels(serviceDictionary['environment'])
        env = env.inject([]) { list, e ->
            def (k, v) = resolveEnvVar(e.key as String, e.value as String)
            list << "${k}=${v}"
            return list
        }

        serviceDictionary['environment'] = env

        return serviceDictionary
    }

    static def envVarsFromFile(filename) {
        def file = new File(filename as String)

        if (!file.exists()) {
            throw new ParserException("Couldn't find env file: $filename")
        }

        def env = [:]

        readLines(file).each { line ->
            line = strip(line)

            if (isNotBlank(line) && !line.startsWith('#')) {
                def (k, v) = splitLabel(line)
                env[k] = v
            }
        }

        return env
    }

    static def getEnvFiles(options, workingDir) {
        if (!('env_file' in options)) {
            return [:]
        }

        if (workingDir == null) {
            throw new ParserException('No workingDir passed to getEnvFiles()')
        }

        def envFiles = options['env_file'] ?: []

        if (!(envFiles instanceof Collection)) {
            envFiles = [envFiles]
        }

        return envFiles.inject([]) { list, path ->
            list << resolvePath(workingDir as String, path as String)
            return list
        }
    }

    static def validateExtendedServiceDict(serviceDict, filename, service) {
        def errorPrefix = "Cannot extend service '$service' in $filename: "

        if (serviceDict['links'] == null) {
            throw new ParserException("$errorPrefix services with 'links' cannot be extended")
        }

        if (serviceDict['volumesFrom'] == null) {
            throw new ParserException("$errorPrefix services with 'volumesFrom' cannot be extended")
        }

        if (serviceDict['net'] != null) {
            if (getServiceNameFromNet(serviceDict['net'] as String) != null) {
                throw new ParserException("$errorPrefix services with 'net: container' cannot be extended")
            }
        }
    }

    static def getServiceNameFromNet(String netConfig) {
        if (netConfig == null) {
            return null
        }

        if (!netConfig.startsWith('container:')) {
            return null
        }

        if (netConfig == 'container:') {
            return null
        }

        return netConfig.split(':')[1]
    }

    static def mergePathMappings(base, override) {
        def d = dictFromPathMappings(base) + dictFromPathMappings(override)
        return pathMappingsFromDict(d)
    }

    static Collection pathMappingsFromDict(Map dict) {
        return dict.inject([]) { list, e ->
            list << joinPathMapping([e.key, e.value])
            return list
        } as Collection
    }

    static String joinPathMapping(pair) {
        def (container, host) = pair

        if (host == null) {
            return container
        } else {
            return "$host:$container"
        }
    }

    static Map dictFromPathMappings(pathMappings) {
        if (pathMappings != null) {
            return pathMappings.inject([:]) { map, e ->
                def (k, v) = splitPathMapping(e as String)
                map[k as String] = v
                return map
            }
        } else {
            return [:]
        }
    }

    static Map mergeLabels(base, override) {
        return parseLabels(base) + parseLabels(override)
    }

    static Map parseLabels(labels) {
        if (labels == null) {
            return [:]
        }

        if (labels instanceof Map) {
            return labels
        }

        if (labels instanceof Collection) {
            return labels.inject([:]) { map, e ->
                def (k, v) = splitLabel(e as String)
                map[k as String] = v
                return map
            } as Map
        }

        throw new ParserException("labels '$labels' must be a list or mapping")
    }

    static Collection splitLabel(String label) {
        if (label.contains('=')) {
            return label.split('=')
        } else {
            return [label, '']
        }
    }

    static Collection splitPathMapping(String string) {
        if (string.contains(':')) {
            def (host, container) = string.split(':')
            return [container, host]
        } else {
            return [string, null]
        }
    }

    static Collection resolveEnvVar(String key, String val) {
        if (val != null) {
            return [key, val]
        } else if (getenv(key) != null) {
            return [key, getenv(key)]
        } else {
            return [key, '']
        }
    }

    static String resolvePath(String workingDir, String path) {
        return FileSystems.default.getPath(workingDir).resolve(path).normalize().toAbsolutePath()
    }

}
