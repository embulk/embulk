package org.embulk.spi.util.dynamic;

import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.json.JsonParser;
import org.embulk.spi.json.JsonParseException;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

public class JsonColumnSetter
        extends AbstractDynamicColumnSetter
{
    private final TimestampFormatter timestampFormatter;
    private final JsonParser jsonParser;

    public JsonColumnSetter(PageBuilder pageBuilder, Column column,
                            DefaultValueSetter defaultValue,
                            TimestampFormatter timestampFormatter)
    {
        super(pageBuilder, column, defaultValue);
        this.timestampFormatter = timestampFormatter;
        this.jsonParser = new JsonParser();
    }

    @Override
    public void setNull()
    {
        pageBuilder.setNull(column);
    }

    @Override
    public void set(boolean v)
    {
        pageBuilder.setJson(column, ValueFactory.newBoolean(v));
    }

    @Override
    public void set(long v)
    {
        pageBuilder.setJson(column, ValueFactory.newInteger(v));
    }

    @Override
    public void set(double v)
    {
        pageBuilder.setJson(column, ValueFactory.newFloat(v));
    }

    @Override
    public void set(String v)
    {
        try {
            pageBuilder.setJson(column, jsonParser.parse(v));
        }
        catch (JsonParseException ex) {
            defaultValue.setTimestamp(pageBuilder, column);
        }
    }

    @Override
    public void set(Timestamp v)
    {
        pageBuilder.setJson(column, ValueFactory.newString(timestampFormatter.format(v)));
    }

    @Override
    public void set(Value v)
    {
        pageBuilder.setJson(column, v);
    }
}
