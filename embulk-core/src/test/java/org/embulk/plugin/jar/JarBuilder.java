package org.embulk.plugin.jar;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarBuilder {
    public JarBuilder() {
        this.manifest = new Manifest();
        this.entries = new HashMap<String, Path>();
    }

    public void build(final Path pathToExistingFile) throws Exception {
        try (final JarOutputStream output = buildPluginJar(pathToExistingFile, this.manifest)) {
            for (String entryName : new TreeSet<String>(this.entries.keySet())) {
                final Path pathToRealFile = this.entries.get(entryName);
                if (pathToRealFile == null) {
                    final JarEntry entry = new JarEntry(entryName + "/");
                    entry.setMethod(JarEntry.STORED);
                    entry.setSize(0);
                    entry.setCrc(0);
                    output.putNextEntry(entry);
                    output.closeEntry();
                } else {
                    final JarEntry entry = new JarEntry(entryName);
                    output.putNextEntry(entry);
                    Files.copy(pathToRealFile, output);
                    output.closeEntry();
                }
            }
        }
    }

    public void addClass(final Class<?> klass) throws Exception {
        final Path classFileRelativePath = getClassFileRelativePath(klass);
        final Path classFileFullPath = getClassFileFullPath(klass);
        this.addFile(classFileRelativePath.toString(), classFileFullPath);

        Path directoryPath = classFileRelativePath.getParent();
        while (directoryPath != null) {
            this.addDirectoryIfAbsent(directoryPath.toString());
            directoryPath = directoryPath.getParent();
        }
    }

    public void addManifestV0(final String embulkPluginMainClass) {
        final Attributes attributes = this.manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue(MANIFEST_PLUGIN_SPI_VERSION, "0");
        attributes.putValue(MANIFEST_PLUGIN_MAIN_CLASS, embulkPluginMainClass);
    }

    private void addDirectoryIfAbsent(final String name) {
        if (!(this.entries.containsKey(name))) {
            this.entries.put(name, null);
        }
    }

    private void addFile(final String name, final Path pathToRealFile) {
        this.entries.put(name, pathToRealFile);
    }

    private JarOutputStream buildPluginJar(final Path pathToExistingFile, final Manifest embeddedManifest)
            throws Exception {
        return new JarOutputStream(Files.newOutputStream(pathToExistingFile), embeddedManifest);
    }

    private Path getClassFileRelativePath(final Class<?> klass) {
        return Paths.get(klass.getName().replace('.', '/') + ".class");
    }

    private Path getClassFileFullPath(final Class<?> klass) throws Exception {
        return Paths.get(klass.getClassLoader().getResource(klass.getName().replace('.', '/') + ".class").toURI());
    }

    private final Manifest manifest;
    private final HashMap<String, Path> entries;

    private static final String MANIFEST_PLUGIN_SPI_VERSION = "Embulk-Plugin-Spi-Version";
    private static final String MANIFEST_PLUGIN_MAIN_CLASS = "Embulk-Plugin-Main-Class";
}
