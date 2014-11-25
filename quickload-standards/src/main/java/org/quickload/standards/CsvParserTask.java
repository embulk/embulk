package org.quickload.standards;

import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.Task;
import org.quickload.record.SchemaConfig;
import org.quickload.spi.LineDecoderTask;
import org.quickload.time.TimestampParser;
import org.quickload.time.TimestampParserTask;

import javax.validation.constraints.NotNull;

public interface CsvParserTask
        extends Task, LineDecoderTask, TimestampParserTask
{
    @Config("columns")
    public SchemaConfig getSchemaConfig();

    @Config("header_line") // how to set default value?? TODO @Default("true")
    @ConfigDefault("false")
    public boolean getHeaderLine();

    @Config("delimiter")
    @ConfigDefault("\",\"")
    public char getDelimiterChar();

    @Config("quote")
    @ConfigDefault("\"\\\"\"")
    public char getQuoteChar();

}
