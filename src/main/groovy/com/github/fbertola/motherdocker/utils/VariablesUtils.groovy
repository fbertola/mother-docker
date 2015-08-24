package com.github.fbertola.motherdocker.utils

import com.sun.security.auth.module.UnixSystem

import static java.lang.System.getProperty
import static java.lang.System.getenv

class VariablesUtils {

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

    static def expandPathVars(String str) {
        return expandVars(str, { String s -> getenv(s) })
    }

    static def expandEnvVars(String str) {
        return expandVars(str, { String s -> getSpecialEnvVariables(s) })
    }

    private static def expandVars(String str, Closure getEnvVar) {
        if (!str.contains('$')) {
            return str
        }

        def length = str.length()
        def matcher = str =~ /\$(\w+|\{[^}]*\})/

        while (matcher.find()) {
            def start = matcher.start()
            def end = matcher.end()
            def name = matcher.group(1)

            if (name.startsWith('{') && name.endsWith('}')) {
                name = name[1..-2]
            }

            def envVar = getEnvVar(name)

            if (envVar) {
                def tail = str[end..<length]
                def head = str[0..<start]

                str = (head + envVar + tail)
            }
        }

        return str
    }

    static def getSpecialEnvVariables(String str) {
        def unix = new UnixSystem()

        switch (str) {
            case 'GID': return unix.gid
            case 'GROUPS': return unix.groups
            case 'UID': return unix.uid
            case 'USERNAME': return unix.username
            default: return null
        }
    }

}
