package org.embulk.exec;

import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.fasterxml.jackson.databind.JsonNode;
import org.embulk.buffer.Buffer;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.NextConfig;
import org.embulk.config.DataSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.Report;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.FileBufferOutput;
import org.embulk.channel.PageOutput;
import org.embulk.record.Schema;
import org.embulk.record.Column;
import org.embulk.record.Page;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.BasicParserPlugin;
import org.embulk.spi.ExecTask;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.ExecControl;

public class GuessExecutor
{
    private final Injector injector;
    private final ConfigSource systemConfig;
    private final List<JsonNode> defaultGuessPlugins;

    @Inject
    public GuessExecutor(Injector injector,
            @ForSystemConfig ConfigSource systemConfig)
    {
        this.injector = injector;
        this.systemConfig = systemConfig;

        // TODO get default guess plugins from injector using Multibinder
        this.defaultGuessPlugins = ImmutableList.copyOf(DataSource.arrayNode()
                .add("gzip")
                .add("charset")
                .add("newline")
                .add("csv"));
    }

    public NextConfig run(ConfigSource config)
    {
        ExecTask exec = PluginExecutors.newExecTask(injector, config);
        return guess(exec, config);
    }

    public NextConfig guess(ExecTask exec, ConfigSource config)
    {
        try {
            return doGuess(exec, config);
        } catch (Throwable ex) {
            throw PluginExecutors.propagePluginExceptions(ex);
        }
    }

    private interface GuessExecutorTask
            extends Task
    {
        @Config("guess_plugins")
        @ConfigDefault("[]")
        public List<JsonNode> getGuessPlugins();

        @Config("exclude_guess_plugins")
        @ConfigDefault("[]")
        public List<JsonNode> getExcludeGuessPlugins();
    }

    private NextConfig doGuess(final ExecTask exec, ConfigSource config)
    {
        Buffer sample = SamplingParserPlugin.runFileInputSampling(exec, config);
        if (sample.limit() == 0) {
            throw new NoSampleException("Can't get sample data because the first input file is empty");
        }

        List<JsonNode> guessPlugins = new ArrayList<JsonNode>(defaultGuessPlugins);
        if (config.get("exec", null) != null) {
            GuessExecutorTask task = exec.loadConfig(config.getObject("exec"), GuessExecutorTask.class);
            guessPlugins.addAll(task.getGuessPlugins());
            guessPlugins.removeAll(task.getExcludeGuessPlugins());
        }

        return runGuessInput(exec, sample, config, guessPlugins);
    }

    private NextConfig runGuessInput(final ExecTask exec, Buffer sample,
            ConfigSource config, List<JsonNode> guessPlugins)
    {
        // repeat guessing upto 10 times
        NextConfig lastGuessed = new NextConfig();
        for (int i=0; i < 10; i++) {
            // include last-guessed config to run guess
            ConfigSource guessConfig = config.deepCopy().mergeRecursively(lastGuessed.deepCopy());

            // override in.parser.type so that FileInputPlugin creates GuessParserPlugin
            ConfigSource guessInputConfig = guessConfig.getObject("in");
            guessInputConfig.getObjectOrSetEmpty("parser")
                .setString("type", "system_guess")
                .set("guess_plugins", DataSource.arrayNode().addAll(guessPlugins));

            // run FileInputPlugin
            final InputPlugin input = new SampledBufferFileInputPlugin(sample);
            NextConfig guessed;
            try {
                input.runInputTransaction(exec, guessInputConfig, new ExecControl() {
                    public List<Report> run(TaskSource inputTaskSource)
                    {
                        input.runInput(exec, inputTaskSource, 0, new PageOutput(null) {  // TODO repeat runInput with processorIndex++ if NoSampleException happens
                            @Override
                            public void add(Page page)
                            {
                                throw new RuntimeException("Input plugin must be a FileInputPlugin to guess parser configuration");  // TODO exception class
                            }
                        });
                        throw new AssertionError("Guess executor must throw GuessedNoticeError");
                    }
                });
                throw new AssertionError("Guess executor must throw GuessedNoticeError");

            } catch (GuessedNoticeError error) {
                guessed = new NextConfig();
                guessed.getObjectOrSetEmpty("in").set("parser", error.getGuessedParserConfig());
            }

            // merge to the last-guessed config
            NextConfig nextGuessed = lastGuessed.deepCopy();
            lastGuessed.mergeRecursively(guessed);
            if (lastGuessed.equals(nextGuessed)) {
                // not changed
                return lastGuessed;
            }
        }
        return lastGuessed;
    }

