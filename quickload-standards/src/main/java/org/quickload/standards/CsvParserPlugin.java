package org.quickload.standards;

import org.quickload.channel.FileBufferInput;
import org.quickload.channel.PageOutput;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.record.Column;
import org.quickload.record.DoubleType;
import org.quickload.record.LongType;
import org.quickload.record.PageBuilder;
import org.quickload.record.RecordProducer;
import org.quickload.record.Schema;
import org.quickload.record.StringType;
import org.quickload.spi.BasicParserPlugin;
import org.quickload.spi.LineDecoder;
import org.quickload.spi.ProcTask;

import java.util.List;

public class CsvParserPlugin extends BasicParserPlugin {
    @Override
    public TaskSource getBasicParserTask(ProcTask proc, ConfigSource config) {
        CsvParserTask task = proc.loadConfig(config, CsvParserTask.class);
        proc.setSchema(task.getSchemaConfig().toSchema());
        return proc.dumpTask(task);
    }

    @Override
    public void runBasicParser(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput) {
        Schema schema = proc.getSchema();
        CsvParserTask task = proc.loadTask(taskSource, CsvParserTask.class);
        CsvTokenizer tokenizer = new CsvTokenizer(new LineDecoder(fileBufferInput, task), task);
        PageBuilder builder = new PageBuilder(proc.getPageAllocator(), proc.getSchema(), pageOutput);

        while (fileBufferInput.nextFile()) {
            for (final List<String> record : tokenizer) {
                if (record.size() != schema.getColumns().size()) {
                    throw new RuntimeException("not match"); // TODO fix the error handling
                }

                schema.produce(builder, new RecordProducer()
                {
                    @Override
                    public void setLong(Column column, LongType.Setter setter)
                    {
                        setter.setLong(Long.parseLong(record.get(column.getIndex())));
                    }

                    @Override
                    public void setDouble(Column column, DoubleType.Setter setter)
                    {
                        setter.setDouble(Double.parseDouble(record.get(column.getIndex())));
                    }

                    @Override
                    public void setString(Column column, StringType.Setter setter)
                    {
                        setter.setString(record.get(column.getIndex()));
                    }
                });
                builder.addRecord();
            }
        }
        builder.flush();
    }
}
