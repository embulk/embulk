package org.embulk.spi;

import java.util.List;

import org.embulk.buffer.Buffer;
import org.embulk.channel.FileBufferChannel;

public class MockFileBufferInput extends FileBufferChannel
{
    protected MockFileBufferInput(List<Buffer> buffers)
    {
        super(Integer.MAX_VALUE);
        for (Buffer buffer : buffers) {
            if (buffer == FileBufferChannel.END_OF_FILE) {
                getOutput().addFile();
            } else {
                getOutput().add(buffer);
            }
        }
        getOutput().addFile();
        completeProducer();
    }
}
