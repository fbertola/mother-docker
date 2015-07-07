package com.github.fbertola.motherdocker

import com.spotify.docker.client.DockerClient
import groovy.util.logging.Slf4j
import com.github.fbertola.motherdocker.yaml.MotherDockingYamlParser

import java.nio.file.FileSystems

import static com.github.fbertola.motherdocker.utils.ParsingUtils.loadYaml

@Slf4j
class MotherDocker {

    static def buildProjectFromFile(String filename, DockerClient client) {
        log.info('Reading file {}', filename)

        if (!(new File(filename)).exists()) {
            throw new IllegalArgumentException("File '${filename}' does not exists")
        }

        def workingDir = FileSystems.default.getPath(filename).parent.normalize().toAbsolutePath()
        def parsedServices = fromDictionary(loadYaml(filename), workingDir, filename)

        log.info('Initializing a new project with options: {}',
                [
                        'workingDir': workingDir,
                        'services'  : parsedServices.collect { it['name'] }
                ])

        return new MotherDockingProject(client, parsedServices)
    }

    private static def fromDictionary(dictionary, workingDir, filename) {
        def serviceDictionaries = []

        dictionary.each { serviceName, serviceDict ->
            if (!(serviceDict instanceof Map)) {
                throw new RuntimeException("Service '$serviceName' doesn't have any configuration options. All top level keys in your docker-compose.yml must map to a dictionary of configuration options.")
            }

            def loader = new MotherDockingYamlParser(workingDir, filename)
            serviceDict = loader.makeServiceDictionary(serviceName, serviceDict)
            validatePaths(serviceDict)

            serviceDictionaries << serviceDict
        }

        return serviceDictionaries
    }

    private static def validatePaths(serviceDictionary) {
        if ('build' in serviceDictionary) {
            def buildPath = new File(serviceDictionary['build'] as String)

            if (!buildPath.exists()) {
                throw new RuntimeException("build path $buildPath either does not exist or is not accessible.")
            }
        }
    }

}
