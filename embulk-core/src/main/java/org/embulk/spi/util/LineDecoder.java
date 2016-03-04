package org.embulk.spi.util;

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
import org.embulk.spi.FileInput;
import static java.nio.charset.StandardCharsets.UTF_8;

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
    private final Charset charset;

    public LineDecoder(FileInput in, DecoderTask task)
    {
        this.charset = task.getCharset();
        CharsetDecoder decoder = charset
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)  // TODO configurable?
            .onUnmappableCharacter(CodingErrorAction.REPLACE);  // TODO configurable?
        this.inputStream = new FileInputInputStream(in);
        this.reader = new BufferedReader(new InputStreamReader(inputStream, decoder));
    }

    public boolean nextFile()
    {
        boolean has = inputStream.nextFile();
        if (has && charset.equals(UTF_8)) {
            skipBom();
        }
        return has;
    }

    private void skipBom()
    {
        boolean skip = false;
        try {
            if (charset.equals(UTF_8)) {
                reader.mark(3);
                int firstChar = reader.read();
                if (firstChar == 0xFEFF) {
                    // skip BOM bytes
                    skip = true;
                }
            }
        }
        catch (IOException ex) {
        }
        finally {
            if (skip) {
                // firstChar is skipped
            }
            else {
                // rollback to the marked position
                try {
                    reader.reset();
                }
                catch (IOException ex) {
                    // unexpected
                    throw new RuntimeException(ex);
                }
            }
        }
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
