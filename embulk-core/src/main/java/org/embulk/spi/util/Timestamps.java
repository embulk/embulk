package org.embulk.spi.util;

import java.util.Optional;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.Task;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.SchemaConfig;
import org.embulk.spi.type.TimestampType;

@Deprecated  // Along with TimestampFormatter and TimestampParser: https://github.com/embulk/embulk/issues/1298
public class Timestamps {
    private Timestamps() {}

    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1289
    public static org.embulk.spi.time.TimestampParser[] newTimestampColumnParsers(
            org.embulk.spi.time.TimestampParser.Task parserTask, SchemaConfig schema) {
        org.embulk.spi.time.TimestampParser[] parsers = new org.embulk.spi.time.TimestampParser[schema.getColumnCount()];
        int i = 0;
        for (ColumnConfig column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                final ParserTimestampColumnOption option = column.getOption().loadConfig(ParserTimestampColumnOption.class);
                parsers[i] = org.embulk.spi.time.TimestampParser.of(
                        option.getFormat().orElse(parserTask.getDefaultTimestampFormat()),
                        option.getTimeZoneId().orElse(parserTask.getDefaultTimeZoneId()),
                        option.getDate().orElse(parserTask.getDefaultDate()));
            }
            i++;
        }
        return parsers;
    }

    private interface ParserTimestampColumnOption extends Task {
        // From org.embulk.spi.time.TimestampParser.TimestampColumnOption.
        @Config("timezone")
        @ConfigDefault("null")
        public Optional<String> getTimeZoneId();

        // From org.embulk.spi.time.TimestampParser.TimestampColumnOption.
        @Config("format")
        @ConfigDefault("null")
        public Optional<String> getFormat();

        // From org.embulk.spi.time.TimestampParser.TimestampColumnOption.
        @Config("date")
        @ConfigDefault("null")
        public Optional<String> getDate();
    }

    // The method has been removed:
    // public static org.embulk.spi.time.TimestampFormatter[] newTimestampColumnFormatters(
    //         org.embulk.spi.time.TimestampFormatter.Task formatterTask,
    //         Schema schema,
    //         Map<String, ? extends org.embulk.spi.time.TimestampFormatter.TimestampColumnOption> columnOptions)
}
