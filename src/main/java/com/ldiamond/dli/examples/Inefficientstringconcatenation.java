package com.ldiamond.dli.examples;

public class Inefficientstringconcatenation {
    public String inefficientStringConcatenation(final String whatToAppend, final int howManyTimes) {
        String result = "";
        for (int i = 0; i < howManyTimes; i++) {
            result += whatToAppend;
        }
        return result;
    }        
}

