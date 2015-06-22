package io.antani.motherdocker

import io.antani.motherdocker.yaml.MotherDockerYamlParser

import java.nio.file.FileSystems

import static io.antani.motherdocker.utils.ParsingUtils.loadYaml

class MotherDocker {

    static def load(String filename) {
        def workingDir = FileSystems.default.getPath(filename).parent.normalize().toAbsolutePath()
        def parsedServices = fromDictionary(loadYaml(filename), workingDir, filename)

        return new MotherDockerProject(parsedServices)
    }

    private static def fromDictionary(dictionary, workingDir, filename) {
        def serviceDictionaries = []

        dictionary.each { serviceName, serviceDict ->
            if (!(serviceDict instanceof Map)) {
                throw new RuntimeException("Service '$serviceName' doesn't have any configuration options. All top level keys in your docker-compose.yml must map to a dictionary of configuration options.")
            }

            def loader = new MotherDockerYamlParser(workingDir, filename)
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
