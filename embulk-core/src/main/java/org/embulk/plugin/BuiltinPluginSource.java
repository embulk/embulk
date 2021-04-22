package org.embulk.plugin;

import com.google.inject.Injector;  // Only for instantiating a plugin.
import java.util.LinkedHashMap;
import java.util.Map;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.FileOutputRunner;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;

public class BuiltinPluginSource implements PluginSource {
    private BuiltinPluginSource(
            final Injector injector,
            final Map<String, Class<? extends DecoderPlugin>> decoderPlugins,
            final Map<String, Class<? extends EncoderPlugin>> encoderPlugins,
            final Map<String, Class<? extends ExecutorPlugin>> executorPlugins,
            final Map<String, Class<? extends FileInputPlugin>> fileInputPlugins,
            final Map<String, Class<? extends FileOutputPlugin>> fileOutputPlugins,
            final Map<String, Class<? extends FilterPlugin>> filterPlugins,
            final Map<String, Class<? extends FormatterPlugin>> formatterPlugins,
            final Map<String, Class<? extends GuessPlugin>> guessPlugins,
            final Map<String, Class<? extends InputPlugin>> inputPlugins,
            final Map<String, Class<? extends OutputPlugin>> outputPlugins,
            final Map<String, Class<? extends ParserPlugin>> parserPlugins) {
        this.injector = injector;
        this.decoderPlugins = decoderPlugins;
        this.encoderPlugins = encoderPlugins;
        this.executorPlugins = executorPlugins;
        this.fileInputPlugins = fileInputPlugins;
        this.fileOutputPlugins = fileOutputPlugins;
        this.filterPlugins = filterPlugins;
        this.formatterPlugins = formatterPlugins;
        this.guessPlugins = guessPlugins;
        this.inputPlugins = inputPlugins;
        this.outputPlugins = outputPlugins;
        this.parserPlugins = parserPlugins;
    }

    public static class Builder {
        private Builder(final Injector injector) {
            this.injector = injector;
            this.decoderPlugins = new LinkedHashMap<>();
            this.encoderPlugins = new LinkedHashMap<>();
            this.executorPlugins = new LinkedHashMap<>();
            this.fileInputPlugins = new LinkedHashMap<>();
            this.fileOutputPlugins = new LinkedHashMap<>();
            this.filterPlugins = new LinkedHashMap<>();
            this.formatterPlugins = new LinkedHashMap<>();
            this.guessPlugins = new LinkedHashMap<>();
            this.inputPlugins = new LinkedHashMap<>();
            this.outputPlugins = new LinkedHashMap<>();
            this.parserPlugins = new LinkedHashMap<>();
        }

