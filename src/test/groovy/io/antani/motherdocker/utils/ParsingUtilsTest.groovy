package io.antani.motherdocker.utils

import io.antani.motherdocker.exceptions.ParserException
import spock.lang.Requires
import spock.lang.Specification

import static io.antani.motherdocker.utils.ParsingUtils.*
import static java.lang.System.getenv

class ParsingUtilsTest extends Specification {

    def 'ProcessContainerOptions - should correctly process container options'() {
        setup:
        def workingDir = '/home/antani'
        def volumes = ['container', './host:container', '~/host:container']
        def build = ['file.txt', './dir/file.txt', '~/dir/file.txt']
        def labels = ['a=b', 'c=d']
        def dict = [
                'volumes': volumes,
                'build'  : build,
                'labels' : labels
        ]

        when: 'trying to process container options'
        def result = processContainerOptions(dict, workingDir)

        then: 'the options are correctly processed'
        resolveHostPaths(volumes, workingDir) == (result['volumes'] as Collection)
        resolveBuildPath(build, workingDir) == result['build']
        parseLabels(labels) == result['labels']
    }

    def 'ProcessContainerOptions - should throw an exception if an invalid key is found'() {
        when: 'trying to process unknown option(s)'
        processContainerOptions(['a': true], '/home/antani')

        then: 'an exception is thrown'
        thrown(ParserException.class)
    }

    def 'ResolveBuildPath - should throw an exception if no working dir is passed'() {
        when: 'no working dir is passed'
        resolveBuildPath('', null)

        then: 'an exception is thrown'
        thrown(ParserException.class)
    }

    def 'ResolveHostPaths - should correctly resolve host paths'() {
        setup:
        def userHome = System.getProperty('user.home')
        def volumes = ['container', './host:container', '~/host:container']

        when: 'trying to expand host paths'
        def resolvedHostPaths = resolveHostPaths(volumes, userHome)

        then: 'the correct result is produces'
        resolvedHostPaths == volumes.collect { v -> resolveHostPath(v, userHome) }
    }

    def 'ResolveHostPaths - should throw an exception if no working dir is passed'() {
        when: 'no working dir is passed'
        resolveHostPaths([], null)

        then: 'an exception is thrown'
        thrown(ParserException.class)
    }

    def 'ResolveHostPath - should correctly resolve host path'() {
        setup:
        def userHome = System.getProperty('user.home')
        System.setProperty('user.home', '/home/antani');

        expect: 'correctly resolved host path'
        resolvedHostPath as String == (resolveHostPath(volume, workingDir) as String)

        cleanup:
        System.setProperty('user.home', userHome)

        where:
        volume             | workingDir     | resolvedHostPath
        'container'        | '/home/antani' | 'container'
        './host:container' | '/home/antani' | '/home/antani/host:container'
        '~/host:container' | '/home/antani' | '/home/antani/host:container'
    }

    def 'MergeServiceDictionaries - should correctly merge two dictionaries'() {
        when: 'trying to merge two dictionaries'
        def result = mergeServiceDictionaries(base, override)

        then: 'the dictionaries are correctly merged'
        result == mergedDictionary

        where:
        base                         | override                    | mergedDictionary
        [:]                          | [:]                         | [:]
        ['volumes': ['antani:1234']] | ['volumes': ['test:4321']]  | ['volumes': mergePathMappings(['antani:1234'], ['test:4321'])]
        ['environment': ['a': 'b']]  | ['environment': ['c': 'd']] | ['environment': mergeLabels(['a': 'b'], ['c': 'd'])]
        ['build': true]              | ['image': true]             | ['image': true]
        ['image': true]              | ['build': true]             | ['build': true]
        ['ports': [1, 2]]            | [:]                         | ['ports': [1, 2]]
        ['ports': [1, 2]]            | ['ports': [3, 4]]           | ['ports': [1, 2, 3, 4]]
        ['dns': true]                | [:]                         | ['dns': [true, null]]
        [:]                          | ['dns': true]               | ['dns': [null, true]]
        ['tty': true]                | [:]                         | ['tty': true]
        [:]                          | ['tty': true]               | ['tty': true]
        ['tty': false]               | ['tty': true]               | ['tty': true]
    }

