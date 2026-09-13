package com.ldiamond.dli.examples;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Usedthewrongcollection {
    public String findMostCommonString (final Set<String> strings) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String s : strings) {
            if (counts.containsKey(s)) {
                counts.put(s, counts.get(s) + 1);
            } else {
                counts.put(s, 1);
            }
        }
        int max = 0;
        String mostCommon = null;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                mostCommon = entry.getKey();
            }
        }
        return mostCommon;
    }
}
