package org.github.fbertola.motherdocker.utils

class StringUtils {


    public static Collection<String> convertToJavaString(Collection coll) {
        return coll.collect { it.toString() }
    }


    public static Map<String, String> convertToJavaString(Map map) {
        return map.inject([:]) { m, v -> m[v.key.toString()] = v.value.toString() } as Map
    }

}
