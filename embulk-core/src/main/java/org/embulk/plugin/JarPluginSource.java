package org.embulk.plugin;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.embulk.EmbulkSystemProperties;
import org.embulk.spi.EmbulkPluginFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JarPluginSource implements PluginSource {

    public static JarPluginSource of(final EmbulkSystemProperties embulkSystemProperties) {
        List<EmbulkPluginFactory> factories = new ArrayList<>();

        String paths = embulkSystemProperties.getProperty("jruby_load_path"); // TODO 暫定的に -I を使用。別のオプション名にすべき
        if (paths != null) {
            createFactory(factories, paths);
        }

        String embulkHome = embulkSystemProperties.getProperty("embulk_home");
        if (embulkHome != null) {
            final String pluginsDirName = "plugins"; // FIXME 暫定的にpluginsというディレクトリーにしている
            Path dir = Paths.get(embulkHome, pluginsDirName);
            if (Files.isDirectory(dir)) {
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.forEachOrdered(path -> {
                        if (Files.isDirectory(path)) {
                            createFactory(factories, path.toString());
                        }
                    });
                } catch (Exception e) {
                    logger.warn("'embulk_home/" + pluginsDirName + "/' read error", e);
                }
            }
        }

        return new JarPluginSource(factories);
    }

    private static void createFactory(List<EmbulkPluginFactory> factories, String paths) {
        List<URL> urls = new ArrayList<>();
        String[] ss = paths.split(File.pathSeparator);
        for (String path : ss) {
            try (Stream<Path> stream = Files.walk(Paths.get(path.trim()))) {
                stream.filter(f -> f.toString().endsWith(".jar")).map(f -> {
                    try {
                        return f.toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }).forEachOrdered(urls::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        if (urls.isEmpty()) {
            logger.debug("not found jar. path={}", paths);
        } else {
            URL[] urlArray = urls.toArray(new URL[urls.size()]);
            URLClassLoader classLoader = URLClassLoader.newInstance(urlArray, JarPluginSource.class.getClassLoader());
            ServiceLoader<EmbulkPluginFactory> serviceLoader = ServiceLoader.load(EmbulkPluginFactory.class, classLoader);
            int count = 0;
            for (EmbulkPluginFactory factory : serviceLoader) {
                factories.add(factory);
                count++;
            }
            if (count == 0) {
                logger.debug("not found EmbulkPluginFactory. path={}", paths);
                try {
                    classLoader.close();
                } catch (IOException e) {
                    logger.warn("classLoader close error", e);
                }
            }
        }
    }

    private JarPluginSource(final List<EmbulkPluginFactory> factories) {
        this.factories = factories;
    }

    @Override
    public <T> T newPlugin(final Class<T> pluginInterface, final PluginType pluginType) throws PluginSourceNotMatchException {
        EmbulkPluginFactory factory = findFactory(pluginInterface, pluginType);
        if (factory == null) {
            throw new PluginSourceNotMatchException();
        }

        Class<?> pluginClass = factory.getPluginClass();
        logger.debug("jarPlugin name={}, version={}, pluginClass={}", factory.getName(), factory.getVersion(), pluginClass.getName());
        try {
            Object plugin = pluginClass.newInstance();
            return pluginInterface.cast(plugin);
        } catch (Exception e) {
            throw new PluginSourceNotMatchException(e);
        }
    }

    private EmbulkPluginFactory findFactory(final Class<?> pluginInterface, final PluginType pluginType) {
        List<EmbulkPluginFactory> factoryList = getFactoryList(pluginInterface, pluginType);
        if (factoryList.isEmpty()) {
            return null;
        }

        if (pluginType.getSourceType() == PluginSource.Type.MAVEN) {
            final MavenPluginType mavenPluginType = (MavenPluginType) pluginType;
            String targetVersion = mavenPluginType.getVersion();
            if (targetVersion != null) {
                for (EmbulkPluginFactory factory : factoryList) {
                    if (targetVersion.equals(factory.getVersion())) {
                        return factory;
                    }
                }
                logger.warn("target version nothing. name={}, version={}, loadVersions={}", pluginType.getName(), targetVersion,
                        factoryList.stream().map(EmbulkPluginFactory::getVersion).collect(Collectors.toList()));
                return null;
            }
        }

        return factoryList.get(0);
    }

    private List<EmbulkPluginFactory> getFactoryList(final Class<?> pluginInterface, final PluginType pluginType) {
        String targetName = pluginType.getName();

        Map<String, List<EmbulkPluginFactory>> map = registries.get(pluginInterface);
        if (map != null) {
            List<EmbulkPluginFactory> factoryList = map.get(targetName);
            if (factoryList != null) {
                return factoryList;
            }
        }

        List<EmbulkPluginFactory> factoryList = new ArrayList<>();
        for (EmbulkPluginFactory factory : this.factories) {
            Class<?> pluginClass = factory.getPluginClass();
            if (pluginClass == null) {
                logger.warn("pluginClass is null. factory={}", factory.getClass().getName());
                continue;
            }
            String pluginName = factory.getName();
            if (pluginName == null) {
                logger.warn("pluginName is null. factory={}", factory.getClass().getName());
                continue;
            }
            if (pluginInterface.isAssignableFrom(pluginClass) && pluginName.equals(targetName)) {
                factoryList.add(factory);
            }
        }

        Collections.sort(factoryList, VERSION_COMPARATOR);
        registries.computeIfAbsent(pluginInterface, k -> new LinkedHashMap<>()).put(targetName, factoryList);
        return factoryList;
    }

    private static final Comparator<EmbulkPluginFactory> VERSION_COMPARATOR = new Comparator<EmbulkPluginFactory>() {
        @Override
        public int compare(EmbulkPluginFactory o1, EmbulkPluginFactory o2) {
            int c = -compareVersion(o1.getVersion(), o2.getVersion());
            return c;
        }

        private int compareVersion(String v1, String v2) {
            if (v1 == null) {
                return (v2 == null) ? 0 : 1;
            } else if (v2 == null) {
                return -1;
            }

            String[] ss1 = v1.split(Pattern.quote("."));
            String[] ss2 = v2.split(Pattern.quote("."));
            final int min = Math.min(ss1.length, ss2.length);
            for (int i = 0; i < min; i++) {
                String s1 = ss1[i];
                String s2 = ss2[i];
                int c;
                try {
                    int n1 = toInt(s1);
                    int n2 = toInt(s2);
                    c = Integer.compare(n1, n2);
                    if (c != 0) {
                        return c;
                    }
                } catch (NumberFormatException e) {
                    logger.warn("NumberFormatException version1={}, version2={}", v1, v2, e);
                }
                c = s1.compareTo(s2);
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(ss1.length, ss2.length);
        }

        private int toInt(String s) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (!Character.isDigit(c)) {
                    return Integer.parseInt(s.substring(0, i));
                }
            }
            return Integer.parseInt(s);
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(JarPluginSource.class);

    private final List<EmbulkPluginFactory> factories;
    private final Map<Class<?>, Map<String, List<EmbulkPluginFactory>>> registries = new HashMap<>();
}
