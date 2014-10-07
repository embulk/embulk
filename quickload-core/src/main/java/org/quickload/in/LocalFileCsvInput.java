package org.quickload.in;

import com.google.common.base.Function;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.DynamicModel;
import org.quickload.exec.BufferManager;
import org.quickload.record.*;
import org.quickload.spi.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class LocalFileCsvInput
        extends BasicInputPlugin<LocalFileCsvInput.Task>
{ // TODO change superclass to FileInputPlugin

    // TODO initialize page allocator object
    // TODO consider when the page allocator object is released?
    private final PageAllocator pageAllocator = new BufferManager(); // TODO

    public interface Task
            extends InputTask, DynamicModel<Task>
    { // TODO change superclass to FileInputTask
        @Config("in:paths") // TODO temporarily added 'in:'
        public List<String> getPaths();

        @Config("schema") // TODO temporarily added 'in:'
        public Schema getSchema();
    }

    @Override
    public Task getTask(ConfigSource config)
    {
        Task task = config.load(Task.class);
        //task.getBasePath()
        //task.set("paths", ...);
        //return task.validate();
        task.set("ProcessorCount", task.getPaths().size());
        return task.validate();
    }

    @Override
    public ThreadInputProcessor startProcessor(final Task task,
            final int processorIndex, final OutputOperator op)
    {
        return ThreadInputProcessor.start(op, new Function<OutputOperator, ReportBuilder>() {
            public ReportBuilder apply(OutputOperator op) {
                // TODO body ported from Processor.runThread
                // TODO ad-hoc
                String path = task.getPaths().get(processorIndex);
                Schema schema = task.getSchema();

                // TODO simple implementation

                PageBuilder pageBuilder = new PageBuilder(pageAllocator, schema, op);
                RecordBuilder recordBuilder = pageBuilder.builder();

                recordBuilder.startRecord();

                // TODO execute it here?
                try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        final String[] lineValues = line.split(","); // TODO ad-hoc parsing
                        RecordProducer recordProducer = new RecordProducer() {
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
                        };
                        schema.produce(recordBuilder, recordProducer);
                        recordBuilder.addRecord();
                    }
                } catch (IOException e) {
                    e.printStackTrace(); // TODO
                }

                recordBuilder.flush();

                return DynamicReport.builder();
            }
        });
    }
}
