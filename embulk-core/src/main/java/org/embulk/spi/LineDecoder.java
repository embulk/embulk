package org.embulk.spi;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;

public class LineDecoder
        implements AutoCloseable, Iterable<String>
{
    // TODO optimize

    public static interface DecoderTask
            extends Task
    {
        @Config("charset")
        @ConfigDefault("\"utf-8\"")
        public Charset getCharset();

        @Config("newline")
        @ConfigDefault("\"CRLF\"")
        public Newline getNewline();
    }

    private final FileInputInputStream inputStream;
    private final BufferedReader reader;

    public LineDecoder(FileInput in, DecoderTask task)
    {
        CharsetDecoder decoder = task.getCharset()
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)  // TODO configurable?
            .onUnmappableCharacter(CodingErrorAction.REPLACE);  // TODO configurable?
        this.inputStream = new FileInputInputStream(in);
        this.reader = new BufferedReader(new InputStreamReader(inputStream, decoder));
    }

    public boolean nextFile()
    {
        return inputStream.nextFile();
    }

    public String poll()
    {
        try {
            return reader.readLine();
        } catch (IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
    }

    public void close()
    {
        try {
            reader.close();
        } catch (IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
    }

    public Iterator<String> iterator()
    {
        return new Ite(this);
    }

    private String nextLine;

    private static class Ite
            implements Iterator<String>
    {
        private LineDecoder self;

        public Ite(LineDecoder self)
        {
            // TODO non-static inner class causes a problem with JRuby
            this.self = self;
        }

        @Override
        public boolean hasNext()
        {
            if (self.nextLine != null) {
                return true;
            } else {
                self.nextLine = self.poll();
                return self.nextLine != null;
            }
        }

        @Override
        public String next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String l = self.nextLine;
            self.nextLine = null;
            return l;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
