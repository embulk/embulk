package org.embulk.deps.maven;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.embulk.plugin.MavenPluginType;

public class MavenPluginPaths {
    private MavenPluginPaths(final MavenPluginType pluginType, final Path pluginJarPath, final List<Path> pluginDependencyJarPaths) {
        this.pluginType = pluginType;
        this.pluginJarPath = pluginJarPath;
        if (pluginDependencyJarPaths == null) {
            this.pluginDependencyJarPaths = Collections.emptyList();
        } else {
            this.pluginDependencyJarPaths = Collections.unmodifiableList(pluginDependencyJarPaths);
        }
    }

    public static MavenPluginPaths of(final MavenPluginType pluginType, final Path pluginJarPath) {
        return new MavenPluginPaths(pluginType, pluginJarPath, null);
    }

    public static MavenPluginPaths of(final MavenPluginType pluginType, final Path pluginJarPath, final Path... pluginDependencyJarPaths) {
        return new MavenPluginPaths(pluginType, pluginJarPath, Arrays.asList(pluginDependencyJarPaths));
    }

    public static MavenPluginPaths of(final MavenPluginType pluginType, final Path pluginJarPath, final List<Path> pluginDependencyJarPaths) {
        return new MavenPluginPaths(pluginType, pluginJarPath, pluginDependencyJarPaths);
    }

    public MavenPluginType getPluginType() {
        return this.pluginType;
    }

    public Path getPluginJarPath() {
        return this.pluginJarPath;
    }

    public List<Path> getPluginDependencyJarPaths() {
        return this.pluginDependencyJarPaths;
    }

    private final MavenPluginType pluginType;
    private final Path pluginJarPath;
    private final List<Path> pluginDependencyJarPaths;
}