        public Builder registerDecoderPlugin(final String name, final Class<? extends DecoderPlugin> decoderImpl) {
            if (null != this.decoderPlugins.putIfAbsent(name, decoderImpl)) {
                throw new IllegalStateException("A decoder plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public Builder registerEncoderPlugin(final String name, final Class<? extends EncoderPlugin> encoderImpl) {
            if (null != this.encoderPlugins.put(name, encoderImpl)) {
                throw new IllegalStateException("An encoder plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public Builder registerExecutorPlugin(final String name, final Class<? extends ExecutorPlugin> executorImpl) {
            if (null != this.executorPlugins.put(name, executorImpl)) {
                throw new IllegalStateException("An executor plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public Builder registerFileInputPlugin(final String name, final Class<? extends FileInputPlugin> fileInputImpl) {
            if (this.inputPlugins.containsKey(name)) {
                throw new IllegalStateException("An input plugin with a name \"" + name + "\" is already registered.");
            } else if (null != this.fileInputPlugins.put(name, fileInputImpl)) {
                throw new IllegalStateException("A file input plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public Builder registerFileOutputPlugin(final String name, final Class<? extends FileOutputPlugin> fileOutputImpl) {
            if (this.outputPlugins.containsKey(name)) {
                throw new IllegalStateException("An output plugin with a name \"" + name + "\" is already registered.");
            } else if (null != this.fileOutputPlugins.put(name, fileOutputImpl)) {
                throw new IllegalStateException("A file output plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public Builder registerFilterPlugin(final String name, final Class<? extends FilterPlugin> filterImpl) {
            if (null != this.filterPlugins.put(name, filterImpl)) {
                throw new IllegalStateException("A filter plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public Builder registerFormatterPlugin(final String name, final Class<? extends FormatterPlugin> formatterImpl) {
            if (null != this.formatterPlugins.put(name, formatterImpl)) {
                throw new IllegalStateException("A formatter plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public Builder registerGuessPlugin(final String name, final Class<? extends GuessPlugin> guessImpl) {
            if (null != this.guessPlugins.put(name, guessImpl)) {
                throw new IllegalStateException("A guess plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public Builder registerInputPlugin(final String name, final Class<? extends InputPlugin> inputImpl) {
            if (this.fileInputPlugins.containsKey(name)) {
                throw new IllegalStateException("A file input plugin with a name \"" + name + "\" is already registered.");
            } else if (this.fileInputPlugins.containsKey(name) || null != this.inputPlugins.put(name, inputImpl)) {
                throw new IllegalStateException("An input plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public Builder registerOutputPlugin(final String name, final Class<? extends OutputPlugin> outputImpl) {
            if (this.fileOutputPlugins.containsKey(name)) {
                throw new IllegalStateException("A file output plugin with a name \"" + name + "\" is already registered.");
            } else if (null != this.outputPlugins.put(name, outputImpl)) {
                throw new IllegalStateException("An output plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public Builder registerParserPlugin(final String name, final Class<? extends ParserPlugin> parserImpl) {
            if (null != this.parserPlugins.put(name, parserImpl)) {
                throw new IllegalStateException("A parser plugin with a name \"" + name + "\" is already registered.");
            }
            return this;
        }

        public BuiltinPluginSource build() {
            return new BuiltinPluginSource(
                    this.injector,
                    this.decoderPlugins,
                    this.encoderPlugins,
                    this.executorPlugins,
                    this.fileInputPlugins,
                    this.fileOutputPlugins,
                    this.filterPlugins,
                    this.formatterPlugins,
                    this.guessPlugins,
                    this.inputPlugins,
                    this.outputPlugins,
                    this.parserPlugins);
        }

        private final Injector injector;
        private final LinkedHashMap<String, Class<? extends DecoderPlugin>> decoderPlugins;
        private final LinkedHashMap<String, Class<? extends EncoderPlugin>> encoderPlugins;
        private final LinkedHashMap<String, Class<? extends ExecutorPlugin>> executorPlugins;
        private final LinkedHashMap<String, Class<? extends FileInputPlugin>> fileInputPlugins;
        private final LinkedHashMap<String, Class<? extends FileOutputPlugin>> fileOutputPlugins;
        private final LinkedHashMap<String, Class<? extends FilterPlugin>> filterPlugins;
        private final LinkedHashMap<String, Class<? extends FormatterPlugin>> formatterPlugins;
        private final LinkedHashMap<String, Class<? extends GuessPlugin>> guessPlugins;
        private final LinkedHashMap<String, Class<? extends InputPlugin>> inputPlugins;
        private final LinkedHashMap<String, Class<? extends OutputPlugin>> outputPlugins;
        private final LinkedHashMap<String, Class<? extends ParserPlugin>> parserPlugins;
    }

    public static Builder builder(final Injector injector) {
        return new Builder(injector);
    }

    @Override
    public <T> T newPlugin(final Class<T> pluginInterface, final PluginType type) throws PluginSourceNotMatchException {
        if (type.getSourceType() != PluginSource.Type.DEFAULT) {
            throw new PluginSourceNotMatchException();
        }
        final String name = type.getName();

        if (InputPlugin.class.isAssignableFrom(pluginInterface)) {
            // Duplications between Input Plugins and File Input Plugins are rejected when registered above.
            final Class<? extends InputPlugin> inputPluginImpl = this.inputPlugins.get(name);
            if (inputPluginImpl != null) {
                return pluginInterface.cast((InputPlugin) this.injector.getInstance(inputPluginImpl));
            }
            final Class<? extends FileInputPlugin> fileInputPluginImpl = this.fileInputPlugins.get(name);
            if (fileInputPluginImpl != null) {
                return pluginInterface.cast(new FileInputRunner((FileInputPlugin) this.injector.getInstance(fileInputPluginImpl)));
            }
        } else if (OutputPlugin.class.isAssignableFrom(pluginInterface)) {
            // Duplications between Output Plugins and File Output Plugins are rejected when registered above.
            final Class<? extends OutputPlugin> outputPluginImpl = this.outputPlugins.get(name);
            if (outputPluginImpl != null) {
                return pluginInterface.cast((OutputPlugin) this.injector.getInstance(outputPluginImpl));
            }
            final Class<? extends FileOutputPlugin> fileOutputPluginImpl = this.fileOutputPlugins.get(name);
            if (fileOutputPluginImpl != null) {
                return pluginInterface.cast(new FileOutputRunner((FileOutputPlugin) this.injector.getInstance(fileOutputPluginImpl)));
            }
        } else {
            final Class<?> impl;
            if (DecoderPlugin.class.isAssignableFrom(pluginInterface)) {
                impl = this.decoderPlugins.get(name);
            } else if (EncoderPlugin.class.isAssignableFrom(pluginInterface)) {
                impl = this.encoderPlugins.get(name);
            } else if (ExecutorPlugin.class.isAssignableFrom(pluginInterface)) {
                impl = this.executorPlugins.get(name);
            } else if (FilterPlugin.class.isAssignableFrom(pluginInterface)) {
                impl = this.filterPlugins.get(name);
            } else if (FormatterPlugin.class.isAssignableFrom(pluginInterface)) {
                impl = this.formatterPlugins.get(name);
            } else if (GuessPlugin.class.isAssignableFrom(pluginInterface)) {
                impl = this.guessPlugins.get(name);
            } else if (ParserPlugin.class.isAssignableFrom(pluginInterface)) {
                impl = this.parserPlugins.get(name);
            } else {
                throw new PluginSourceNotMatchException("Plugin interface " + pluginInterface + " is not supported.");
            }

            if (impl != null) {
                return pluginInterface.cast(this.injector.getInstance(impl));
            }
        }
        throw new PluginSourceNotMatchException();
    }

    private final Injector injector;
    private final Map<String, Class<? extends DecoderPlugin>> decoderPlugins;
    private final Map<String, Class<? extends EncoderPlugin>> encoderPlugins;
    private final Map<String, Class<? extends ExecutorPlugin>> executorPlugins;
    private final Map<String, Class<? extends FileInputPlugin>> fileInputPlugins;
    private final Map<String, Class<? extends FileOutputPlugin>> fileOutputPlugins;
    private final Map<String, Class<? extends FilterPlugin>> filterPlugins;
    private final Map<String, Class<? extends FormatterPlugin>> formatterPlugins;
    private final Map<String, Class<? extends GuessPlugin>> guessPlugins;
    private final Map<String, Class<? extends InputPlugin>> inputPlugins;
    private final Map<String, Class<? extends OutputPlugin>> outputPlugins;
    private final Map<String, Class<? extends ParserPlugin>> parserPlugins;
}
