package org.embulk.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Represents a pre-defined set of self-contained JAR file resources in the Embulk JAR file.
 *
 * <p>{@link org.embulk.EmbulkDependencyClassLoader} is introduced only to control the visibility from plugins,
 * not for customizability. It is intentionally designed to be a singleton.
 */
public final class SelfContainedJarFiles {
    private SelfContainedJarFiles() {
        // No instantiation.
    }

    public static final class StaticInitializer {
        private StaticInitializer() {
            this.jarFileResources = new HashMap<>();
        }

        public StaticInitializer addJarFileResources(final String category, final Collection<String> resources) {
            this.jarFileResources.compute(category, (dummy, old) -> {
                final ArrayList<String> targetList = (old != null) ? old : new ArrayList();
                targetList.addAll(resources);
                return targetList;
            });
            return this;
        }

        public StaticInitializer addFromManifest(final Manifest manifest) {
            final Attributes attributes = manifest.getMainAttributes();

            // embulk-core's own self-contained dependencies
            this.addJarFileResources(CORE, splitAttribute(attributes.getValue("Embulk-Resource-Class-Path")));

            // self-contained plugins
            final List<String> pluginNames = splitAttribute(attributes.getValue("Embulk-Plugins"));
            for (final String pluginName : pluginNames) {
                this.addJarFileResources(pluginName, splitAttribute(attributes.getValue("Embulk-Plugin-" + pluginName)));
            }

            return this;
        }

        public void initialize() {
            initializeAll(this.jarFileResources);
        }

        private final HashMap<String, ArrayList<String>> jarFileResources;
    }

    public static StaticInitializer staticInitializer() {
        return new StaticInitializer();
    }

    public static boolean has(final String category) {
        return JAR_RESOURCE_NAMES.containsKey(category);
    }

    /**
     * Returns the JAR manifest of the first JAR resource in the category.
     *
     * <p>This is for self-contained plugins. Plugin's main JAR file (e.g. {@code embulk-input-file.jar}) should be
     * listed first in the {@code Embulk-Plugin-*} (e.g. {@code Embulk-Plugin-embulk-input-file}) attribute of
     * Embulk executable's top-level manifest.
     *
     * <pre>{@code Embulk-Plugin-embulk-decoder-gzip: /lib/embulk-decoder-gzip.jar /lib/commons-compress-1.10.jar}</pre>
     */
    public static Manifest getFirstManifest(final String category) {
        final List<String> jarResourceNames = JAR_RESOURCE_NAMES.get(category);
        if (jarResourceNames.isEmpty()) {
            return null;
        }

        final String firstJarResourceName = jarResourceNames.get(0);
        final SelfContainedJarFile selfContainedJarFile = Holder.INSTANCE.get(firstJarResourceName);
        return selfContainedJarFile.getManifest();
    }

    /**
     * It must be called initially at once before all other getResource*() methods are called.
     *
     * public to be called from org.embulk.cli. Not for plugins. Not guaranteed.
     */
    private static void initializeAll(final HashMap<String, ArrayList<String>> jarFileResources) {
        synchronized (JAR_RESOURCE_NAMES) {
            if (JAR_RESOURCE_NAMES.isEmpty()) {
                for (final Map.Entry<String, ArrayList<String>> entry : jarFileResources.entrySet()) {
                    JAR_RESOURCE_NAMES.put(entry.getKey(), entry.getValue());
                }
            } else {
                throw new LinkageError("Double initialization of self-contained JAR files.");
            }
        }
    }

    static Resource getSingleResource(final String targetResourceName, final String category) {
        if (category == null) {
            throw new NullPointerException("SelfContainedJarFiles.getSingleResources received null.");
        }
        final List<String> jarResourceNames = JAR_RESOURCE_NAMES.get(category);
        if (jarResourceNames == null) {
            throw new IllegalArgumentException(
                    "SelfContainedJarFiles.getSingleResources received unexpected category: " + category);
        }

        String foundJarResourceName = null;
        Resource resourceToReturn = null;
        for (final String jarResourceName : jarResourceNames) {
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

    static Collection<Resource> getMultipleResources(final String targetResourceName, final String category) {
        if (category == null) {
            throw new NullPointerException("SelfContainedJarFiles.getMultipleResources received null.");
        }
        final List<String> jarResourceNames = JAR_RESOURCE_NAMES.get(category);
        if (jarResourceNames == null) {
            throw new IllegalArgumentException(
                    "SelfContainedJarFiles.getMultipleResources received unexpected category: " + category);
        }

        final ArrayList<Resource> resourcesToReturn = new ArrayList<>();
        for (final String jarResourceName : jarResourceNames) {
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
            for (final Map.Entry<String, List<String>> entry : JAR_RESOURCE_NAMES.entrySet()) {
                for (final String jarResourceName : entry.getValue()) {
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

    // Category for embulk-core's own self-contained dependencies.
    public static final String CORE = "$embulk-deps$";

    // Note this is technically mutable -- so that it can be initialized lazily via StaticInitializer.
    private static final HashMap<String, List<String>> JAR_RESOURCE_NAMES = new HashMap<>();
}