    private static class SampledBufferFileInputPlugin
            extends FileInputPlugin
    {
        private final Buffer buffer;

        public SampledBufferFileInputPlugin(Buffer buffer)
        {
            this.buffer = buffer;
        }

        public NextConfig runFileInputTransaction(ExecTask exec, ConfigSource config,
                ExecControl control)
        {
            exec.setProcessorCount(1);
            control.run(new TaskSource());
            return new NextConfig();
        }

        public Report runFileInput(ExecTask exec, TaskSource taskSource,
                int processorIndex, FileBufferOutput fileBufferOutput)
        {
            fileBufferOutput.add(buffer);
            fileBufferOutput.addFile();
            return new Report();
        }
    }

    public static class GuessParserPlugin
            extends BasicParserPlugin
    {
        private interface PluginTask
                extends Task
        {
            @Config("guess_plugins")
            public List<JsonNode> getGuessPluginTypes();

            public ConfigSource getConfigSource();
            public void setConfigSource(ConfigSource config);
        }

        @Override
        public TaskSource getBasicParserTask(ExecTask exec, ConfigSource config)
        {
            PluginTask task = exec.loadConfig(config, PluginTask.class);
            task.setConfigSource(config);

            // set dummy schema to bypass ExecTask validation
            exec.setSchema(new Schema(ImmutableList.<Column>of()));

            return exec.dumpTask(task);
        }

        @Override
        public void runBasicParser(ExecTask exec,
                TaskSource taskSource, int processorIndex,
                FileBufferInput fileBufferInput, PageOutput pageOutput)
        {
            PluginTask task = exec.loadTask(taskSource, PluginTask.class);
            final ConfigSource config = task.getConfigSource();

            // get sample buffer
            Buffer sample = null;
            RuntimeException decodeException = null;
            try {
                while (fileBufferInput.nextFile()) {
                    for (Buffer buffer : fileBufferInput) {
                        if (sample == null) {
                            // get header data of the file
                            sample = buffer;
                        }
                    }
                }
            } catch (RuntimeException ex) {
                // ignores exceptions because FileDecoderPlugin can throw exceptions
                // such as "Unexpected end of ZLIB input stream"
                decodeException = ex;
            }
            if (sample == null) {
                if (decodeException != null) {
                    throw decodeException;
                }
                throw new NoSampleException("No input buffer to guess");
            }

            // load guess plugins
            ImmutableList.Builder<GuessPlugin> builder = ImmutableList.builder();
            for (JsonNode guessType : task.getGuessPluginTypes()) {
                GuessPlugin guess = exec.newPlugin(GuessPlugin.class, guessType);
                builder.add(guess);
            }
            List<GuessPlugin> guesses = builder.build();

            // run guess plugins
            NextConfig guessedParserConfig = new NextConfig();
            for (int i=0; i < guesses.size(); i++) {
                GuessPlugin guess = guesses.get(i);
                NextConfig guessed = guess.guess(exec, config, sample);
                guessedParserConfig.mergeRecursively(guessed);
            }

            throw new GuessedNoticeError(guessedParserConfig);
        }
    }

    public static class GuessedNoticeError
            extends Error
    {
        private final NextConfig guessedParserConfig;

        public GuessedNoticeError(NextConfig guessedParserConfig)
        {
            this.guessedParserConfig = guessedParserConfig;
        }

        public NextConfig getGuessedParserConfig()
        {
            return guessedParserConfig;
        }
    }
}
