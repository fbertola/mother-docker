package io.antani.motherdocker.utils

import static java.lang.System.getProperty
import static java.lang.System.getenv

class PathUtils {

    static def expandUser(String path) {
        if (!path.startsWith('~')) {
            return path
        }

        def i = path.indexOf('/')

        if (i != 1) {
            return path
        }

        def home = getProperty('user.home')
        home - ~/\/\u0024/

        return home + path[i..-1]
    }

    static def expandVars(String path) {
        if (!path.contains('$')) {
            return path
        }

        def length = path.length()
        def matcher = path =~ /\$(\w+|\{[^}]*\})/

        while (matcher.find()) {
            def start = matcher.start()
            def end = matcher.end()
            def name = matcher.group(1)

            if (name.startsWith('{') && name.endsWith('}')) {
                name = name[1..-2]
            }

            def envVar = getenv(name)

            if (envVar) {
                def tail = path[end..<length]
                def head = path[0..<start]

                path = head + envVar + tail
            }
        }

        return path
    }

}
