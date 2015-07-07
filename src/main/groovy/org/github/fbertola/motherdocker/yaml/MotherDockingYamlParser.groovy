package org.github.fbertola.motherdocker.yaml

import org.github.fbertola.motherdocker.exceptions.ParserException

import java.nio.file.FileSystems

import static org.github.fbertola.motherdocker.utils.ParsingUtils.*

class MotherDockingYamlParser {

    protected workingDir = null
    protected fileName = null
    protected alreadySeen = []

    MotherDockingYamlParser(workingDir, fileName, alreadySeen = []) {
        this.workingDir = workingDir
        this.fileName = fileName
        this.alreadySeen = alreadySeen
    }

    def makeServiceDictionary(name, serviceDictionary) {
        if (signature(name) in alreadySeen) {
            throw new ParserException("Circular reference for '$name' in $serviceDictionary")
        }

        serviceDictionary = serviceDictionary.clone()
        serviceDictionary['name'] = name
        serviceDictionary = resolveEnvironment(serviceDictionary as Map, workingDir)
        resolveExtends(serviceDictionary)

        processContainerOptions(serviceDictionary, workingDir)
    }

    private def resolveExtends(serviceDictionary) {
        if (!serviceDictionary['extends']) {
            return serviceDictionary
        }

        def extendsOptions = processExtendsOptions(serviceDictionary['name'], serviceDictionary['extends'])
        def otherConfigPath = resolvePath(workingDir as String, extendsOptions['file'] as String)
        def otherWorkingDir = getPathParent(otherConfigPath)
        def otherAlreadySeen = alreadySeen + [signature(serviceDictionary['name'])]
        def otherLoader = new MotherDockingYamlParser(otherWorkingDir, otherConfigPath, otherAlreadySeen)
        def otherConfig = loadYaml(otherConfigPath)
        def otherServiceDict = otherConfig[extendsOptions['service'] as String]

        if (!otherServiceDict) {
            throw new ParserException("File ${otherConfigPath} does not contains '${extendsOptions['service']}' service")
        }

        otherServiceDict = otherLoader.makeServiceDictionary(serviceDictionary['name'], otherServiceDict)
        validateExtendedServiceDict(otherServiceDict, otherConfigPath, extendsOptions['service'])

        mergeServiceDictionaries(otherServiceDict, serviceDictionary)
    }

    private String signature(name) {
        return "$fileName::$name"
    }

    private static String getPathParent(String path) {
        return FileSystems.default.getPath(path).parent.toAbsolutePath()
    }

}
