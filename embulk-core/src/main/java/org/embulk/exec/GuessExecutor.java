package org.embulk.exec;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import com.google.common.collect.ImmutableList;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import org.embulk.plugin.PluginType;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.spi.Schema;
import org.embulk.spi.Page;
import org.embulk.spi.Buffer;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecAction;
import org.embulk.spi.ExecSession;
import org.embulk.spi.FileInput;
import org.embulk.spi.PageOutput;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.spi.FileInputRunner;
import static org.embulk.spi.util.Inputs.each;

public class GuessExecutor
{
    private final List<PluginType> defaultGuessPlugins;

    private interface GuessExecutorSystemTask
            extends Task
    {
        @Config("guess_plugins")
        @ConfigDefault("[]")
        public List<PluginType> getGuessPlugins();
    }

    private interface GuessExecutorTask
            extends Task
    {
        @Config("guess_plugins")
        @ConfigDefault("[]")
        public List<PluginType> getGuessPlugins();

        @Config("exclude_guess_plugins")
        @ConfigDefault("[]")
        public List<PluginType> getExcludeGuessPlugins();
    }

    public static void registerDefaultGuessPluginTo(Binder binder, PluginType type)
    {
        Multibinder<PluginType> multibinder = Multibinder.newSetBinder(binder, PluginType.class, ForGuess.class);
        multibinder.addBinding().toInstance(type);
    }

    @Inject
    public GuessExecutor(@ForSystemConfig ConfigSource systemConfig,
            @ForGuess Set<PluginType> defaultGuessPlugins)
    {
        GuessExecutorSystemTask systemTask = systemConfig.loadConfig(GuessExecutorSystemTask.class);

        ImmutableList.Builder<PluginType> list = ImmutableList.builder();
        list.addAll(defaultGuessPlugins);
        list.addAll(systemTask.getGuessPlugins());
        this.defaultGuessPlugins = list.build();
    }

    public ConfigDiff guess(ExecSession exec, final ConfigSource config)
    {
        try {
            return Exec.doWith(exec, new ExecAction<ConfigDiff>() {
                public ConfigDiff run()
                {
                    try (SetCurrentThreadName dontCare = new SetCurrentThreadName("guess")) {
                        return doGuess(config);
                    }
                }
            });
        } catch (ExecutionException ex) {
            throw Throwables.propagate(ex.getCause());
        }
    }

    protected InputPlugin newInputPlugin(ConfigSource inputConfig)
    {
        return Exec.newPlugin(InputPlugin.class, inputConfig.get(PluginType.class, "type"));
    }

    private ConfigDiff doGuess(ConfigSource config)
    {
        ConfigSource inputConfig = config.getNested("in");
        ConfigSource execConfig = config.getNestedOrGetEmpty("exec");

        InputPlugin input = newInputPlugin(inputConfig);

        ConfigDiff inputGuessed;
        if (input instanceof ConfigurableGuessInputPlugin) {
            inputGuessed = ((ConfigurableGuessInputPlugin) input).guess(execConfig, inputConfig);
        }
        else {
            try {
                inputGuessed = input.guess(inputConfig);
            } catch (AbstractMethodError ex) {
                // for backward compatibility with embulk v0.4 interface
                throw new UnsupportedOperationException(input.getClass().getSimpleName()+".guess(ConfigSource) is not implemented. This input plugin does not support guessing.");
            }
        }

        ConfigDiff wrapped = Exec.newConfigDiff();
        wrapped.getNestedOrSetEmpty("in").merge(inputGuessed);
        return wrapped;
    }

    // called by FileInputRunner
    public ConfigDiff guessParserConfig(Buffer sample, ConfigSource inputConfig, ConfigSource execConfig)
    {
        List<PluginType> guessPlugins = new ArrayList<PluginType>(defaultGuessPlugins);

        GuessExecutorTask task = execConfig.loadConfig(GuessExecutorTask.class);
        guessPlugins.addAll(task.getGuessPlugins());
        guessPlugins.removeAll(task.getExcludeGuessPlugins());

        return guessParserConfig(sample, inputConfig, guessPlugins);
    }

