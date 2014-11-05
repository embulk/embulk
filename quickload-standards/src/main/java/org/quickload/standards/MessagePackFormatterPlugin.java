package org.quickload.standards;

import com.google.inject.Inject;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;
import org.msgpack.packer.Packer;
import org.quickload.buffer.Buffer;
import org.quickload.buffer.BufferAllocator;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.record.Column;
import org.quickload.record.Page;
import org.quickload.record.PageReader;
import org.quickload.record.RecordConsumer;
import org.quickload.record.RecordCursor;
import org.quickload.record.Schema;
import org.quickload.channel.PageInput;
import org.quickload.channel.FileBufferOutput;
import org.quickload.spi.FormatterPlugin;
import org.quickload.spi.ProcTask;

import java.io.IOException;

public class MessagePackFormatterPlugin
        implements FormatterPlugin
{
    public interface PluginTask
            extends Task
    {
    }

    @Override
    public TaskSource getFormatterTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = proc.loadConfig(config, PluginTask.class);
        return proc.dumpTask(task);
    }

    @Override
    public void runFormatter(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput)
    {
        Schema schema = proc.getSchema();
        BufferAllocator bufferAllocator = proc.getBufferAllocator();
        PageReader pageReader = new PageReader(schema);
        MessagePack msgpack = new MessagePack();

        for (Page page : pageInput) {
            // TODO simple implementation
            final BufferPacker packer = msgpack.createBufferPacker(); // TODO

            RecordCursor recordCursor = pageReader.cursor(page);

            while (recordCursor.next()) {
                try {
                    packer.writeMapBegin(schema.getColumns().size());
                } catch (IOException e) { // TODO better handling
                    e.printStackTrace(); // TODO
                }

                schema.consume(recordCursor, new RecordConsumer() {
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
                });

                try {
                    packer.writeMapEnd();
                } catch (IOException e) { // TODO better handling
                    e.printStackTrace(); // TODO
                }
            }

            byte[] bytes = packer.toByteArray();
            Buffer buf = bufferAllocator.allocateBuffer(bytes.length); // TODO
            buf.write(bytes, 0, bytes.length);
            fileBufferOutput.add(buf);
        }
        fileBufferOutput.addFile();
    }
}
