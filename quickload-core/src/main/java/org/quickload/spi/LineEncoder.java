package org.quickload.spi;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import org.quickload.buffer.Buffer;
import org.quickload.buffer.BufferAllocator;
import org.quickload.channel.BufferOutput;

public class LineEncoder
{
    private static final int MINIMUM_ENCODE_BUFFER_SIZE = 128;  // TODO configurable?

    private final BufferAllocator bufferAllocator;
    private final BufferOutput output;
    private final Charset charset;
    private final byte[] newline;

    public LineEncoder(BufferAllocator bufferAllocator, LineEncoderTask task,
            BufferOutput output)
    {
        this.bufferAllocator = bufferAllocator;
        this.output = output;

        try {
            this.charset = Charset.forName(task.getCharset());
        } catch (UnsupportedCharsetException ex) {
            throw new IllegalArgumentException("Unsupported encoding is set to in.parser.charset", ex);
        }

        switch (task.getNewline()) {
        case "CRLF":
            this.newline = "\r\n".getBytes(charset);
            break;
        case "LF":
            this.newline = "\n".getBytes(charset);
            break;
        case "CR":
            this.newline = "\r".getBytes(charset);
            break;
        default:
            throw new IllegalArgumentException("out.formatter.newline must be either of CRLF, LF, or CR");
        }
    }

    public void addNewLine()
    {
        // TODO optimize
        output.add(Buffer.wrap(newline));
    }

    public void addLine(String line)
    {
        // TODO optimize
        ByteBuffer bb = charset.encode(line);
        output.add(Buffer.wrap(bb.array(), bb.limit()));
        addNewLine();
    }

    public void addText(String text)
    {
        ByteBuffer bb = charset.encode(text);
        Buffer buffer = Buffer.wrap(bb.array(), bb.limit());
        output.add(buffer);
    }

    public void flush()
    {
    }
}
