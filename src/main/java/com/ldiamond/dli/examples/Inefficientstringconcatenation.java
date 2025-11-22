package com.ldiamond.dli.examples;

public class Inefficientstringconcatenation {
    public String inefficientStringConcatenation(final String whatToAppend, final int howManyTimes) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < howManyTimes; i++) {
            result.append(whatToAppend);
        }
        return result.toString();
    }        
}
