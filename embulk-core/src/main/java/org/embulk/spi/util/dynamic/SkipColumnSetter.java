package org.embulk.spi.util.dynamic;

import java.time.Instant;
import org.embulk.spi.json.JsonValue;
import org.msgpack.value.Value;

public class SkipColumnSetter extends AbstractDynamicColumnSetter {
    private static final SkipColumnSetter instance = new SkipColumnSetter();

    public static SkipColumnSetter get() {
        return instance;
    }

    private SkipColumnSetter() {
        super(null, null, null);
    }

    @Override
    public void setNull() {}

    @Override
    public void set(boolean v) {}

    @Override
    public void set(long v) {}

    @Override
    public void set(double v) {}

    @Override
    public void set(String v) {}

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void set(final org.embulk.spi.time.Timestamp v) {}

    @Override
    public void set(Instant v) {}

    @Deprecated
    @Override
    public void set(Value v) {}

    @Override
    public void set(final JsonValue v) {}
}
