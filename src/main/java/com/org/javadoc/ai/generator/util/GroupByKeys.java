package com.org.javadoc.ai.generator.util;

import java.util.*;

public class GroupByKeys {

    public static Map<String, List<String>> groupByKeys(List<Map<String, String>> classDescriptions) {
        Map<String, List<String>> groupedMap = new HashMap<>();

        for (Map<String, String> map : classDescriptions) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                groupedMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
        }

        return groupedMap;
    }
}
