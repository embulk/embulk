package org.embulk.deps;

public enum DependencyCategory {
    BUFFER("Buffer", "Embulk-Resource-Class-Path-Buffer"),
    YAML("Yaml", "Embulk-Resource-Class-Path-Yaml"),
    CLI("CLI", "Embulk-Resource-Class-Path-Cli"),
    MAVEN("Maven", "Embulk-Resource-Class-Path-Maven"),
    ;

    private DependencyCategory(final String name, final String manifestAttributeName) {
        this.name = name;
        this.manifestAttributeName = manifestAttributeName;
    }

    public String getName() {
        return this.name;
    }

    public String getManifestAttributeName() {
        return this.manifestAttributeName;
    }

    private final String name;
    private final String manifestAttributeName;
}
