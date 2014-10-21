package org.quickload.spi;

import org.quickload.config.Config;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.buffer.Buffer;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

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
        private long lineNum = 0;
        private StringBuilder sbuf;

        public LineDecodeOperator(LineOperator next)
        {
            super(next);
        }

        public void addBuffer(Buffer buffer)
        {
            // TODO needs internal buffer
            sbuf = new StringBuilder();
            // TODO use streaming decoder
            Charset charset = Charset.forName("UTF-8");
            CharBuffer cb = charset.decode(buffer.getBuffer());

            for (int i = 0; i < cb.capacity(); i++) {
                if (cb.get(i) != '\n') {
                    sbuf.append(cb.get(i));
                } else {
                    if (sbuf.length() != 0) {
                        lineNum++;
                    }
                    next.addLine(sbuf.toString());
                    sbuf = new StringBuilder();
                }
            }
        }
    }
}