    private ConfigDiff guessParserConfig(Buffer sample,
            ConfigSource config, List<PluginType> guessPlugins)
    {
        // repeat guessing upto 10 times
        ConfigDiff lastGuessed = Exec.newConfigDiff();
        for (int i=0; i < 10; i++) {
            // include last-guessed config to run guess input
            ConfigSource originalConfig = config.deepCopy().merge(lastGuessed);
            ConfigSource guessInputConfig = originalConfig.deepCopy();
            guessInputConfig.getNestedOrSetEmpty("parser")
                .set("type", "system_guess")  // override in.parser.type so that FileInputRunner.run uses GuessParserPlugin
                .set("guess_plugins", guessPlugins)
                .set("orig_config", originalConfig);

            // run FileInputPlugin
            final FileInputRunner input = new FileInputRunner(new BufferFileInputPlugin(sample));
            ConfigDiff guessed;
            try {
                input.transaction(guessInputConfig, new InputPlugin.Control() {
                    public List<TaskReport> run(TaskSource inputTaskSource, Schema schema, int taskCount)
                    {
                        if (taskCount == 0) {
                            throw new NoSampleException("No input files to guess");
                        }
                        input.run(inputTaskSource, null, 0, new PageOutput() {
                            @Override
                            public void add(Page page)
                            {
                                throw new RuntimeException("Input plugin must be a FileInputPlugin to guess parser configuration");  // TODO exception class
                            }

                            @Override
                            public void finish() { }

                            @Override
                            public void close() { }
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

    public static class GuessParserPlugin
            implements ParserPlugin
    {
        private interface PluginTask
                extends Task
        {
            @Config("guess_plugins")
            public List<PluginType> getGuessPluginTypes();

            @Config("orig_config")
            public ConfigSource getOriginalConfig();
        }

        @Override
        public void transaction(ConfigSource config, ParserPlugin.Control control)
        {
            PluginTask task = config.loadConfig(PluginTask.class);
            control.run(task.dump(), null);
        }

        @Override
        public void run(TaskSource taskSource, Schema schema,
                FileInput input, PageOutput pageOutput)
        {
            PluginTask task = taskSource.loadTask(PluginTask.class);
            final ConfigSource originalConfig = task.getOriginalConfig();

            // get sample buffer
            Buffer sample = readSample(input, 32*1024);  // TODO get sample size from system config. See also SamplingParserPlugin().

            // load guess plugins
            ImmutableList.Builder<GuessPlugin> builder = ImmutableList.builder();
            for (PluginType guessType : task.getGuessPluginTypes()) {
                GuessPlugin guess = Exec.newPlugin(GuessPlugin.class, guessType);
                builder.add(guess);
            }
            List<GuessPlugin> guesses = builder.build();

            // run guess plugins
            ConfigSource mergedConfig = originalConfig.deepCopy();
            ConfigDiff mergedGuessed = Exec.newConfigDiff();
            for (int i=0; i < guesses.size(); i++) {
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

        private static Buffer readSample(FileInput fileInput, int sampleSize)
        {
            Buffer sample = Buffer.allocate(sampleSize);
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

        private static class ConfigSourceList extends ArrayList<ConfigSource> { };

        private static ConfigDiff addAssumedDecoderConfigs(ConfigSource originalConfig, ConfigDiff guessed)
        {
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
    }

    public static class GuessedNoticeError
            extends Error
    {
        private final ConfigDiff guessedConfig;

        public GuessedNoticeError(ConfigDiff guessedConfig)
        {
            this.guessedConfig = guessedConfig;
        }

        public ConfigDiff getGuessedConfig()
        {
            return guessedConfig;
        }
    }
}
