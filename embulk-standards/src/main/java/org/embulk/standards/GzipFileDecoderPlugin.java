package org.embulk.standards;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigInject;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.FileInput;
import org.embulk.spi.util.FileInputInputStream;
import org.embulk.spi.util.InputStreamFileInput;

public class GzipFileDecoderPlugin
        implements DecoderPlugin
{
    public interface PluginTask
            extends Task
    {
        @ConfigInject
        BufferAllocator getBufferAllocator();
    }

    @Override
    public void transaction(ConfigSource config, DecoderPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump());
    }

    @Override
    public FileInput open(TaskSource taskSource, FileInput fileInput)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        final FileInputInputStream files = new FileInputInputStream(fileInput);
        return new InputStreamFileInput(
                task.getBufferAllocator(),
                new InputStreamFileInput.Provider() {
                    public InputStream openNext() throws IOException
                    {
                        if (!files.nextFile()) {
                            return null;
                        }
                        return new MultiStreamGZIPInputStream(files, 8*1024);
                    }

                    public void close() throws IOException
                    {
                        files.close();
                    }
                });
    }

    // This is necessary to support a gzip file that contains multiple deflate streams.
    // GZIPInputStream returns -1 if it reaches end of a deflate stream. But next read() call
    // returns 1 or larger if the file contains multiple deflate streams.
    private static class MultiStreamGZIPInputStream
            extends FilterInputStream
    {
        public MultiStreamGZIPInputStream(InputStream in, int size)
            throws IOException
        {
            super(new GZIPInputStream(in, size));
        }

        @Override
        public int read()
            throws IOException
        {
            int r = in.read();
            if (r < 0) {
                return in.read();
            }
            return r;
        }

        @Override
        public int read(byte[] b)
            throws IOException
        {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len)
            throws IOException
        {
            int r = in.read(b, off, len);
            if (r < 0) {
                return in.read(b, off, len);
            }
            return r;
        }

        @Override
        public long skip(long n)
            throws IOException
        {
            long r = in.skip(n);
            if (r < 0L) {
                return in.skip(n);
            }
            return r;
        }
    }
}
