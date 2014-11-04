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
import org.quickload.channel.FileBufferInput;
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
        @Config("in:schema")@NotNull
        public Schema getSchema();

        @Config("in:column_header") // how to set default value?? TODO @Default("true")
        public boolean getColumnHeader();
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
            FileBufferInput fileBufferInput, PageOutput pageOutput)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        Schema schema = proc.getSchema();
        LineDecoder decoder = new LineDecoder(fileBufferInput, task);
        PageBuilder pageBuilder = new PageBuilder(proc.getPageAllocator(), proc.getSchema(), pageOutput);
        CSVRecordProducer recordProducer = new CSVRecordProducer(); // TODO where should be it initialized?

        while (fileBufferInput.nextFile()) {
            for (String line : decoder) {
                recordProducer.setColumnStrings(line.split(",")); // TODO ad-hoc parsing
                schema.produce(pageBuilder, recordProducer);
                pageBuilder.addRecord();
            }
        }
        pageBuilder.flush();
    }

    static class CSVRecordProducer implements RecordProducer
    {
        private String[] columnStrings;

        CSVRecordProducer()
        {
        }

        public void setColumnStrings(String[] columnsStrings)
        {
            this.columnStrings = columnsStrings;
        }

        @Override
        public void setLong(Column column, LongType.Setter setter) {
            setter.setLong(Long.parseLong(columnStrings[column.getIndex()]));
        }

        @Override
        public void setDouble(Column column, DoubleType.Setter setter) {
            setter.setDouble(Double.parseDouble(columnStrings[column.getIndex()]));
        }

        @Override
        public void setString(Column column, StringType.Setter setter) {
            setter.setString(columnStrings[column.getIndex()]);
        }
    }
}
