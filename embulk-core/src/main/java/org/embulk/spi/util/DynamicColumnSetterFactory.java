package org.embulk.spi.util;

import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.type.BooleanType;
import org.embulk.spi.type.DoubleType;
import org.embulk.spi.type.JsonType;
import org.embulk.spi.type.LongType;
import org.embulk.spi.type.StringType;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.type.Type;
import org.embulk.spi.util.dynamic.BooleanColumnSetter;
import org.embulk.spi.util.dynamic.DefaultValueSetter;
import org.embulk.spi.util.dynamic.DoubleColumnSetter;
import org.embulk.spi.util.dynamic.JsonColumnSetter;
import org.embulk.spi.util.dynamic.LongColumnSetter;
import org.embulk.spi.util.dynamic.NullDefaultValueSetter;
import org.embulk.spi.util.dynamic.StringColumnSetter;
import org.embulk.spi.util.dynamic.TimestampColumnSetter;

class DynamicColumnSetterFactory {
    private final DefaultValueSetter defaultValue;
    private final DynamicPageBuilder.BuilderTask task;
    private final boolean useColumnForTimestampMetadata;

    private DynamicColumnSetterFactory(
            final DynamicPageBuilder.BuilderTask task,
            final DefaultValueSetter defaultValue,
            final boolean useColumnForTimestampMetadata) {
        this.defaultValue = defaultValue;
        this.task = task;
        this.useColumnForTimestampMetadata = useColumnForTimestampMetadata;
    }

    static DynamicColumnSetterFactory createWithTimestampMetadataFromBuilderTask(
            final DynamicPageBuilder.BuilderTask task,
            final DefaultValueSetter defaultValue) {
        return new DynamicColumnSetterFactory(task, defaultValue, false);
    }

    static DynamicColumnSetterFactory createWithTimestampMetadataFromColumn(
            final DynamicPageBuilder.BuilderTask task,
            final DefaultValueSetter defaultValue) {
        return new DynamicColumnSetterFactory(task, defaultValue, true);
    }

    public static DefaultValueSetter nullDefaultValue() {
        return new NullDefaultValueSetter();
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1298
    public DynamicColumnSetter newColumnSetter(PageBuilder pageBuilder, Column column) {
        Type type = column.getType();
        if (type instanceof BooleanType) {
            return new BooleanColumnSetter(pageBuilder, column, defaultValue);
        } else if (type instanceof LongType) {
            return new LongColumnSetter(pageBuilder, column, defaultValue);
        } else if (type instanceof DoubleType) {
            return new DoubleColumnSetter(pageBuilder, column, defaultValue);
        } else if (type instanceof StringType) {
            final org.embulk.spi.time.TimestampFormatter formatter = org.embulk.spi.time.TimestampFormatter.of(
                    getTimestampFormatForFormatter(column), getTimeZoneId(column));
            return new StringColumnSetter(pageBuilder, column, defaultValue, formatter);
        } else if (type instanceof TimestampType) {
            // TODO use flexible time format like Ruby's Time.parse
            final org.embulk.spi.time.TimestampParser parser;
            if (this.useColumnForTimestampMetadata) {
                final TimestampType timestampType = (TimestampType) type;
                // https://github.com/embulk/embulk/issues/935
                parser = org.embulk.spi.time.TimestampParser.of(
                        getFormatFromTimestampTypeWithDepracationSuppressed(timestampType), getTimeZoneId(column));
            } else {
                parser = org.embulk.spi.time.TimestampParser.of(getTimestampFormatForParser(column), getTimeZoneId(column));
            }
            return new TimestampColumnSetter(pageBuilder, column, defaultValue, parser);
        } else if (type instanceof JsonType) {
            final org.embulk.spi.time.TimestampFormatter formatter = org.embulk.spi.time.TimestampFormatter.of(
                    getTimestampFormatForFormatter(column), getTimeZoneId(column));
            return new JsonColumnSetter(pageBuilder, column, defaultValue, formatter);
        }
        throw new ConfigException("Unknown column type: " + type);
    }

    private String getTimestampFormatForFormatter(Column column) {
        DynamicPageBuilder.ColumnOption option = getColumnOption(column);
        if (option != null) {
            return option.getTimestampFormatString();
        } else {
            return "%Y-%m-%d %H:%M:%S.%6N";
        }
    }

    private String getTimestampFormatForParser(Column column) {
        DynamicPageBuilder.ColumnOption option = getColumnOption(column);
        if (option != null) {
            return option.getTimestampFormatString();
        } else {
            return "%Y-%m-%d %H:%M:%S.%N";
        }
    }

    private String getTimeZoneId(Column column) {
        DynamicPageBuilder.ColumnOption option = getColumnOption(column);
        if (option != null) {
            return option.getTimeZoneId().or(task.getDefaultTimeZoneId());
        } else {
            return task.getDefaultTimeZoneId();
        }
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1301
    private DynamicPageBuilder.ColumnOption getColumnOption(Column column) {
        ConfigSource option = task.getColumnOptions().get(column.getName());
        if (option != null) {
            return option.loadConfig(DynamicPageBuilder.ColumnOption.class);
        } else {
            return null;
        }
    }

    // TODO: Stop using TimestampType.getFormat.
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/935
    private String getFormatFromTimestampTypeWithDepracationSuppressed(final TimestampType timestampType) {
        return timestampType.getFormat();
    }
}
