package com.github.fbertola.motherdocker.utils

class StringUtils {

    public static Map sanitizeStrings(Map map) {
        return map.inject([:]) { result, v ->
            result[sanitizeStrings(v.key) as String] = sanitizeStrings(v.value)
            result
        } as Map
    }

    public static List sanitizeStrings(Collection coll) {
        return coll.collect { sanitizeStrings(it) } as List
    }

    public static String sanitizeStrings(String str) {
        return str.replaceAll('\\r\\n|\\r|\\n', ' ').toString();
    }

    // Pass-through
    public static def sanitizeStrings(obj) {
        obj
    }

}
