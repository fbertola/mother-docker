package com.github.fbertola.motherdocker.utils

class StringUtils {

    public static Set<String> ensureJavaString(Set coll) {
        return coll.collect { it.toString() } as Set
    }

    public static List<String> ensureJavaString(Collection coll) {
        return coll.collect { it.toString() } as List
    }


    public static Map<String, String> ensureJavaString(Map map) {
        return map.inject([:]) { m, v -> m[v.key.toString()] = v.value.toString() } as Map
    }

}
