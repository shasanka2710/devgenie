package com.org.javadoc.ai.generator.util;

import com.org.javadoc.ai.generator.model.ClassDescription;

import java.util.*;

public class GroupByKeys {

    /**
     * Private constructor to prevent instantiation of the utility class.
     */
    private GroupByKeys() {
        // Not called
    }

    public static Map<String, Set<String>> groupByKeys(List<ClassDescription> classDescriptions) {
        Map<String, Set<String>> resultMap = new HashMap<>();
        for (ClassDescription cd : classDescriptions) {
            resultMap.computeIfAbsent(cd.getClassName(), k -> new HashSet<>()).add(cd.getDescription());
        }
        return resultMap;
    }
}
