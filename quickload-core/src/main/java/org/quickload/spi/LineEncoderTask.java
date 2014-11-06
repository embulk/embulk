package org.quickload.spi;

import org.quickload.config.Config;
import org.quickload.config.Task;

public interface LineEncoderTask
        extends Task
{
    @Config(value="out:encoding", defaultValue="\"utf-8\"")
    public String getEncoding();

    @Config(value="out:newline", defaultValue="\"CRLF\"")
    public String getNewline();
}
