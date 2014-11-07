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
        @Config("in:type")
        @NotNull
        public JsonNode getInputType();
    }

    public NextConfig run(ConfigSource config)
    {
        ProcTask proc = new ProcTask(injector);
        return guess(proc, config);
    }

    public NextConfig guess(final ProcTask proc, ConfigSource config)
    {
        JsonNodeFactory js = JsonNodeFactory.instance;
        ObjectNode parserType = js.objectNode();
        parserType.put("injected", "guess");  // GuessExecutor.GuessParserPlugin injected by ExecModule.configure
        config.set("in:parser_type", parserType);
        config.setBoolean("in:guess", true);

        GuessTask task = proc.loadConfig(config, GuessTask.class);
        final InputPlugin input = proc.newPlugin(InputPlugin.class, task.getInputType());
        try {
            input.runInputTransaction(proc, config, new ProcControl() {
                public List<Report> run(TaskSource inputTaskSource)
                {
                    // TODO validate proc.getProcessorCount > 0
                    input.runInput(proc, inputTaskSource, 0, null);
                    return null;
                }
            });
            return new NextConfig();
        } catch (GuessedNoticeError guessed) {
            return guessed.getGuessed();
        }
    }

    public static class GuessParserPlugin
            implements ParserPlugin
    {
        private interface PluginTask
                extends Task
        {
            @Config(value="guess:sample_size", defaultValue="32768")
            public int getSampleSize();

            @Config(value="guess:types")
            public List<JsonNode> getGuessTypes();

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

            List<Buffer> samples = getSamples(fileBufferInput, maxSampleSize);

            // load guess plugins
            ImmutableList.Builder<ParserGuessPlugin> builder = ImmutableList.builder();
            for (JsonNode guessType : task.getGuessTypes()) {
                ParserGuessPlugin guess = proc.newPlugin(ParserGuessPlugin.class, guessType);
                builder.add(guess);
            }
            List<ParserGuessPlugin> guesses = builder.build();

            // repeat guessing
            NextConfig guessed = new NextConfig();
            int startSize = guessed.getFieldNames().size();
            while (true) {
                for (int i=0; i < guesses.size(); i++) {
                    ParserGuessPlugin guess = guesses.get(i);
                    guessed.setAll(guess.guess(proc, config, samples));
                }
                int guessedSize = guessed.getFieldNames().size();
                if (guessedSize <= startSize) {
                    break;
                }
                startSize = guessedSize;
            }

            throw new GuessedNoticeError(guessed);
        }

        public static List<Buffer> getSamples(FileBufferInput fileBufferInput, int maxSampleSize)
        {
            long sampleSize = 0;
            ImmutableList.Builder<Buffer> builder = ImmutableList.builder();
            Iterator<Buffer> ite = fileBufferInput.iterator();
            while (sampleSize < maxSampleSize) {
                if (!ite.hasNext()) {
                    break;
                }
                Buffer buffer = ite.next();
                sampleSize += buffer.limit();
                builder.add(buffer);
            }
            if (sampleSize == 0) {
                throw new RuntimeException("empty");
            }
            return builder.build();
        }
    }
}
