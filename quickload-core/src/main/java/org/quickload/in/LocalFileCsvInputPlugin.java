package org.quickload.in;

import com.google.common.base.Function;
import org.quickload.buffer.Buffer;
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
import org.quickload.spi.AbstractBufferOperator;
import org.quickload.spi.BufferOperator;
import org.quickload.spi.DynamicReport;
import org.quickload.spi.FileInputPlugin;
import org.quickload.spi.FileInputTask;
import org.quickload.spi.OutputOperator;
import org.quickload.spi.ParserPlugin;
import org.quickload.spi.ParserTask;
import org.quickload.spi.Report;
import org.quickload.spi.ReportBuilder;
import org.quickload.spi.ThreadInputProcessor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class LocalFileCsvInputPlugin
        extends FileInputPlugin<LocalFileCsvInputPlugin.Task>
{
    // TODO initialize page allocator object
    // TODO consider when the page allocator object is released?
    private final PageAllocator pageAllocator = new BufferManager(); // TODO

    public interface Task
            extends FileInputTask, DynamicModel<Task>
    { // TODO change superclass to FileInputTask
        @Config("in:paths") // TODO temporarily added 'in:'
        public List<String> getPaths();

        @Config("in:schema") // TODO temporarily added 'in:'
        public Schema getSchema();

        public String getParserType();

        public ParserTask getParserTask();
    }

    @Override
    public Task getTask(ConfigSource config)
    {
        Task task = config.load(Task.class);
        task.set("ProcessorCount", task.getPaths().size());

        task.set("ParserType", "csv");
        MyParserTask parserTask = config.load(MyParserTask.class);
        parserTask.set("ParserSchema", task.getSchema());

        task.set("ParserTask", parserTask);

        return task.validate();
    }

    public interface MyParserTask extends ParserTask, DynamicModel<MyParserTask>
    {
        public Schema getParserSchema();
    }

    public static class MyParserPlugin implements ParserPlugin<MyParserTask>
    { // TODO should move this to other place...

        public BufferOperator openOperator(MyParserTask task, int processorIndex, OutputOperator op)
        {
            return new MyBufferOperator(task, processorIndex, op);
        }

        public void shutdown() {} // ignore
    }

    @Override
    public ParserPlugin getParserPlugin(String type)
    {
        return new MyParserPlugin(); // TODO should improve
    }

    public static class MyBufferOperator extends AbstractBufferOperator
    {
        private final MyParserTask task;
        private final int processorIndex;
        private final OutputOperator op;
        private final PageAllocator pageAllocator;

        public MyBufferOperator(MyParserTask task, int processorIndex, OutputOperator op)
        {
            this.task = task;
            this.processorIndex = processorIndex;
            this.op = op;
            pageAllocator = new BufferManager();
        }

        @Override
        public void addBuffer(Buffer buffer)
        {
            // TODO ad-hoc
            Schema schema = task.getParserSchema();

            PageBuilder pageBuilder = new PageBuilder(pageAllocator, schema, op);
            RecordBuilder recordBuilder = pageBuilder.builder();

            recordBuilder.startRecord();

            // TODO simple parser and line operator
            byte[] bytes = buffer.get();
            StringBuilder sbuf = new StringBuilder(); // TODO
            for (int i = 0; i < bytes.length; i++) {
                char c = (char)bytes[i];
                if (c == '\n' || c == '\r') {
                    addLine(sbuf.toString(), schema, recordBuilder);
                    sbuf = new StringBuilder(); // TODO
                } else {
                    sbuf.append(c);
                }
            }

            if (sbuf.length() != 0) {
                addLine(sbuf.toString(), schema, recordBuilder);
            }

            recordBuilder.flush();
        }

        private void addLine(String line, Schema schema, RecordBuilder recordBuilder)
        {
            System.out.println("# line: " + line);
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

        @Override
        public Report completed()
        {
            return null; // TODO
        }

        @Override
        public void close() throws Exception
        {
            // TODO
        }
    }

    // TODO port startFileInputProcessor
    @Override
    public ThreadInputProcessor startFileInputProcessor(final Task task,
            final int processorIndex, final BufferOperator op)
    {
        return ThreadInputProcessor.start(op, new Function<BufferOperator, ReportBuilder>() {
            public ReportBuilder apply(BufferOperator op) {

                // TODO ad-hoc
                String path = task.getPaths().get(processorIndex);

                try {
                    File file = new File(path);
                    byte[] bytes = new byte[(int) file.length()]; // ad-hoc

                    int len, offset = 0;
                    try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                        while ((len = in.read(bytes, offset, bytes.length - offset)) > 0) {
                            offset += len;
                        }
                        Buffer buffer = new Buffer(bytes);
                        op.addBuffer(buffer);
                    }
                } catch (Exception e) {
                    e.printStackTrace(); // TODO
                }

                return DynamicReport.builder();
            }
        });
    }
}