    def 'ProcessExtendsOptions - should correctly process extends options'() {
        expect: 'correctly processed extends options'
        processExtendsOptions('name', options) == options

        where:
        options << [
                ['service': true],
                ['service': true, 'file': true]
        ]
    }

    def 'ProcessExtendsOptions - should not process ill-formed extends options'() {
        when: 'trying to process incorrect extends options'
        processExtendsOptions('name', options)

        then: 'an exception is thrown'
        thrown(ParserException.class)

        where:
        options << [
                null,
                [],
                [:],
                ['file': true]
        ]
    }

    @Requires({ new File('./src/test/resources/env_vars.properties').exists() && getenv('JAVA_HOME') })
    def 'ResolveEnvironment - should correctly resolve environment'() {
        expect: 'a correctly resolved environment section'
        resolveEnvironment(dict, workingDir) == resolvedDict

        where:
        dict                                                     | workingDir | resolvedDict
        [:]                                                      | '.'        | [:]
        ['a': 'b']                                               | '.'        | ['a': 'b']
        ['env_file': './src/test/resources/env_vars.properties'] | '.'        | ['environment': ['prop1': 'prop1', 'prop2': 'prop2', 'prop3': 'prop3']]
        ['environment': ['JAVA_HOME': null]]                     | '.'        | ['environment': ['JAVA_HOME': getenv('JAVA_HOME')]]
    }

    @Requires({ new File('./src/test/resources/env_vars.properties').exists() })
    def 'EnvVarsFromFile - should correctly load env vars from file'() {
        setup:
        def fileName = './src/test/resources/env_vars.properties'

        when: 'trying to read env vars from file'
        def envVars = envVarsFromFile(fileName)

        then: 'the correct vars are read'
        envVars == [
                'prop1': 'prop1',
                'prop2': 'prop2',
                'prop3': 'prop3'
        ]
    }

    @Requires({ !(new File('./src/test/resources/env_vars.unknown').exists()) })
    def 'EnvVarsFromFile - should fail if file does not exists'() {
        setup:
        def fileName = './src/test/resources/env_vars.unknown'

        when: 'trying to read env vars from a nonexistent file'
        envVarsFromFile(fileName)

        then: 'an exception is thrown'
        thrown(ParserException.class)
    }

    def 'GetEnvFiles - should correctly extract env_file paths'() {
        expect: 'correct env_file path'
        getEnvFiles(options, workingDir) == paths

        where:
        options                                          | workingDir     | paths
        []                                               | '/home/antani' | [:]
        ['env_file': null]                               | '/home/antani' | [:]
        ['env_file': '']                                 | '/home/antani' | [:]
        ['env_file': './test/file.txt']                  | '/home/antani' | ['/home/antani/test/file.txt']
        ['env_file': ['./test/file.txt', '../file.txt']] | '/home/antani' | ['/home/antani/test/file.txt', '/home/file.txt']
    }

    def 'GetEnvFile - should fail if no working dir is specified'() {
        when: 'trying to invoke this method without specifying  a working dir'
        getEnvFiles(['env_file': 'a.txt'], null)

        then: 'an exception is thrown'
        thrown(ParserException.class)
    }

    def 'ValidateExtendedServiceDict - should throws exceptions for invalid extended service dictionaries'() {
        when: 'trying to validate an invalid extended service dictionaries'
        validateExtendedServiceDict(dict, 'filename', 'service')

        then: 'an exception is thrown'
        thrown(ParserException.class)

        where:
        dict << [
                ['a': 'b'],
                ['volumesFrom': true],
                ['links': true],
                ['links': null, 'volumesFrom': true],
                ['links': true, 'volumesFrom': null],
                ['net': 'container:abc']
        ]

    }

    def 'GetServiceNameFromNet - should correctly extract service name'() {
        expect: 'service name'
        getServiceNameFromNet(netConfig) == name

        where:
        netConfig       | name
        null            | null
        ''              | null
        'abc'           | null
        'container:'    | null
        'container:abc' | 'abc'
    }

    def 'MergePathMappings - should merge path mappings'() {
        expect: 'merged path mappings'
        mergePathMappings(base, override) == mergedPathMappings

        where:
        base                    | override        | mergedPathMappings
        ['antani:1234']         | ['test:4321']   | ['antani:1234', 'test:4321']
        ['antani:1234']         | ['test:1234']   | ['test:1234']
        ['1234']                | ['antani:1234'] | ['antani:1234']
        ['1234', 'antani:1234'] | ['test:4321']   | ['antani:1234', 'test:4321']
    }

