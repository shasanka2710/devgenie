package com.org.javadoc.ai.generator.util;

import java.util.*;

public class GroupByKeys {

    /**
     * Private constructor to prevent instantiation of the utility class.
     */
    private GroupByKeys() {
        // Not called
    }

    public static Map<String, Set<String>> groupByKeys(List<Map<String, String>> classDescriptions) {
        Map<String, Set<String>> groupedMap = new HashMap<>();
        for (Map<String, String> map : classDescriptions) {
            String className = map.get("className");
            String desc = map.get("description");
            if (className != null && desc != null) {
                groupedMap.computeIfAbsent(className, k -> new LinkedHashSet<>()).add(desc);
            }
        }
        return groupedMap;
    }
}
