package org.quickload.exec;

import java.util.List;
import javax.validation.constraints.NotNull;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.quickload.spi.InputPlugin;
import org.quickload.config.Config;
import org.quickload.config.NextConfig;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.Report;
import org.quickload.spi.ProcTask;
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
        parserType.put("injected", "guess");
        config.put("in:parser_type", parserType);
        config.putBoolean("in:guess", true);

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
}