    def 'PathMappingsFromDict - should correctly create a path mapping from dict'() {
        expect: 'merged labels'
        pathMappingsFromDict(dict) == pathMappings

        where:
        dict                                             | pathMappings
        null                                             | []
        [:]                                              | []
        ['9999': null]                                   | ['9999']
        ['1234': 'antani']                               | ['antani:1234']
        ['1234': 'antani', '4321': 'test', '9999': null] | ['antani:1234', 'test:4321', '9999']
    }

    def 'JoinPathMapping - should correctly join path mappings'() {
        expect: 'joined path mappings'
        joinPathMapping(pair) == joinedPathMapping

        where:
        pair               | joinedPathMapping
        ['1234', 'antani'] | 'antani:1234'
        ['1234', null]     | '1234'
    }

    def 'DictFromPathMappings - should correctly create a dict of path mappings'() {
        expect: 'dict of path mappings'
        dictFromPathMappings(pathMapping) == dict

        where:
        pathMapping                  | dict
        null                         | [:]
        ['antani:1234', 'test:4321'] | ['1234': 'antani', '4321': 'test']
    }

    def 'MergeLabels - should correctly merge labels'() {
        expect: 'merged labels'
        mergeLabels(base, override) == merged

        where:
        base       | override       | merged
        ['a': 'b'] | ['c': 'd']     | ['a': 'b', 'c': 'd']
        ['a': 'b'] | ['a': 'c']     | ['a': 'c']
        ['a': 'b'] | ['c=d', 'e=f'] | ['a': 'b', 'c': 'd', 'e': 'f']
        ['a,b']    | ['c=d', 'e=f'] | ['a,b': '', 'c': 'd', 'e': 'f']
    }

    def 'ParseLabels - should correctly parse well-formed labels'() {
        expect: 'parsed labels'
        parseLabels(labels) == parsedLabels

        where:
        labels         | parsedLabels
        null           | [:]
        ['a': 'b']     | ['a': 'b']
        ['a=b', 'c=d'] | ['a': 'b', 'c': 'd']
    }

    def 'ParseLabels - should not correctly parse ill-formed labels'() {
        when: 'trying to parse an ill-formed label'
        parseLabels('label')

        then: 'an exception is thrown'
        thrown(ParserException.class)
    }

    def 'SplitLabel - should correctly split labels'() {
        expect: 'splitted labels'
        splitLabel(label) == splittedLabels

        where:
        label | splittedLabels
        'a=b' | ['a', 'b']
        'a:b' | ['a:b', '']
    }

    def 'SplitPathMapping - should correctly split path mappings'() {
        expect: 'splitted paths'
        splitPathMapping(string) == pathMapping

        where:
        string        | pathMapping
        'antani:1234' | ['1234', 'antani']
        'antani,1234' | ['antani,1234', null]
    }

    @Requires({ getenv('JAVA_HOME') && !getenv('ANTANI_HOME') })
    def 'ResolveEnvVar - should resolve environment variables'() {
        expect: 'expanded vars'
        resolveEnvVar(key, val) == expandedVars

        where:
        key           | val  | expandedVars
        'a'           | 'b'  | ['a', 'b']
        'ANTANI_HOME' | null | ['ANTANI_HOME', '']
        'JAVA_HOME'   | null | ['JAVA_HOME', getenv('JAVA_HOME')]
    }

    def 'ResolvePath - should correctly resolve paths'() {
        expect: 'resolved path'
        resolvePath(workingDir, path) == resolvedPath

        where:
        workingDir     | path               | resolvedPath
        '/home/antani' | 'test/file.txt'    | '/home/antani/test/file.txt'
        '/home/antani' | './test/file.txt'  | '/home/antani/test/file.txt'
        '/home/antani' | '../test/file.txt' | '/home/test/file.txt'
        '/home/antani' | 'file.txt'         | '/home/antani/file.txt'
        '/home/antani' | './file.txt'       | '/home/antani/file.txt'
        '/home/antani' | '../file.txt'      | '/home/file.txt'
    }
}
