package com.github.fbertola.motherdocker.utils

import spock.lang.Requires
import spock.lang.Specification

import static VariablesUtils.expandUser
import static VariablesUtils.expandVars
import static java.lang.System.getenv

class VariablesUtilsTest extends Specification {

    def setup() {
        System.setProperty('user.home', '/home/antani')
    }

    def 'ExpandUser - should expand a ~/ path element'() {
        setup: 'a path starting with ~/'
        def path = '~/test/path'

        when: 'expand user home'
        path = expandUser(path)

        then: 'the path is correctly expanded'
        path == '/home/antani/test/path'
    }

    def 'ExpandUser - should not expand a different user home'() {
        setup: 'a path pointing to a different user home'
        def path = '~user/test/path'

        when: 'expand user home'
        path = expandUser(path)

        then: 'nothing is done'
        path == '~user/test/path'
    }

    def 'ExpandUser - should not expand anything'() {
        setup: 'a path with a ~ not at the beginning'
        def path = '/test/~/path'

        when: 'expand user home'
        path = expandUser(path)

        then: 'nothing is done'
        path == '/test/~/path'
    }

    @Requires({ getenv('JAVA_HOME') })
    def 'ExpandVars - should correctly expand $JAVA_HOME variable'() {
        expect: 'expanded variables'
        expandVars(path) != path

        where: 'a bunch of path-variable combinations'
        path << [
                '$JAVA_HOME/test/path',
                '${JAVA_HOME}/test/path',
                '/test/${JAVA_HOME}/path',
                '/test/$JAVA_HOME/path',
                '/test/path/${JAVA_HOME}',
                '/test/path/$JAVA_HOME',
                '${JAVA_HOME}',
                '$JAVA_HOME'
        ]
    }


    @Requires({ !getenv('ANTANI_VAR') })
    def 'ExpandVars - should not expand non-existent variables'() {
        expect: 'nothing was expanded'
        expandVars(path) == path

        where: 'a bunch of ill path-variable combinations'
        path << [
                '$ANTANI_VAR/test/path',
                '${ANTANI_VAR}/test/path',
                '/test/${ANTANI_VAR}/path',
                '/test/$ANTANI_VAR/path',
                '/test/path/${ANTANI_VAR}',
                '/test/path/$ANTANI_VAR',
                '${ANTANI_VAR}',
                '$ANTANI_VAR'
        ]
    }
}
