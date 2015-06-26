package org.github.fbertola.motherdocker

import com.spotify.docker.client.DockerClient
import org.github.fbertola.motherdocker.yaml.MotherDockingYamlParser

import java.nio.file.FileSystems

import static org.github.fbertola.motherdocker.utils.ParsingUtils.loadYaml

class MotherDocker {

    static def buildProjectFromFile(String filename, DockerClient client) {
        def workingDir = FileSystems.default.getPath(filename).parent.normalize().toAbsolutePath()
        def parsedServices = fromDictionary(loadYaml(filename), workingDir, filename)

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
