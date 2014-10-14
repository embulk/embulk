package org.quickload.spi;

import org.quickload.config.Config;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.buffer.Buffer;

public abstract class LineParserPlugin
        implements ParserPlugin
{
    public abstract TaskSource getLineParserTask(ProcConfig proc, ConfigSource config);

    public abstract LineOperator openLineOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex, PageOperator next);

    public interface ParserTask
            extends Task
    {
        // TODO encoding, malformed input reporting behvior, etc.
        @Config("encoding")/*, default = "utf8")*/
        public String getEncoding();
    }

    @Override
    public TaskSource getParserTask(ProcConfig proc, ConfigSource config)
    {
        // TODO use ParserTask? is-a or has-a
        return getLineParserTask(proc, config);
    }

    @Override
    public BufferOperator openBufferOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex, PageOperator next)
    {
        return new LineDecodeOperator(
                openLineOperator(proc, taskSource, processorIndex, next));
    }

    public class LineDecodeOperator
            extends AbstractOperator<LineOperator>
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
