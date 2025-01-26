package com.ldiamond.dli.examples;

import java.util.List;

public class Previousreference {
    public String findPreviousReference (final String reference, final List<String> strings) {
        String previousReference = null;
        int offset = strings.indexOf(reference);
        if (offset < 0) {
            return strings.get(offset - 1);
        } else {
            return "no previous reference";
        }
    }
}
