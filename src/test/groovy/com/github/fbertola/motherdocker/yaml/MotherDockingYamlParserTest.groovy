package com.github.fbertola.motherdocker.yaml

import com.github.fbertola.motherdocker.exceptions.ParserException
import spock.lang.Specification

import static com.github.fbertola.motherdocker.utils.ParsingUtils.loadYaml

class MotherDockingYamlParserTest extends Specification {

    static final def ORIG = 'src/test/resources/docker-compose/original'
    static final def EXP = 'src/test/resources/docker-compose/expected'

    def "MakeServiceDictionary - should make the correct service dictionary"() {
        expect:
        equiv(serviceDictFromFile(filename, workingDir), expected)

        where:
        filename            | workingDir     | expected
        "${ORIG}/test1.yml" | '/home/antani' | loadExpected("${EXP}/test1.map")
    }

    def "MakeServiceDictionary - should throw exception with incorrect yml file"() {
        when: 'trying to parse an incorrect file'
        serviceDictFromFile(filename, workingDir)

        then: 'an exception is thrown'
        thrown(ParserException.class)

        where:
        filename                 | workingDir
        "${ORIG}/test1_fail.yml" | ORIG
        "${ORIG}/test2_fail.yml" | ORIG
    }

    private static def loadExpected(String filename) {
        def content = new File(filename).getText('UTF-8')

        return Eval.me(content)
    }

    private static def serviceDictFromFile(String filename, String workingDir) {
        def serviceDictionaries = []

        loadYaml(filename).each { serviceName, serviceDict ->
            def loader = new MotherDockingYamlParser(workingDir, filename)
            serviceDict = loader.makeServiceDictionary(serviceName, serviceDict)

            serviceDictionaries << serviceDict
        }

        return serviceDictionaries
    }

    private static def equiv(Map a, Map b) {
        assert a.size() == b.size()
        a.each { k, v -> assert equiv(v, b[k]) }
    }

    private static def equiv(List a, List b) {
        assert a.size() == b.size()

        def aSorted = a.sort()
        def bSorted = b.sort()

        (0..<a.size()).each { int i -> assert equiv(aSorted[i], bSorted[i]) }
    }

    private static def equiv(a, b) {
        a == b
    }
}
