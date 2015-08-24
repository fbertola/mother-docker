package com.github.fbertola.motherdocker.utils

import com.sun.security.auth.module.UnixSystem

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

            def envVar = getEnvVariable(name)

            if (envVar) {
                def tail = path[end..<length]
                def head = path[0..<start]

                path = head + envVar + tail
            }
        }

        return path
    }

    static def getEnvVariable(String name) {
        if (name in ['GID', 'GROUPS', 'UID', 'USERNAME']) {
            def unix = new UnixSystem()

            switch (name) {
                case 'GID': return unix.gid
                case 'GROUPS': return unix.groups
                case 'UID': return unix.uid
                case 'USERNAME': return unix.username
            }
        } else {
            return getenv(name)
        }
    }

}
