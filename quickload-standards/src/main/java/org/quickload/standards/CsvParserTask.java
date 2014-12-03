package org.quickload.standards;

import com.google.common.base.Optional;
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

    @Config("escape")
    @ConfigDefault("\"\\\\\"")
    public char getEscapeChar();

    // Null value handling: if the CsvParser found 'non-quoted empty string's,
    // it replaces them to string that users specified like "\N", "NULL".
    @Config("null_string")
    @ConfigDefault("null")
    public Optional<String> getNullString();

    @Config("trimmed_if_not_quoted")
    @ConfigDefault("false")
    public boolean getTrimmedIfNotQuoted();

    @Config("max_quoted_size_limit")
    @ConfigDefault("131072") //128kB
    public long getMaxQuotedSizeLimit();
}
