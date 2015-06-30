package org.embulk.spi.util;

import org.embulk.config.Task;
import org.embulk.spi.Schema;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.SchemaConfig;

public class Timestamps
{
    private Timestamps() { }

    private interface TimestampColumnOption
            extends Task, TimestampParser.TimestampColumnOption
    { }

    public static TimestampParser[] newTimestampColumnParsers(
            TimestampParser.Task parserTask, SchemaConfig schema)
    {
        TimestampParser[] parsers = new TimestampParser[schema.getColumnCount()];
        int i = 0;
        for (ColumnConfig column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                TimestampColumnOption option = column.getOption().loadConfig(TimestampColumnOption.class);
                parsers[i] = new TimestampParser(parserTask, option);
            }
            i++;
        }
        return parsers;
    }

    private interface TimestampColumnOption
            extends Task, TimestampFormatter.TimestampColumnOption
    { }

    public static TimestampFormatter[] newTimestampColumnFormatters(
            TimestampFormatter.Task formatterTask, Schema schema,
            Map<String, TimestampColumnOption> columnOptions)
    {
        TimestampFormatter[] formatters = new TimestampFormatter[schema.getColumnCount()];
        int i = 0;
        for (Column column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                Optional<TimestampColumnOption> option = Optional.fromNullable(columnOptions.get(column.getName()));
                formatters[i] = new TimestampFormatter(formatterTask, option);
            }
            i++;
        }
        return formatters;
    }
}
