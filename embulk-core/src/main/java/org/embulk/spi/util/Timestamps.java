package org.embulk.spi.util;

import com.google.common.base.Optional;
import java.util.Map;
import org.embulk.config.Task;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;
import org.embulk.spi.type.TimestampType;

@Deprecated  // Along with TimestampFormatter and TimestampParser: https://github.com/embulk/embulk/issues/1298
public class Timestamps {
    private Timestamps() {}

    private interface TimestampColumnOption extends Task, org.embulk.spi.time.TimestampParser.TimestampColumnOption {}

    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1289
    public static org.embulk.spi.time.TimestampParser[] newTimestampColumnParsers(
            org.embulk.spi.time.TimestampParser.Task parserTask, SchemaConfig schema) {
        org.embulk.spi.time.TimestampParser[] parsers = new org.embulk.spi.time.TimestampParser[schema.getColumnCount()];
        int i = 0;
        for (ColumnConfig column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                TimestampColumnOption option = column.getOption().loadConfig(TimestampColumnOption.class);
                parsers[i] = org.embulk.spi.time.TimestampParser.of(parserTask, option);
            }
            i++;
        }
        return parsers;
    }

    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1289
    public static org.embulk.spi.time.TimestampFormatter[] newTimestampColumnFormatters(
            org.embulk.spi.time.TimestampFormatter.Task formatterTask, Schema schema,
            Map<String, ? extends org.embulk.spi.time.TimestampFormatter.TimestampColumnOption> columnOptions) {
        org.embulk.spi.time.TimestampFormatter[] formatters = new org.embulk.spi.time.TimestampFormatter[schema.getColumnCount()];
        int i = 0;
        for (Column column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                final Optional<org.embulk.spi.time.TimestampFormatter.TimestampColumnOption> option =
                        Optional.fromNullable(columnOptions.get(column.getName()));
                formatters[i] = org.embulk.spi.time.TimestampFormatter.of(formatterTask, option);
            }
            i++;
        }
        return formatters;
    }
}
