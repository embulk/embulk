package org.embulk.deps;

public enum DependencyCategory {
    CLI("CLI", EmbulkSelfContainedJarFiles.Type.CLI),
    MAVEN("Maven", EmbulkSelfContainedJarFiles.Type.MAVEN),
    ;

    private DependencyCategory(final String name, final EmbulkSelfContainedJarFiles.Type selfContainType) {
        this.name = name;
        this.selfContainType = selfContainType;
    }

    public String getName() {
        return this.name;
    }

    public EmbulkSelfContainedJarFiles.Type getSelfContainType() {
        return this.selfContainType;
    }

    private final String name;
    private final EmbulkSelfContainedJarFiles.Type selfContainType;
}
