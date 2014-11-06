package org.quickload.spi;

import org.quickload.config.Config;
import org.quickload.config.Task;

public interface LineDecoderTask
        extends Task
{
    @Config(value="in:encoding", defaultValue="\"utf-8\"")
    public String getEncoding();

    @Config(value="in:newline", defaultValue="\"crlf\"")
    public String getNewline();
}
