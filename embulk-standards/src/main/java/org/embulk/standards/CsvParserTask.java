package org.embulk.standards;

import com.google.common.base.Optional;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.Task;
import org.embulk.record.SchemaConfig;
import org.embulk.spi.LineDecoderTask;
import org.embulk.time.TimestampParser;
import org.embulk.time.TimestampParserTask;

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

    @Config("trim_if_not_quoted")
    @ConfigDefault("false")
    public boolean getTrimIfNotQuoted();

    @Config("max_quoted_size_limit")
    @ConfigDefault("131072") //128kB
    public long getMaxQuotedSizeLimit();
}
