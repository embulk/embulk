package org.embulk.exec;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Buffer;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.TransactionalFileInput;

import java.util.List;

public class BufferFileInputPlugin
        implements FileInputPlugin
{
    private Buffer buffer;

    public BufferFileInputPlugin(Buffer buffer)
    {
        this.buffer = buffer;
    }

    public ConfigDiff transaction(ConfigSource config, FileInputPlugin.Control control)
    {
        control.run(Exec.newTaskSource(), 1);
        return Exec.newConfigDiff();
    }

    public ConfigDiff resume(TaskSource taskSource,
            int taskCount,
            FileInputPlugin.Control control)
    {
        throw new UnsupportedOperationException();
    }

    public void cleanup(TaskSource taskSource,
            int taskCount,
            List<TaskReport> successTaskReports)
    {
        if (buffer != null) {
            buffer.release();
            buffer = null;
        }
    }

    public TransactionalFileInput open(TaskSource taskSource, int taskIndex)
    {
        return new BufferTransactionalFileInput(buffer);
    }

    private static class BufferTransactionalFileInput
            implements TransactionalFileInput
    {
        private Buffer buffer;

        public BufferTransactionalFileInput(Buffer buffer)
        {
            this.buffer = buffer;
        }

        @Override
        public Buffer poll()
        {
            Buffer b = buffer;
            buffer = null;
            return b;
        }

        @Override
        public boolean nextFile()
        {
            return buffer != null;
        }

        @Override
        public void close() { }

        @Override
        public void abort() { }

        @Override
        public TaskReport commit()
        {
            return null;
        }
    }
}