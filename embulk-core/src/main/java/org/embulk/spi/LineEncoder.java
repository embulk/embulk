package org.embulk.spi;

import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import com.fasterxml.jackson.annotation.JacksonInject;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;

public class LineEncoder
        implements AutoCloseable
{
    // TODO optimize

    public interface Task
            extends org.embulk.config.Task
    {
        @Config("charset")
        @ConfigDefault("\"utf-8\"")
        public Charset getCharset();

        @Config("newline")
        @ConfigDefault("\"CRLF\"")
        public Newline getNewline();

        @JacksonInject
        public BufferAllocator getBufferAllocator();
    }

    private final String newline;
    private final FileOutputOutputStream outputStream;
    private final Writer writer;

    public LineEncoder(FileOutput out, Task task)
    {
        CharsetEncoder encoder = task.getCharset()
            .newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)  // TODO configurable?
            .onUnmappableCharacter(CodingErrorAction.REPLACE);  // TODO configurable?
        this.newline = task.getNewline().getString();
        this.outputStream = new FileOutputOutputStream(out, task.getBufferAllocator());
        this.writer = new OutputStreamWriter(outputStream, encoder);
    }

    public void addNewLine()
    {
        try {
            writer.append(newline);
        } catch (IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
    }

    public void addLine(String line)
    {
        try {
            writer.append(line);
        } catch (IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
        addNewLine();
    }

    public void addText(String text)
    {
        try {
            writer.append(text);
        } catch (IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
    }

    public void nextFile()
    {
        outputStream.nextFile();
    }

    public void finish()
    {
        outputStream.finish();
    }

    @Override
    public void close()
    {
        try {
            writer.close();
        } catch (IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
    }
}
