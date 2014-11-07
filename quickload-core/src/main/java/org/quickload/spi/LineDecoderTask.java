package org.quickload.spi;

import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.Task;

public interface LineDecoderTask
        extends Task
{
    @Config("charset")
    @ConfigDefault("\"utf-8\"")
    public String getCharset();

    @Config("newline")
    @ConfigDefault("\"CRLF\"")
    public String getNewline();
}
