package com.github.fbertola.motherdocker.utils

class StringUtils {

    public static Map sanitizeStringValues(Map map) {
        return map.inject([:]) { result, v ->
            result[v.key as String] = sanitizeStringValues(v.value)
            result
        } as Map
    }

    public static List sanitizeStringValues(Collection coll) {
        return coll.collect { sanitizeStringValues(it) } as List
    }

    public static String sanitizeStringValues(String str) {
        return str.replaceAll('\\r\\n|\\r|\\n', ' ').toString();
    }

    // Pass-through
    public static def sanitizeStringValues(obj) {
        obj
    }

}
