package org.quickload.standards;

import javax.validation.constraints.NotNull;
import org.quickload.exec.BufferManager;
import org.quickload.record.Column;
import org.quickload.record.DoubleType;
import org.quickload.record.LongType;
import org.quickload.record.RecordProducer;
import org.quickload.record.Schema;
import org.quickload.record.StringType;
import org.quickload.record.PageBuilder;
import org.quickload.channel.BufferInput;
import org.quickload.channel.PageOutput;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.spi.ParserPlugin;
import org.quickload.spi.ProcTask;
import org.quickload.spi.LineDecoder;
import org.quickload.spi.LineDecoderTask;

public class CsvParserPlugin
        implements ParserPlugin
{
    public interface PluginTask
            extends Task, LineDecoderTask
    {
        @Config("in:schema")
        @NotNull
        public Schema getSchema();
    }

    @Override
    public TaskSource getParserTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = config.loadTask(PluginTask.class);
        proc.setSchema(task.getSchema());
        return config.dumpTask(task);
    }

    public void runParser(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            BufferInput bufferInput, PageOutput pageOutput)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        Schema schema = proc.getSchema();
        LineDecoder decoder = new LineDecoder(bufferInput, task);
        PageBuilder pageBuilder = new PageBuilder(proc.getPageAllocator(), proc.getSchema(), pageOutput);

        for (String line : decoder) {
            final String[] lineValues = line.split(","); // TODO ad-hoc parsing
            schema.produce(pageBuilder, new RecordProducer() {
                @Override
                public void setLong(Column column, LongType.Setter setter) {
                    // TODO setter.setLong(Long.parseLong(lineValues[column.getIndex()]));
                }

                @Override
                public void setDouble(Column column, DoubleType.Setter setter) {
                    // TODO setter.setDouble(Double.parseDouble(lineValues[column.getIndex()]));
                }

                @Override
                public void setString(Column column, StringType.Setter setter) {
                    setter.setString(lineValues[column.getIndex()]);
                }
            });
            pageBuilder.addRecord();
        }
        pageBuilder.flush();
    }
}
