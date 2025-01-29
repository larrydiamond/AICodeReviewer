package com.ldiamond.dli.examples;

public class BookStoreProduct {
    String name;
    String description;
    String displayName;

    public BookStoreProduct(String name, String description, String displayName) {
        this.name = name;
        this.description = displayName;
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




