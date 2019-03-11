package org.embulk.deps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a set of self-contained JAR file resources in the Embulk JAR file.
 *
 * <p>It is intentionally designed to be a singleton. {@link DependencyClassLoader} is introduced only for dependency
 * visibility, not for customizability. It should not be customizable so flexibly.
 */
public final class EmbulkSelfContainedJarFiles {
    private EmbulkSelfContainedJarFiles() {
        // No instantiation.
    }

    public enum Type {
        NONE,
        MAVEN,
        CLI,
        ;
    }

    public static final class StaticInitializer {
        private StaticInitializer() {
            this.mavenJarFileResourceNames = new ArrayList<>();
            this.cliJarFileResourceNames = new ArrayList<>();
        }

        public StaticInitializer addMavenJarFileResourceName(final String resourceName) {
            this.mavenJarFileResourceNames.add(resourceName);
            return this;
        }

        public StaticInitializer addMavenJarFileResourceNames(final Collection<String> resourceNames) {
            this.mavenJarFileResourceNames.addAll(resourceNames);
            return this;
        }

        public StaticInitializer addCliJarFileResourceName(final String resourceName) {
            this.cliJarFileResourceNames.add(resourceName);
            return this;
        }

        public StaticInitializer addCliJarFileResourceNames(final Collection<String> resourceNames) {
            this.cliJarFileResourceNames.addAll(resourceNames);
            return this;
        }

        public StaticInitializer addFromManifest(final Manifest manifest) {
            final Attributes attributes = manifest.getMainAttributes();
            this.addMavenJarFileResourceNames(splitAttribute(attributes.getValue("Embulk-Resource-Class-Path-Maven")));
            this.addCliJarFileResourceNames(splitAttribute(attributes.getValue("Embulk-Resource-Class-Path-Cli")));
            return this;
        }

        public void initialize() {
            initializeAll(
                    Collections.unmodifiableList(this.mavenJarFileResourceNames),
                    Collections.unmodifiableList(this.cliJarFileResourceNames));
        }

        private final ArrayList<String> mavenJarFileResourceNames;
        private final ArrayList<String> cliJarFileResourceNames;
    }

    public static StaticInitializer staticInitializer() {
        return new StaticInitializer();
    }

    /**
     * It must be called initially at once before all other getResource*() methods are called.
     *
     * public to be called from org.embulk.cli. Not for plugins. Not guaranteed.
     */
    private static void initializeAll(
            final List<String> mavenJarResourceNames,
            final List<String> cliJarResourceNames) {
        synchronized (JAR_RESOURCE_NAMES) {
            if (JAR_RESOURCE_NAMES.isEmpty()) {
                JAR_RESOURCE_NAMES.put(Type.NONE, Collections.unmodifiableList(new ArrayList<String>()));
                JAR_RESOURCE_NAMES.put(Type.MAVEN, Collections.unmodifiableList(new ArrayList<String>(mavenJarResourceNames)));
                JAR_RESOURCE_NAMES.put(Type.CLI, Collections.unmodifiableList(new ArrayList<String>(cliJarResourceNames)));
            } else {
                throw new LinkageError("Doubly-initialized a set of self-contained JAR files.");
            }
        }
    }

    static Resource getSingleResource(final String targetResourceName, final Type type) {
        String foundJarResourceName = null;
        Resource resourceToReturn = null;
        for (final String jarResourceName : JAR_RESOURCE_NAMES.get(type)) {
            final SelfContainedJarFile selfContainedJarFile = Holder.INSTANCE.get(jarResourceName);
            final Resource resourceFound = selfContainedJarFile.getResource(targetResourceName);
            if (resourceFound != null) {
                if (resourceToReturn != null) {
                    throw new LinkageError(String.format("Duplicated resource: '%s' in '%s' v.s. '%s'", targetResourceName, foundJarResourceName, jarResourceName));
                }
                foundJarResourceName = jarResourceName;
                resourceToReturn = resourceFound;
            }
        }
        return resourceToReturn;
    }

    static Collection<Resource> getMultipleResources(final String targetResourceName, final Type type) {
        final ArrayList<Resource> resourcesToReturn = new ArrayList<>();
        for (final String jarResourceName : JAR_RESOURCE_NAMES.get(type)) {
            final SelfContainedJarFile selfContainedJarFile = Holder.INSTANCE.get(jarResourceName);
            final Resource resourceFound = selfContainedJarFile.getResource(targetResourceName);
            if (resourceFound != null) {
                resourcesToReturn.add(resourceFound);
            }
        }
        return resourcesToReturn;
    }

    private static class Holder {  // Initialization-on-demand holder idiom.
        static final Map<String, SelfContainedJarFile> INSTANCE;

        static {
            final HashSet<String> allJarResourceNames = new HashSet<>();
            for (final String jarResourceName : JAR_RESOURCE_NAMES.get(Type.MAVEN)) {
                allJarResourceNames.add(jarResourceName);
            }
            for (final String jarResourceName : JAR_RESOURCE_NAMES.get(Type.CLI)) {
                allJarResourceNames.add(jarResourceName);
            }

            final HashMap<String, SelfContainedJarFile> instanceBuilt = new HashMap<>();
            for (final String jarResourceName : allJarResourceNames) {
                instanceBuilt.put(jarResourceName, new SelfContainedJarFile(jarResourceName));
            }
            INSTANCE = Collections.unmodifiableMap(instanceBuilt);
        }
    }

    private static List<String> splitAttribute(final String value) {
        final ArrayList<String> list = new ArrayList<>();

        if (value != null) {
            final StringTokenizer tokenizer = new StringTokenizer(value);
            while (tokenizer.hasMoreTokens()) {
                list.add(tokenizer.nextToken());
            }
        }

        return list;
    }

    private static final Logger logger = LoggerFactory.getLogger(EmbulkSelfContainedJarFiles.class);

    private static final EnumMap<Type, List<String>> JAR_RESOURCE_NAMES = new EnumMap<>(Type.class);
}
