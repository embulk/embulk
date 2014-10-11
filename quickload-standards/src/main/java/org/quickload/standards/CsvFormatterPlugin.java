package org.quickload.standards;

import org.quickload.buffer.Buffer;
import org.quickload.exec.BufferManager;
import org.quickload.record.*;
import org.quickload.spi.*;

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
            // TODO simple implementation
            final StringBuilder sbuf = new StringBuilder(); // TODO

            PageReader pageReader = new PageReader(pageAllocator, schema);
            RecordCursor recordCursor = pageReader.cursor(page);

            while (recordCursor.next()) {
                RecordConsumer recordConsumer = new RecordConsumer()
                {
                    @Override
                    public void setNull(Column column) {
                        sbuf.append(',');
                    }

                    @Override
                    public void setLong(Column column, long value) {
                        sbuf.append(Long.toString(value)).append(',');
                    }

                    @Override
                    public void setDouble(Column column, double value) {
                        sbuf.append(Double.toString(value)).append(',');
                    }

                    @Override
                    public void setString(Column column, String value) {
                        sbuf.append(value).append(',');
                    }
                };
                schema.consume(recordCursor, recordConsumer);
                sbuf.append('\n');
            }
            op.addBuffer(new Buffer(sbuf.toString().getBytes()));
        }

        @Override
        public Report completed()
        {
            return null; // TODO
        }

        @Override
        public void close()
        {
            // TODO
        }
    }
}
