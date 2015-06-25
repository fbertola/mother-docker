package io.antani.motherdocker.yaml

import spock.lang.Specification

import static io.antani.motherdocker.utils.ParsingUtils.loadYaml

class MotherDockingYamlParserTest extends Specification {

    static final def ORIG = 'src/test/resources/docker-compose/original'
    static final def EXP = 'src/test/resources/docker-compose/expected'

    def "MakeServiceDictionary - should make the correct service dictionary"() {
        expect:
        equiv(serviceDictFromFile(filename, workingDir), expected)

        where:
        filename            | workingDir     | expected
        "${ORIG}/test1.yml" | '/home/antani' | loadExpected("${EXP}/test1.map")
        // "${ORIG}/test2.yml" | '/home/antani' | loadExpected("${EXP}/exp2.map")
        // "${ORIG}/test3.yml" | '/home/antani' | loadExpected("${EXP}/exp3.map")
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

    private def equiv(Map a, Map b) {
        assert a.size() == b.size()
        a.each { k, v -> assert equiv(v, b[k]) }
    }

    private def equiv(List a, List b) {
        assert a.size() == b.size()

        def aSorted = a.sort()
        def bSorted = b.sort()

        (0..<a.size()).each { int i -> assert equiv(aSorted[i], bSorted[i]) }
    }

    private def equiv(a, b) {
        a == b
    }
}
