package org.embulk.spi.util;

import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.embulk.spi.FileOutput;
import org.embulk.spi.BufferAllocator;

public class LineEncoder
        implements AutoCloseable
{
    // TODO optimize

    public interface EncoderTask
            extends Task
    {
        @Config("charset")
        @ConfigDefault("\"utf-8\"")
        public Charset getCharset();

        @Config("newline")
        @ConfigDefault("\"CRLF\"")
        public Newline getNewline();

        @ConfigInject
        public BufferAllocator getBufferAllocator();
    }

    private final String newline;
    private final FileOutput underlyingFileOutput;
    private final FileOutputOutputStream outputStream;
    private Writer writer;

    public LineEncoder(FileOutput out, EncoderTask task)
    {
        CharsetEncoder encoder = task.getCharset()
            .newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)  // TODO configurable?
            .onUnmappableCharacter(CodingErrorAction.REPLACE);  // TODO configurable?
        this.newline = task.getNewline().getString();
        this.underlyingFileOutput = out;
        this.outputStream = new FileOutputOutputStream(underlyingFileOutput, task.getBufferAllocator(), FileOutputOutputStream.CloseMode.FLUSH_FINISH);
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, encoder), 32*1024);
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
        try {
            writer.flush();
        } catch (IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
        outputStream.nextFile();
    }

    public void finish()
    {
        try {
            if (writer != null) {
                writer.close();  // FLUSH_FINISH
                writer = null;
                // underlyingFileOutput.finish() is already called by close() because CloseMode is FLUSH_FINISH
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close()
    {
        try {
            if (writer != null) {
                writer.close();  // FLUSH_FINISH
                writer = null;
            }
            underlyingFileOutput.close();  // this is necessary because CloseMode is not FLUSH_FINISH_CLOSE
        } catch (IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
    }
}
