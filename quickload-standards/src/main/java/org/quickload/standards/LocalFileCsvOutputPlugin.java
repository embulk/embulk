package org.quickload.standards;

import com.google.inject.Inject;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.DynamicModel;
import org.quickload.exec.BufferManager;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Column;
import org.quickload.record.Page;
import org.quickload.record.PageAllocator;
import org.quickload.record.PageReader;
import org.quickload.record.RecordConsumer;
import org.quickload.record.RecordCursor;
import org.quickload.record.Schema;
import org.quickload.spi.AbstractOutputOperator;
import org.quickload.spi.BasicOutputPlugin;
import org.quickload.spi.DynamicReport;
import org.quickload.spi.InputTask;
import org.quickload.spi.OutputTask;
import org.quickload.spi.Report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

public class LocalFileCsvOutputPlugin
        extends BasicOutputPlugin<LocalFileCsvOutputPlugin.Task>
{ // TODO change superclass to FileOutputPlugin

    @Inject
    public LocalFileCsvOutputPlugin(PluginManager pluginManager)
    {
    }

    public interface Task
            extends OutputTask, DynamicModel<Task>
    { // TODO change superclass to FileOutputTask
        @Config("out:paths")
        public List<String> getPaths();

        public Schema getOutputSchema();
    }

    public static class Operator
            extends AbstractOutputOperator
    {
        private final Task task;
        private final int processorIndex;
        private final PageAllocator pageAllocator;

        Operator(Task task, int processorIndex) {
            this.task = task;
            this.processorIndex = processorIndex;
            this.pageAllocator = new BufferManager(); // TODO
        }

        @Override
        public void addPage(Page page)
        {
            // TODO ad-hoc
            String path = task.getPaths().get(processorIndex);
            // TODO manually create schema object now
            //Schema schema = (Schema) task.get("out:schema");
            Schema schema = task.getOutputSchema();

            // TODO simple implementation

            PageReader pageReader = new PageReader(pageAllocator, schema);
            RecordCursor recordCursor = pageReader.cursor(page);
            File file = new File(path);

            try (PrintWriter w = new PrintWriter(file)) {
                // TODO writing data to the file

                while (recordCursor.next()) {
                    RecordConsumer recordConsumer = new RecordConsumer() {
                        @Override
                        public void setNull(Column column) {
                            // TODO
                        }

                        @Override
                        public void setLong(Column column, long value) {
                            // TODO
                        }

                        @Override
                        public void setDouble(Column column, double value) {
                            // TODO
                        }

                        @Override
                        public void setString(Column column, String value) {
                            System.out.print(value);
                            System.out.print(',');
                            //w.append(value).append(',');
                        }
                    };
                    schema.consume(recordCursor, recordConsumer);
                    System.out.println();
                    //w.append('\n');
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void close() throws Exception
        {
        }

        @Override
        public Report completed()
        {
            return DynamicReport.builder().build(null);
        }
    }

    @Override
    public Task getTask(ConfigSource config, InputTask input)
    {
        Task task = config.load(Task.class);
        task.set("OutputSchema", input.getSchema());
        return task.validate();
    }

    @Override
    public void begin(Task task)
    {
    }

    @Override
    public Operator openOperator(Task task, int processorIndex)
    {
        return new Operator(task, processorIndex);
    }

    @Override
    public void commit(Task task, List<Report> reports)
    {
    }

    @Override
    public void abort(Task task)
    {
    }
}
