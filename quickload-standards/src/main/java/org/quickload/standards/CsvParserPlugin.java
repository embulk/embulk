package org.quickload.standards;

import javax.validation.constraints.NotNull;
import com.google.inject.Inject;
import org.quickload.exec.BufferManager;
import org.quickload.record.Column;
import org.quickload.record.DoubleType;
import org.quickload.record.LongType;
import org.quickload.record.RecordProducer;
import org.quickload.record.Schema;
import org.quickload.record.StringType;
import org.quickload.record.PageBuilder;
import org.quickload.config.*;
import org.quickload.spi.*;

public class CsvParserPlugin
        extends LineParserPlugin<CsvParserPlugin.Task>
{
    private final BufferManager bufferManager;

    @Inject
    public CsvParserPlugin(BufferManager bufferManager)
    {
        this.bufferManager = bufferManager;
    }

    public interface Task
            extends LineParserTask
    {
        @Config("in:schema")
        @NotNull
        public Schema getSchema();
    }

    @Override
    public Task getTask(ConfigSource config)
    {
        Task task = config.load(Task.class);
        task.validate();
        return task;
    }

    @Override
    public LineOperator openLineOperator(Task task, int processorIndex,
            OutputOperator op)
    {
        return new Operator(task.getSchema(), processorIndex, op);
    }

    public void shutdown()
    {
        // TODO
    }

    class Operator
            extends AbstractOperator<OutputOperator>
            implements LineOperator
    {
        private final Schema schema;
        private final PageBuilder pageBuilder;

        public Operator(Schema schema, int processorIndex,
                OutputOperator op)
        {
            super(op);
            this.schema = schema;
            this.pageBuilder = new PageBuilder(bufferManager, schema, op);
        }

        @Override
        public void addLine(String line)
        {
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

        @Override
        public Report completed()
        {
            pageBuilder.flush();
            return super.completed();
        }
    }
}
