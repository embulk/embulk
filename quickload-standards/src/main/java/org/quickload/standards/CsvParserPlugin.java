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
        extends LineParserPlugin
{
    private final BufferManager bufferManager;

    @Inject
    public CsvParserPlugin(BufferManager bufferManager)
    {
        this.bufferManager = bufferManager;
    }

    public interface PluginTask
            extends Task
    {
        @Config("in:schema")@NotNull
        public Schema getSchema();

        @Config("in:column_header") // how to set default value?? TODO @Default("true")
        public boolean getColumnHeader();
    }

    @Override
    public TaskSource getLineParserTask(ProcConfig procConfig, ConfigSource config)
    {
        PluginTask task = config.loadTask(PluginTask.class);
        procConfig.setSchema(task.getSchema());
        return config.dumpTask(task);
    }

    @Override
    public LineOperator openLineOperator(ProcTask procTask,
            TaskSource taskSource, int processorIndex, PageOperator next)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        return new Operator(procTask.getSchema(), task.getColumnHeader(), processorIndex, next);
    }

    public void shutdown()
    {
        // TODO
    }

    class Operator extends AbstractOperator<PageOperator>
            implements LineOperator {
        private final Schema schema;
        private final boolean hasColumnHeader;
        private final PageBuilder pageBuilder;
        private final CSVRecordProducer recordProducer;

        private boolean alreadyReadColumnHeaderLine;

        public Operator(Schema schema, boolean hasColumnHeader, int processorIndex,
                        PageOperator next) {
            super(next);
            this.schema = schema;
            this.hasColumnHeader = hasColumnHeader;
            this.pageBuilder = new PageBuilder(bufferManager, schema, next);
            this.recordProducer = new CSVRecordProducer();

            alreadyReadColumnHeaderLine = !hasColumnHeader;
        }

        @Override
        public void addLine(String line) {
            if (!alreadyReadColumnHeaderLine) {
                alreadyReadColumnHeaderLine = true;
                return;
            }

            recordProducer.setColumnStrings(line.split(",")); // TODO ad-hoc splitting
            schema.produce(pageBuilder, recordProducer);
            pageBuilder.addRecord();
        }

        @Override
        public Report completed() {
            pageBuilder.flush();
            return super.completed();
        }
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
