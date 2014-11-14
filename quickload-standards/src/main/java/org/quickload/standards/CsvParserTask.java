package org.quickload.standards;

import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.Task;
import org.quickload.record.SchemaConfig;
import org.quickload.spi.LineDecoderTask;

import javax.validation.constraints.NotNull;

public interface CsvParserTask
        extends Task, LineDecoderTask
{
    @Config("columns")
    @NotNull
    public SchemaConfig getSchemaConfig();

    @Config("column_header") // how to set default value?? TODO @Default("true")
    @ConfigDefault("false")
    public boolean getColumnHeader();

    @Config("delimiter")
    @ConfigDefault("\",\"")
    public char getDelimiterChar();

    @Config("quote")
    @ConfigDefault("\"\\\"\"")
    public char getQuoteChar();

}
