package org.quickload.exec;

import java.util.List;
import java.util.Iterator;
import com.google.common.collect.ImmutableList;
import javax.validation.constraints.NotNull;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.quickload.buffer.Buffer;
import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.NextConfig;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.Report;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.PageOutput;
import org.quickload.record.Schema;
import org.quickload.record.Column;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.ParserPlugin;
import org.quickload.spi.ProcTask;
import org.quickload.spi.ParserGuessPlugin;
import org.quickload.spi.ProcControl;

public class GuessExecutor
{
    private final Injector injector;

    @Inject
    public GuessExecutor(Injector injector)
    {
        this.injector = injector;
    }

    public interface GuessTask
            extends Task
    {
        @Config("in")
        @NotNull
        public ConfigSource getInputConfig();
    }

    public NextConfig run(ConfigSource config)
    {
        ProcTask proc = new ProcTask(injector);
        return guess(proc, config);
    }

    protected InputPlugin newInputPlugin(ProcTask proc, GuessTask task)
    {
        return proc.newPlugin(InputPlugin.class, task.getInputConfig().get("type"));
    }

    public NextConfig guess(final ProcTask proc, ConfigSource config)
    {
        GuessTask task = proc.loadConfig(config, GuessTask.class);

        // override in.parser.type so that FileInputPlugin creates GuessParserPlugin
        ((ObjectNode) task.getInputConfig().get("parser")).put("type", "system_guess");

        // create FileInputPlugin
        final InputPlugin input = newInputPlugin(proc, task);

        try {
            input.runInputTransaction(proc, task.getInputConfig(), new ProcControl() {
                public List<Report> run(TaskSource inputTaskSource)
                {
                    input.runInput(proc, inputTaskSource, 0, null);
                    return null;
                }
            });
            return new NextConfig();
        } catch (GuessedNoticeError guessed) {
            return guessed.getGuessedParserConfig().setAll(config);
        }
    }

    public static class GuessParserPlugin
            implements ParserPlugin
    {
        private interface PluginTask
                extends Task
        {
            @Config("guess_sample_size")
            @ConfigDefault("32768")
            public int getSampleSize();

            @Config("guess_plugins")
            @ConfigDefault("[]")  // TODO require some plugins
            public List<JsonNode> getGuessPluginTypes();

            public ConfigSource getConfigSource();
            public void setConfigSource(ConfigSource config);
        }

        public TaskSource getParserTask(ProcTask proc, ConfigSource config)
        {
            PluginTask task = proc.loadConfig(config, PluginTask.class);

            task.setConfigSource(config);

            // set dummy schema to bypass ProcTask validation
            proc.setSchema(new Schema(ImmutableList.<Column>of()));

            return proc.dumpTask(task);
        }

        public void runParser(ProcTask proc,
                TaskSource taskSource, int processorIndex,
                FileBufferInput fileBufferInput, PageOutput pageOutput)
        {
            PluginTask task = proc.loadTask(taskSource, PluginTask.class);
            final int maxSampleSize = task.getSampleSize();
            final ConfigSource config = task.getConfigSource();

            Buffer sample = getSample(fileBufferInput, maxSampleSize);

            // load guess plugins
            ImmutableList.Builder<ParserGuessPlugin> builder = ImmutableList.builder();
            for (JsonNode guessType : task.getGuessPluginTypes()) {
                ParserGuessPlugin guess = proc.newPlugin(ParserGuessPlugin.class, guessType);
                builder.add(guess);
            }
            List<ParserGuessPlugin> guesses = builder.build();

            // repeat guessing
            NextConfig guessedParserConfig = new NextConfig();
            int configKeys = guessedParserConfig.getFieldNames().size();
            while (true) {
                for (int i=0; i < guesses.size(); i++) {
                    ParserGuessPlugin guess = guesses.get(i);
                    guessedParserConfig.setAll(guess.guess(proc, config, sample));  // TODO needs recursive merging?
                }
                int guessedConfigKeys = guessedParserConfig.getFieldNames().size();
                if (guessedConfigKeys <= configKeys) {
                    break;
                }
                configKeys = guessedConfigKeys;
            }

            throw new GuessedNoticeError(guessedParserConfig);
        }

        public static Buffer getSample(FileBufferInput fileBufferInput, int maxSampleSize)
        {
            int sampleSize = 0;
            ImmutableList.Builder<Buffer> builder = ImmutableList.builder();
            for (Buffer buffer : fileBufferInput) {
                sampleSize += buffer.limit();
                builder.add(buffer);
                if (sampleSize >= maxSampleSize) {
                    break;
                }
            }
            if (sampleSize == 0) {
                throw new RuntimeException("empty");
            }

            Buffer sample = Buffer.allocate(sampleSize);
            int offset = 0;
            for (Buffer buffer : builder.build()) {
                sample.setBytes(offset, buffer, 0, buffer.limit());
                offset += buffer.limit();
            }
            return sample;
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
