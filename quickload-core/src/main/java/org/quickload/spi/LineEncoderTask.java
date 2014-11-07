package org.quickload.spi;

import org.quickload.config.Config;
import org.quickload.config.Task;

public interface LineEncoderTask
        extends Task
{
    @Config(value="charset", defaultValue="\"utf-8\"")
    public String getCharset();

    @Config(value="newline", defaultValue="\"CRLF\"")
    public String getNewline();
}
