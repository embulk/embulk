package org.embulk.spi.util;

import org.embulk.spi.type.Type;
import org.embulk.spi.type.BooleanType;
import org.embulk.spi.type.LongType;
import org.embulk.spi.type.DoubleType;
import org.embulk.spi.type.StringType;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.util.dynamic.BooleanColumnSetter;
import org.embulk.spi.util.dynamic.LongColumnSetter;
import org.embulk.spi.util.dynamic.DoubleColumnSetter;
import org.embulk.spi.util.dynamic.StringColumnSetter;
import org.embulk.spi.util.dynamic.TimestampColumnSetter;
import org.embulk.spi.util.dynamic.DefaultValueSetter;
import org.embulk.spi.util.dynamic.NullDefaultValueSetter;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.config.ConfigException;

public class DynamicColumnSetterFactory
{
    private final DefaultValueSetter defaultValue;

    public DynamicColumnSetterFactory(DefaultValueSetter defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public static DefaultValueSetter nullDefaultValue()
    {
        return new NullDefaultValueSetter();
    }

    public DynamicColumnSetter newColumnSetter(PageBuilder pageBuilder, Column column)
    {
        Type type = column.getType();
        if (type instanceof BooleanType) {
            return new BooleanColumnSetter(pageBuilder, column, defaultValue);
        } else if (type instanceof LongType) {
            return new LongColumnSetter(pageBuilder, column, defaultValue);
        } else if (type instanceof DoubleType) {
            return new DoubleColumnSetter(pageBuilder, column, defaultValue);
        } else if (type instanceof StringType) {
            TimestampFormatter formatter = null;  // TODO
            return new StringColumnSetter(pageBuilder, column, defaultValue, formatter);
        } else if (type instanceof TimestampType) {
            TimestampParser parser = null;  // TODO
            return new TimestampColumnSetter(pageBuilder, column, defaultValue, parser);
        }
        throw new ConfigException("Unknown column type: "+type);
    }
}
