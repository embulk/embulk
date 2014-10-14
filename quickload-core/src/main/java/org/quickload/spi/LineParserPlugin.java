package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.buffer.Buffer;

public abstract class LineParserPlugin <T extends LineParserTask>
        extends BasicParserPlugin<T>
{
    public abstract T getTask(ConfigSource config);

    public abstract LineOperator openLineOperator(T task, int processorIndex, OutputOperator op);

    @Override
    public BufferOperator openOperator(T task, int processorIndex, OutputOperator op)
    {
        return new LineDecodeOperator(openLineOperator(task, processorIndex, op));
    }

    public class LineDecodeOperator
            extends AbstractOperator <LineOperator>
            implements BufferOperator
    {
        public LineDecodeOperator(LineOperator next)
        {
            super(next);
        }

        public void addBuffer(Buffer buffer)
        {
            // TODO needs internal buffer
            // TODO use streaming decoder
            for (String line : new String(buffer.get()).split("\n")) {
                next.addLine(line);
            }
        }
    }
}
