package org.embulk.plugin;

public abstract class PluginType {
    public static final PluginType LOCAL = DefaultPluginType.create("local");

    /**
     * Constructs {@code PluginType}.
     *
     * The constructor is {@code protected} to be called from subclasses, e.g. {@code DefaultPluginType}.
     */
    protected PluginType(final String source, final String name) {
        this.sourceType = PluginSource.Type.of(source);
        this.name = name;
    }

    public final PluginSource.Type getSourceType() {
        return sourceType;
    }

    public final String getSourceName() {
        return sourceType.toString();
    }

    public final String getName() {
        return name;
    }

    private final PluginSource.Type sourceType;
    private final String name;
}
