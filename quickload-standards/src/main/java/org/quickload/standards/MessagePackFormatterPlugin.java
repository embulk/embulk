package org.quickload.standards;

import com.google.inject.Inject;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;
import org.msgpack.packer.Packer;
import org.quickload.buffer.Buffer;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.exec.BufferManager;
import org.quickload.record.*;
import org.quickload.spi.AbstractOperator;
import org.quickload.spi.BufferOperator;
import org.quickload.spi.FormatterPlugin;
import org.quickload.spi.PageOperator;
import org.quickload.spi.ProcTask;
import org.quickload.spi.Report;

import java.io.IOException;

public class MessagePackFormatterPlugin
        implements FormatterPlugin
{
    private final BufferManager bufferManager;

    @Inject
    public MessagePackFormatterPlugin(BufferManager bufferManager) { this.bufferManager = bufferManager; }

    public interface PluginTask
            extends Task
    {
    }

    @Override
    public TaskSource getFormatterTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = config.loadTask(PluginTask.class);
        return config.dumpTask(task);
    }

    @Override
    public PageOperator openPageOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex, BufferOperator next)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        return new PluginOperator(proc.getSchema(), processorIndex, next);
    }

    @Override
    public void shutdown()
    {
        // TODO
    }

    class PluginOperator
            extends AbstractOperator<BufferOperator>
            implements PageOperator
    {
        private final Schema schema;
        private final PageReader pageReader;
        private final int processorIndex;
        private final MessagePack msgpack;

        private PluginOperator(Schema schema, int processorIndex, BufferOperator next)
        {
            super(next);
            this.schema = schema;
            this.pageReader = new PageReader(bufferManager, schema);
            this.processorIndex = processorIndex;
            this.msgpack = new MessagePack();
        }

        @Override
        public void addPage(Page page)
        {
            // TODO simple implementation
            final BufferPacker packer = msgpack.createBufferPacker(); // TODO

            RecordCursor recordCursor = pageReader.cursor(page);

            while (recordCursor.next()) {
                RecordConsumer recordConsumer = new RecordConsumer()
                {
                    @Override
                    public void setNull(Column column)
                    {
                        try {
                            writeColumnName(packer, column);
                            packer.writeNil();
                        } catch (IOException e) { // TOOD should handle IOException here?
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void setLong(Column column, long value)
                    {
                        try {
                            writeColumnName(packer, column);
                            packer.write(value);
                        } catch (IOException e) { // TOOD should handle IOException here?
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void setDouble(Column column, double value) {
                        try {
                            writeColumnName(packer, column);
                            packer.write(value);
                        } catch (IOException e) { // TOOD should handle IOException here?
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void setString(Column column, String value) {
                        try {
                            writeColumnName(packer, column);
                            packer.write(value);
                        } catch (IOException e) { // TOOD should handle IOException here?
                            e.printStackTrace();
                        }
                    }

                    private void writeColumnName(Packer packer, Column column)
                            throws IOException
                    {
                        packer.write(column.getName());
                    }
                };

                try {
                    packer.writeMapBegin(schema.getColumns().size());
                    schema.consume(recordCursor, recordConsumer);
                    packer.writeMapEnd();
                } catch (IOException e) { // TODO better handling
                    e.printStackTrace(); // TODO
                }
            }

            byte[] bytes = packer.toByteArray();
            Buffer buf = bufferManager.allocateBuffer(bytes.length); // TODO
            buf.write(bytes, 0, bytes.length);
            next.addBuffer(buf);
        }

        @Override
        public Report completed()
        {
            return null; // TODO
        }

        @Override
        public void close() throws Exception
        {
            // TODO
        }
    }
}
