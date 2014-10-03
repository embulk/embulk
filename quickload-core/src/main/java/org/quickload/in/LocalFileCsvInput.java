package org.quickload.in;

import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.DynamicModel;
import org.quickload.exec.BufferManager;
import org.quickload.record.Column;
import org.quickload.record.DoubleType;
import org.quickload.record.LongType;
import org.quickload.record.PageAllocator;
import org.quickload.record.PageBuilder;
import org.quickload.record.RecordBuilder;
import org.quickload.record.RecordProducer;
import org.quickload.record.Schema;
import org.quickload.record.StringType;
import org.quickload.spi.BasicInputPlugin;
import org.quickload.spi.DynamicReport;
import org.quickload.spi.InputProgress;
import org.quickload.spi.InputTask;
import org.quickload.spi.OutputOperator;
import org.quickload.spi.ReportBuilder;
import org.quickload.spi.ThreadInputProcessor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class LocalFileCsvInput
        extends BasicInputPlugin<LocalFileCsvInput.Task>
{ // TODO change superclass to FileInputPlugin

    public interface Task
            extends InputTask, DynamicModel<Task>
    { // TODO change superclass to FileInputTask
        @Config("in:paths") // TODO temporarily added 'in:'
        public List<String> getPaths();

        @Config("in:schema") // TODO temporarily added 'in:'
        public Schema getSchema();
    }

    public static class Processor
            extends ThreadInputProcessor
    {
        private final Task task; // TODO change FileInputTask
        private final int processorIndex;
        private final PageAllocator pageAllocator;

        public Processor(Task task,
                int processorIndex, OutputOperator op)
        {
            super(op);
            this.task = task;
            this.processorIndex = processorIndex;
             // TODO Sada push the refactoring related to ThreadInProcessor
            this.thread.start();
            // TODO initialize page allocator object
            // TODO consider when the page allocator object is released?
            this.pageAllocator = new BufferManager(); // TODO
        }

        @Override
        public ReportBuilder runThread() throws Exception
        {
            // TODO ad-hoc
            String path = task.getPaths().get(processorIndex);
            //Schema schema = task.getSchema();
            // TODO manually create schema object now
            Schema schema = new Schema(Arrays.asList(
                    new Column(0, "date_code", StringType.STRING),
                    new Column(1, "customer_code", StringType.STRING),
                    new Column(2, "product_code", StringType.STRING),
                    new Column(3, "employee_code", StringType.STRING)));

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
            }

            recordBuilder.flush();

            return DynamicReport.builder();
        }

        @Override
        public InputProgress getProgress()
        {
            return null;
        }
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
    public Processor startProcessor(Task task,
            int processorIndex, OutputOperator op)
    {
        return new Processor(task, processorIndex, op);
    }
}
