package org.embulk.deps;

public enum DependencyCategory {
    BUFFER("Buffer", "Embulk-Resource-Class-Path-Buffer", "org.embulk.deps.buffer.classpath"),
    CONFIG("Config", "Embulk-Resource-Class-Path-Config", "org.embulk.deps.config.classpath"),
    GUESS("Guess", "Embulk-Resource-Class-Path-Guess", "org.embulk.deps.guess.classpath"),
    CLI("CLI", "Embulk-Resource-Class-Path-Cli", "org.embulk.deps.cli.classpath"),
    MAVEN("Maven", "Embulk-Resource-Class-Path-Maven", "org.embulk.deps.maven.classpath"),
    ;

    private DependencyCategory(final String name, final String manifestAttributeName, final String systemPropertyName) {
        this.name = name;
        this.manifestAttributeName = manifestAttributeName;
        this.systemPropertyName = systemPropertyName;
    }

    public String getName() {
        return this.name;
    }

    public String getManifestAttributeName() {
        return this.manifestAttributeName;
    }

    public String getSystemPropertyName() {
        return this.systemPropertyName;
    }

    private final String name;
    private final String manifestAttributeName;
    private final String systemPropertyName;
}
