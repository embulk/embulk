package org.quickload.spi;

import java.nio.charset.Charset;
import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.Task;

public interface LineDecoderTask
        extends Task
{
    @Config("charset")
    @ConfigDefault("\"utf-8\"")
    public Charset getCharset();

    @Config("newline")
    @ConfigDefault("\"CRLF\"")
    public Newline getNewline();
}
