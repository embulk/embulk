package org.embulk.jruby;

import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginSourceNotMatchException;
import org.embulk.plugin.PluginType;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JRubyPluginSource implements PluginSource {
    public static final String PLUGIN_CLASS_LOADER_FACTORY_VARIABLE_NAME = "$temporary_internal_plugin_class_loader_factory__";

    private static final Logger logger = LoggerFactory.getLogger(JRubyPluginSource.class);

    private final ScriptingContainerDelegate jruby;
    private final PluginClassLoaderFactory pluginClassLoaderFactory;

    public JRubyPluginSource(final ScriptingContainerDelegate jruby, final PluginClassLoaderFactory pluginClassLoaderFactory) {
        this.jruby = jruby;
        this.pluginClassLoaderFactory = pluginClassLoaderFactory;
    }

    public <T> T newPlugin(Class<T> iface, PluginType type) throws PluginSourceNotMatchException {
        if (this.jruby == null) {
            throw new PluginSourceNotMatchException(
                    "JRuby is not configured properly. If you are using a RubyGem-based plugin, prepare your own JRuby package, "
                            + "and configure the Embulk system property \"jruby\" with it. "
                            + "For example: \"jruby=file:///your/path/to/jruby-complete-9.1.15.0.jar\"");
        }

        // TODO: Check jruby.getJRubyVersion() to check compatibility issues.

        if (type.getSourceType() != PluginSource.Type.DEFAULT) {
            throw new PluginSourceNotMatchException();
        }

        final String name = type.getName();

        final String category;
        if (InputPlugin.class.isAssignableFrom(iface)) {
            category = "input";
        } else if (OutputPlugin.class.isAssignableFrom(iface)) {
            category = "output";
        } else if (ParserPlugin.class.isAssignableFrom(iface)) {
            category = "parser";
        } else if (FormatterPlugin.class.isAssignableFrom(iface)) {
            category = "formatter";
        } else if (DecoderPlugin.class.isAssignableFrom(iface)) {
            category = "decoder";
        } else if (EncoderPlugin.class.isAssignableFrom(iface)) {
            category = "encoder";
        } else if (FilterPlugin.class.isAssignableFrom(iface)) {
            category = "filter";
        } else if (GuessPlugin.class.isAssignableFrom(iface)) {
            category = "guess";
        } else if (ExecutorPlugin.class.isAssignableFrom(iface)) {
            category = "executor";
        } else {
            // unsupported plugin category
            throw new PluginSourceNotMatchException("Plugin interface " + iface + " is not supported in JRuby");
        }

        synchronized (this.jruby) {
            try {
                this.jruby.put(PLUGIN_CLASS_LOADER_FACTORY_VARIABLE_NAME, this.pluginClassLoaderFactory);
            } catch (final Throwable ex) {
                throw new PluginSourceNotMatchException(ex);
            }

            String methodName = "new_java_" + category;
            try {
                // get Embulk::Plugin
                //this.rubyPluginManager = ((RubyModule) jruby.get("Embulk")).const_get(
                //        RubySymbol.newSymbol(
                //            jruby.getProvider().getRuntime(), "Plugin"));
                final Object rubyPluginManager = jruby.runScriptlet("Embulk::Plugin");
                return jruby.callMethod(rubyPluginManager, methodName, name, iface);
            } catch (Throwable ex) {
                throw new PluginSourceNotMatchException(ex);
            } finally {
                try {
                    this.jruby.remove(PLUGIN_CLASS_LOADER_FACTORY_VARIABLE_NAME);
                } catch (final Throwable ex) {
                    // Pass-through.
                    logger.warn("Failed to remove a Ruby global variable: " + PLUGIN_CLASS_LOADER_FACTORY_VARIABLE_NAME, ex);
                }
            }
        }
    }
}
