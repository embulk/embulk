package org.embulk.plugin.maven;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MavenPluginPaths {
    private MavenPluginPaths(final Path pluginJarPath, final List<Path> pluginDependencyJarPaths) {
        this.pluginJarPath = pluginJarPath;
        if (pluginDependencyJarPaths == null) {
            this.pluginDependencyJarPaths = Collections.emptyList();
        } else {
            this.pluginDependencyJarPaths = Collections.unmodifiableList(pluginDependencyJarPaths);
        }
    }

    public static MavenPluginPaths of(final Path pluginJarPath) {
        return new MavenPluginPaths(pluginJarPath, null);
    }

    public static MavenPluginPaths of(final Path pluginJarPath, final Path... pluginDependencyJarPaths) {
        return new MavenPluginPaths(pluginJarPath, Arrays.asList(pluginDependencyJarPaths));
    }

    public static MavenPluginPaths of(final Path pluginJarPath, final List<Path> pluginDependencyJarPaths) {
        return new MavenPluginPaths(pluginJarPath, pluginDependencyJarPaths);
    }

    public Path getPluginJarPath() {
        return this.pluginJarPath;
    }

    public List<Path> getPluginDependencyJarPaths() {
        return this.pluginDependencyJarPaths;
    }

    private final Path pluginJarPath;
    private final List<Path> pluginDependencyJarPaths;
}
