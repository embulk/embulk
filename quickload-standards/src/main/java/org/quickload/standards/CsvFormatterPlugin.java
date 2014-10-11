package org.quickload.standards;

import org.quickload.config.ConfigSource;
import org.quickload.exec.BufferManager;
import org.quickload.record.*;
import org.quickload.spi.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class CsvFormatterPlugin<T extends FormatterTask>
        implements FormatterPlugin<T>
{
    @Override
    public OutputOperator openOperator(T task, int processorIndex, BufferOperator op)
    {
        return new CSVFormatterOutputOperator(task.getSchema(), processorIndex, op);
    }

    public void shutdown()
    {
        // TODO
    }

    static class CSVFormatterOutputOperator extends AbstractOutputOperator
    {
        private Schema schema;
        private int processorIndex;
        private BufferOperator op;
        private PageAllocator pageAllocator;

        private CSVFormatterOutputOperator(Schema schema, int processorIndex, BufferOperator op)
        {
            this.schema = schema;
            this.processorIndex = processorIndex;
            this.op = op;
            this.pageAllocator = new BufferManager(); // TODO
        }

        @Override
        public void addPage(Page page)
        {
            // TODO ad-hoc
            //String path = task.getPaths().get(processorIndex);
            String path = "/tmp/foo.csv";

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
        public Report completed() {
            return null; // TODO
        }

        @Override
        public void close() {}
    }
}
