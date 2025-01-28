package com.ldiamond.dli.examples;

public class PetStoreProduct {
    String name;
    String description;
    String displayName;

    public PetStoreProduct(String name, String description, String displayName) {
        this.name = name;
        this.description = description;
        this.displayName = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }
}
