package org.quickload.standards;

import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.exec.BufferManager;
import org.quickload.record.*;
import org.quickload.spi.*;

public class CsvFormatterPlugin<T extends FormatterTask>
        implements FormatterPlugin<T>
{
    private final BufferManager bufferManager;

    @Inject
    public CsvFormatterPlugin(BufferManager bufferManager)
    {
        this.bufferManager = bufferManager;
    }

    @Override
    public OutputOperator openOperator(T task, int processorIndex, BufferOperator op)
    {
        return new CSVFormatterOutputOperator(task.getSchema(), processorIndex,
                op, bufferManager);
    }

    public void shutdown()
    {
        // TODO
    }

    static class CSVFormatterOutputOperator extends AbstractOutputOperator
    {
        private final Schema schema;
        private final int processorIndex;
        private final BufferOperator op;
        private final BufferManager bufferManager;

        private CSVFormatterOutputOperator(Schema schema, int processorIndex,
                                           BufferOperator op, BufferManager bufferManager)
        {
            this.schema = schema;
            this.processorIndex = processorIndex;
            this.op = op;
            this.bufferManager = bufferManager;
        }

        @Override
        public void addPage(Page page)
        {
            // TODO simple implementation
            final StringBuilder sbuf = new StringBuilder(); // TODO

            PageReader pageReader = new PageReader(bufferManager, schema);
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
