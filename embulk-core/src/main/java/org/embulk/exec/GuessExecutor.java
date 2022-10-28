package org.embulk.exec;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.plugin.DefaultPluginType;
import org.embulk.plugin.PluginType;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferImpl;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecAction;
import org.embulk.spi.ExecInternal;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.FileInput;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.Schema;

public class GuessExecutor {
    private final List<PluginType> defaultGuessPlugins;

    private interface GuessExecutorTask extends Task {
        @Config("guess_plugins")
        @ConfigDefault("[]")
        public List<PluginType> getGuessPlugins();

        @Config("exclude_guess_plugins")
        @ConfigDefault("[]")
        public List<PluginType> getExcludeGuessPlugins();

        @Config("guess_sample_buffer_bytes")
        @ConfigDefault("32768") // 32 * 1024
        public int getSampleBufferBytes();
    }

    // Used by FileInputRunner#guess(..)
    public static ConfigSource createSampleBufferConfigFromExecConfig(ConfigSource execConfig) {
        final GuessExecutorTask execTask = loadGuessExecutorTask(execConfig);
        return Exec.newConfigSource().set("sample_buffer_bytes", execTask.getSampleBufferBytes());
    }

    public GuessExecutor(final EmbulkSystemProperties embulkSystemProperties) {
        final String defaultGuessPlugins = embulkSystemProperties.getProperty("default_guess_plugins", null);
        final String guessPlugins = embulkSystemProperties.getProperty("guess_plugins", null);

        final ArrayList<PluginType> guessPluginsBuilt = new ArrayList<>();

        if (defaultGuessPlugins != null && !defaultGuessPlugins.isEmpty()) {
            for (final String defaultGuessPlugin : defaultGuessPlugins.split(",")) {
                guessPluginsBuilt.add(DefaultPluginType.create(defaultGuessPlugin));
            }
        }

        if (guessPlugins != null && !guessPlugins.isEmpty()) {
            for (final String guessPlugin : guessPlugins.split(",")) {
                guessPluginsBuilt.add(DefaultPluginType.create(guessPlugin));
            }
        }

        this.defaultGuessPlugins = Collections.unmodifiableList(guessPluginsBuilt);
    }

