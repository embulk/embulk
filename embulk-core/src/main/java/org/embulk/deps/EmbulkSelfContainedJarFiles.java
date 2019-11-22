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
 * Represents a pre-defined set of self-contained JAR file resources in the Embulk JAR file.
 *
 * <p>It is intentionally designed to be a singleton. {@link DependencyClassLoader} is introduced only for dependency
 * visibility, not for customizability. It should not be customizable so flexibly.
 */
public final class EmbulkSelfContainedJarFiles {
    private EmbulkSelfContainedJarFiles() {
        // No instantiation.
    }

    public static final class StaticInitializer {
        private StaticInitializer() {
            this.jarFileResources = new EnumMap<>(DependencyCategory.class);
            for (final DependencyCategory category : DependencyCategory.values()) {
                this.jarFileResources.put(category, new ArrayList<>());
            }
        }

        public StaticInitializer addJarFileResource(final DependencyCategory category, final String resource) {
            this.jarFileResources.get(category).add(resource);
            return this;
        }

        public StaticInitializer addJarFileResources(final DependencyCategory category, final Collection<String> resources) {
            this.jarFileResources.get(category).addAll(resources);
            return this;
        }

        @Deprecated
        public StaticInitializer addMavenJarFileResourceName(final String resourceName) {
            return this.addJarFileResource(DependencyCategory.MAVEN, resourceName);
        }

        @Deprecated
        public StaticInitializer addMavenJarFileResourceNames(final Collection<String> resourceNames) {
            return this.addJarFileResources(DependencyCategory.MAVEN, resourceNames);
        }

        @Deprecated
        public StaticInitializer addCliJarFileResourceName(final String resourceName) {
            return this.addJarFileResource(DependencyCategory.CLI, resourceName);
        }

        @Deprecated
        public StaticInitializer addCliJarFileResourceNames(final Collection<String> resourceNames) {
            return this.addJarFileResources(DependencyCategory.CLI, resourceNames);
        }

        public StaticInitializer addFromManifest(final Manifest manifest) {
            final Attributes attributes = manifest.getMainAttributes();
            for (final DependencyCategory category : DependencyCategory.values()) {
                this.addJarFileResources(category, splitAttribute(attributes.getValue(category.getManifestAttributeName())));
            }
            return this;
        }

        public void initialize() {
            initializeAll(jarFileResources);
        }

        private final EnumMap<DependencyCategory, ArrayList<String>> jarFileResources;
    }

    public static StaticInitializer staticInitializer() {
        return new StaticInitializer();
    }

    /**
     * It must be called initially at once before all other getResource*() methods are called.
     *
     * public to be called from org.embulk.cli. Not for plugins. Not guaranteed.
     */
    private static void initializeAll(final EnumMap<DependencyCategory, ArrayList<String>> jarFileResources) {
        synchronized (JAR_RESOURCE_NAMES) {
            if (JAR_RESOURCE_NAMES.isEmpty()) {
                for (final DependencyCategory category : DependencyCategory.values()) {
                    JAR_RESOURCE_NAMES.put(category, jarFileResources.get(category));
                }
            } else {
                throw new LinkageError("Doubly-initialized a set of self-contained JAR files.");
            }
        }
    }

    static Resource getSingleResource(final String targetResourceName, final DependencyCategory category) {
        if (category == null) {
            return null;
        }

        String foundJarResourceName = null;
        Resource resourceToReturn = null;
        for (final String jarResourceName : JAR_RESOURCE_NAMES.get(category)) {
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

    static Collection<Resource> getMultipleResources(final String targetResourceName, final DependencyCategory category) {
        if (category == null) {
            return null;
        }

        final ArrayList<Resource> resourcesToReturn = new ArrayList<>();
        for (final String jarResourceName : JAR_RESOURCE_NAMES.get(category)) {
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
            for (final DependencyCategory category : DependencyCategory.values()) {
                for (final String jarResourceName : JAR_RESOURCE_NAMES.get(category)) {
                    allJarResourceNames.add(jarResourceName);
                }
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

    private static final EnumMap<DependencyCategory, List<String>> JAR_RESOURCE_NAMES = new EnumMap<>(DependencyCategory.class);
}
