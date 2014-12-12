package org.embulk.spi;

import java.nio.charset.Charset;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.Task;

public interface LineEncoderTask
        extends Task
{
    @Config("charset")
    @ConfigDefault("\"utf-8\"")
    public Charset getCharset();

    @Config("newline")
    @ConfigDefault("\"CRLF\"")
    public Newline getNewline();
}