    public ConfigDiff guess(ExecSessionInternal exec, final ConfigSource config) {
        try {
            return ExecInternal.doWith(exec, new ExecAction<ConfigDiff>() {
                    public ConfigDiff run() {
                        try (SetCurrentThreadName dontCare = new SetCurrentThreadName("guess")) {
                            return doGuess(config);
                        }
                    }
                });
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            }
            if (ex.getCause() instanceof Error) {
                throw (Error) ex.getCause();
            }
            throw new RuntimeException(ex.getCause());
        }
    }

    protected InputPlugin newInputPlugin(ConfigSource inputConfig) {
        return ExecInternal.newPlugin(InputPlugin.class, inputConfig.get(PluginType.class, "type"));
    }

    private ConfigDiff doGuess(ConfigSource config) {
        ConfigSource inputConfig = config.getNested("in");
        ConfigSource execConfig = config.getNestedOrGetEmpty("exec");

        InputPlugin input = newInputPlugin(inputConfig);

        ConfigDiff inputGuessed;
        if (input instanceof ConfigurableGuessInputPlugin) {
            inputGuessed = ((ConfigurableGuessInputPlugin) input).guess(execConfig, inputConfig);
        } else {
            try {
                inputGuessed = input.guess(inputConfig);
            } catch (AbstractMethodError ex) {
                // for backward compatibility with embulk v0.4 interface
                throw new UnsupportedOperationException(input.getClass().getSimpleName() + ".guess(ConfigSource) is not implemented. This input plugin does not support guessing.");
            }
        }

        ConfigDiff wrapped = Exec.newConfigDiff();
        wrapped.getNestedOrSetEmpty("in").merge(inputGuessed);
        return wrapped;
    }

    // called by FileInputRunner
    public ConfigDiff guessParserConfig(Buffer sample, ConfigSource inputConfig, ConfigSource execConfig) {
        List<PluginType> guessPlugins = new ArrayList<PluginType>(defaultGuessPlugins);

        final GuessExecutorTask task = loadGuessExecutorTask(execConfig);
        guessPlugins.addAll(task.getGuessPlugins());
        guessPlugins.removeAll(task.getExcludeGuessPlugins());
        final int guessParserSampleBufferBytes = task.getSampleBufferBytes();

        return guessParserConfig(sample, inputConfig, guessPlugins, guessParserSampleBufferBytes);
    }

    private ConfigDiff guessParserConfig(Buffer sample, ConfigSource config, List<PluginType> guessPlugins, final int guessParserSampleBufferBytes) {
        // repeat guessing upto 10 times
        ConfigDiff lastGuessed = Exec.newConfigDiff();
        for (int i = 0; i < 10; i++) {
            // include last-guessed config to run guess input
            ConfigSource originalConfig = config.deepCopy().merge(lastGuessed);
            ConfigSource guessInputConfig = originalConfig.deepCopy();
            guessInputConfig.getNestedOrSetEmpty("parser")
                    .set("type", "system_guess")  // override in.parser.type so that FileInputRunner.run uses GuessParserPlugin
                    .set("guess_plugins", guessPlugins)
                    .set("orig_config", originalConfig)
                    .set("guess_parser_sample_buffer_bytes", guessParserSampleBufferBytes);

            // run FileInputPlugin
            final FileInputRunner input = new FileInputRunner(new BufferFileInputPlugin(sample));
            ConfigDiff guessed;
            try {
                input.transaction(guessInputConfig, new InputPlugin.Control() {
                        public List<TaskReport> run(TaskSource inputTaskSource, Schema schema, int taskCount) {
                            if (taskCount == 0) {
                                throw new NoSampleException("No input files to guess");
                            }
                            input.run(inputTaskSource, null, 0, new PageOutput() {
                                    @Override
                                    public void add(Page page) {
                                        throw new RuntimeException("Input plugin must be a FileInputPlugin to guess parser configuration");  // TODO exception class
                                    }

                                    @Override
                                    public void finish() {}

                                    @Override
                                    public void close() {}
                                });
                            throw new AssertionError("Guess executor must throw GuessedNoticeError");
                        }
                    });

                throw new AssertionError("Guess executor must throw GuessedNoticeError");

            } catch (GuessedNoticeError error) {
                guessed = lastGuessed.deepCopy().merge(error.getGuessedConfig());
            }

            // merge to the last-guessed config
            if (lastGuessed.equals(guessed)) {
                // not changed
                return lastGuessed;
            }
            lastGuessed = guessed;
        }

        return lastGuessed;
    }

    public static class GuessParserPlugin implements ParserPlugin {
        private interface PluginTask extends Task {
            @Config("guess_plugins")
            public List<PluginType> getGuessPluginTypes();

            @Config("orig_config")
            public ConfigSource getOriginalConfig();

            @Config("guess_parser_sample_buffer_bytes")
            public int getGuessParserSampleBufferBytes();
        }

        @Override
        public void transaction(ConfigSource config, ParserPlugin.Control control) {
            final PluginTask task = loadPluginTask(config);
            control.run(task.dump(), null);
        }

        @Override
        public void run(TaskSource taskSource, Schema schema, FileInput input, PageOutput pageOutput) {
            final PluginTask task = loadPluginTaskFromTaskSource(taskSource);
            final ConfigSource originalConfig = task.getOriginalConfig();
            final int guessParserSampleBufferBytes = task.getGuessParserSampleBufferBytes();

            // get sample buffer
            Buffer sample = readSample(input, guessParserSampleBufferBytes);

            // load guess plugins
            ImmutableList.Builder<GuessPlugin> builder = ImmutableList.builder();
            for (PluginType guessType : task.getGuessPluginTypes()) {
                GuessPlugin guess = ExecInternal.newPlugin(GuessPlugin.class, guessType);
                builder.add(guess);
            }
            List<GuessPlugin> guesses = builder.build();

            // run guess plugins
            ConfigSource mergedConfig = originalConfig.deepCopy();
            ConfigDiff mergedGuessed = Exec.newConfigDiff();
            for (int i = 0; i < guesses.size(); i++) {
                ConfigDiff guessed = guesses.get(i).guess(originalConfig, sample);
                guessed = addAssumedDecoderConfigs(originalConfig, guessed);
                mergedGuessed.merge(guessed);
                mergedConfig.merge(mergedGuessed);
                if (!mergedConfig.equals(originalConfig)) {
                    // config updated
                    throw new GuessedNoticeError(mergedGuessed);
                }
            }
            throw new GuessedNoticeError(mergedGuessed);
        }

        @Override
        public TaskReport runWithResult(final TaskSource taskSource, final Schema schema, final FileInput input, final PageOutput output)
        {
            return null;
        }

        private static Buffer readSample(FileInput fileInput, int sampleSize) {
            Buffer sample = BufferImpl.allocate(sampleSize);
            try {
                SamplingParserPlugin.readSample(fileInput, sample, 0, sampleSize);
            } catch (RuntimeException ex) {
                // ignores exceptions because FileDecoderPlugin can throw exceptions
                // such as "Unexpected end of ZLIB input stream" if decoder plugin
                // is wrongly guessed.
            }
            if (sample.limit() > 0) {
                return sample;
            }
            throw new NoSampleException("No input buffer to guess");
        }

        private static class ConfigSourceList extends ArrayList<ConfigSource> {}

        private static ConfigDiff addAssumedDecoderConfigs(ConfigSource originalConfig, ConfigDiff guessed) {
            List<ConfigSource> guessedDecoders = guessed.get(ConfigSourceList.class, "decoders", null);
            if (guessedDecoders == null) {
                return guessed;
            } else {
                List<ConfigSource> assumedDecoders = originalConfig.get(ConfigSourceList.class, "decoders", new ConfigSourceList());
                ImmutableList.Builder<ConfigSource> added = ImmutableList.builder();
                for (ConfigSource assuemed : assumedDecoders) {
                    added.add(Exec.newConfigSource());
                }
                added.addAll(guessedDecoders);
                return guessed.set("decoders", added.build());
            }
        }

        @SuppressWarnings("deprecation") // https://github.com/embulk/embulk/issues/1301
        private static PluginTask loadPluginTask(final ConfigSource config) {
            return config.loadConfig(PluginTask.class);
        }

        @SuppressWarnings("deprecation") // https://github.com/embulk/embulk/issues/1301
        private static PluginTask loadPluginTaskFromTaskSource(final TaskSource taskSource) {
            return taskSource.loadTask(PluginTask.class);
        }
    }

    public static class GuessedNoticeError extends Error {
        private final ConfigDiff guessedConfig;

        public GuessedNoticeError(ConfigDiff guessedConfig) {
            this.guessedConfig = guessedConfig;
        }

        public ConfigDiff getGuessedConfig() {
            return guessedConfig;
        }
    }

    @SuppressWarnings("deprecation") // https://github.com/embulk/embulk/issues/1301
    private static GuessExecutorTask loadGuessExecutorTask(final ConfigSource config) {
        return config.loadConfig(GuessExecutorTask.class);
    }
}
