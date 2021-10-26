package org.embulk.plugin;

import com.google.inject.Injector;
import java.util.Map;
import org.embulk.EmbulkSystemProperties;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginSourceNotMatchException;
import org.embulk.plugin.PluginType;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.FileOutputRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>It replaces {@code InjectedPluginSource} for old embulk-standards plugins which were embedded in an Embulk executable binary.
 */
public class SelfContainedPluginSource implements PluginSource {
    public SelfContainedPluginSource(
            final Injector injector,
            final EmbulkSystemProperties embulkSystemProperties,
            final PluginClassLoaderFactory pluginClassLoaderFactory) {
        this.injector = injector;
        this.embulkSystemProperties = embulkSystemProperties;
        this.registries = SelfContainedPluginRegistry.generateRegistries(embulkSystemProperties, pluginClassLoaderFactory);
    }

    @Override
    public <T> T newPlugin(final Class<T> pluginInterface, final PluginType pluginType) throws PluginSourceNotMatchException {
        if (pluginType.getSourceType() != PluginSource.Type.DEFAULT) {
            throw new PluginSourceNotMatchException();
        }

        final DefaultPluginType defaultPluginType = (DefaultPluginType) pluginType;

        final SelfContainedPluginRegistry registry = this.registries.get(pluginInterface);
        if (registry == null) {
            // unsupported plugin category
            throw new PluginSourceNotMatchException("Plugin interface " + pluginInterface + " is not supported.");
        }
        final String category = registry.getCategory();

        this.rejectTraditionalStandardPluginsFromEmbulkSystemProperties(category, pluginType.getName());

        final Class<?> pluginMainClass = registry.lookup(defaultPluginType);

        final Object pluginMainObject;
        try {
            // Unlike JRubyPluginSource,
            // SelfContainedPluginSource does not have "registration" before creating an instance of the plugin class.
            // FileInputPlugin and FileOutputPlugin are wrapped with FileInputRunner and FileOutputRunner here.
            if (FileInputPlugin.class.isAssignableFrom(pluginMainClass)) {
                final FileInputPlugin fileInputPluginMainObject;
                try {
                    fileInputPluginMainObject = (FileInputPlugin) this.injector.getInstance(pluginMainClass);
                } catch (final ClassCastException ex) {
                    throw new PluginSourceNotMatchException(
                            "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not file-input.",
                            ex);
                }
                pluginMainObject = new FileInputRunner(fileInputPluginMainObject);
            } else if (FileOutputPlugin.class.isAssignableFrom(pluginMainClass)) {
                final FileOutputPlugin fileOutputPluginMainObject;
                try {
                    fileOutputPluginMainObject = (FileOutputPlugin) this.injector.getInstance(pluginMainClass);
                } catch (final ClassCastException ex) {
                    throw new PluginSourceNotMatchException(
                            "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not file-output.",
                            ex);
                }
                pluginMainObject = new FileOutputRunner(fileOutputPluginMainObject);
            } else {
                if (!pluginInterface.isAssignableFrom(pluginMainClass)) {
                    throw new PluginSourceNotMatchException(
                            "Plugin class \"" + pluginMainClass.getName() + "\" is not a valid " + category + " plugin.");
                }
                pluginMainObject = this.injector.getInstance(pluginMainClass);
            }
        } catch (final ExceptionInInitializerError ex) {
            throw new PluginSourceNotMatchException(
                    "Plugin class \"" + pluginMainClass.getName()
                            + "\" is not instantiatable due to exception in initialization.",
                    ex);
        } catch (final SecurityException ex) {
            throw new PluginSourceNotMatchException(
                    "Plugin class \"" + pluginMainClass.getName()
                            + "\" is not instantiatable due to security manager.",
                    ex);
        }

        try {
            return pluginInterface.cast(pluginMainObject);
        } catch (final ClassCastException ex) {
            throw new PluginSourceNotMatchException(
                    "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not " + category + " actually.",
                    ex);
        }
    }

    private void rejectTraditionalStandardPluginsFromEmbulkSystemProperties(
            final String category, final String type) throws PluginSourceNotMatchException {
        switch (category) {
            case "input":
                switch (type) {
                    case "config":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.input.config.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard config input plugin by the Embulk system property "
                                    + "\"standards.input.config.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    case "file":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.input.file.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard local file input plugin by the Embulk system property "
                                    + "\"standards.input.file.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    default:
                        break;
                }
                break;
            case "parser":
                switch (type) {
                    case "csv":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.parser.csv.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard CSV parser plugin by the Embulk system property "
                                    + "\"standards.parser.csv.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    case "json":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.parser.json.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard JSON parser plugin by the Embulk system property "
                                    + "\"standards.parser.json.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    default:
                        break;
                }
                break;
            case "decoder":
                switch (type) {
                    case "gzip":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.decoder.gzip.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard gzip decoder plugin by the Embulk system property "
                                    + "\"standards.decoder.gzip.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    case "bzip2":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.decoder.bzip2.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard bzip2 decoder plugin by the Embulk system property "
                                    + "\"standards.decoder.bzip2.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    default:
                        break;
                }
                break;
            case "output":
                switch (type) {
                    case "file":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.output.file.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard local file output plugin by the Embulk system property "
                                    + "\"standards.output.file.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    case "null":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.output.null.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard null output plugin by the Embulk system property "
                                    + "\"standards.output.null.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    case "stdout":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.output.stdout.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard stdout output plugin by the Embulk system property "
                                    + "\"standards.output.stdout.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    default:
                        break;
                }
                break;
            case "formatter":
                switch (type) {
                    case "csv":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.formatter.csv.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard CSV formatter plugin by the Embulk system property "
                                    + "\"standards.formatter.csv.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    default:
                        break;
                }
                break;
            case "encoder":
                switch (type) {
                    case "gzip":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.encoder.gzip.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard gzip encoder plugin by the Embulk system property "
                                    + "\"standards.encoder.gzip.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    case "bzip2":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.encoder.bzip2.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard bzip2 encoder plugin by the Embulk system property "
                                    + "\"standards.encoder.bzip2.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    default:
                        break;
                }
                break;
            case "filter":
                switch (type) {
                    case "rename":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.filter.rename.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard rename filter plugin by the Embulk system property "
                                    + "\"standards.filter.rename.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    case "remove_columns":
                        if (this.embulkSystemProperties.getPropertyAsBoolean("standards.filter.remove_columns.disabled", false)) {
                            logger.warn(
                                    "Disabling the standard remove columns plugin by the Embulk system property "
                                    + "\"standards.filter.remove_columns.disabled\" is deprecated.");
                            throw new PluginSourceNotMatchException();
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SelfContainedPluginSource.class);

    private final Injector injector;
    private final EmbulkSystemProperties embulkSystemProperties;
    private final Map<Class<?>, SelfContainedPluginRegistry> registries;
}
