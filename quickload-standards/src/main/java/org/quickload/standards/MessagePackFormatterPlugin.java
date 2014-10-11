package org.quickload.standards;

import org.quickload.record.Page;
import org.quickload.record.Schema;
import org.quickload.spi.*;

public class MessagePackFormatterPlugin<T extends FormatterTask>
        implements FormatterPlugin<T> {

    @Override
    public OutputOperator openOperator(T task, int processorIndex, BufferOperator op)
    {
        return new MessagePackFormatterOutputOperator(task.getSchema(), processorIndex, op);
    }

    @Override
    public void shutdown()
    {
        // TODO
    }

    static class MessagePackFormatterOutputOperator extends AbstractOutputOperator
    {
        private final Schema schema;
        private final int processorIndex;
        private final BufferOperator op;

        private MessagePackFormatterOutputOperator(Schema schema, int processorIndex, BufferOperator op)
        {
            this.schema = schema;
            this.processorIndex = processorIndex;
            this.op = op;
        }

        @Override
        public void addPage(Page page)
        {
            // TODO
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
}
