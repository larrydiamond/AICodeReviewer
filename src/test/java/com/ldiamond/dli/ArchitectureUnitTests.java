package com.ldiamond.dli;

import org.junit.jupiter.api.Test;
import java.util.Arrays;

import com.ldiamond.archunittest.ArchitectureRule;
import com.ldiamond.archunittest.ArchitectureUnitTest;

public class ArchitectureUnitTests {
       
    @Test
    public void runArchitectureTests() {
        ArchitectureUnitTest.testArchitecture(
            Arrays.asList(ArchitectureRule.ARCHUNIT_NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS, ArchitectureRule.USE_CONCURRENTSKIPLISTMAP_INSTEAD_OF_TREEMAP),
            "com.ldiamond.dli");
    }

}
