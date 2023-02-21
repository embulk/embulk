package org.embulk.spi.util.dynamic;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.json.JsonValue;
import org.msgpack.value.Value;

public class BooleanColumnSetter extends AbstractDynamicColumnSetter {
    private static final String[] TRUE_STRINGS_ARRAY = {
            "true", "True", "TRUE",
            "yes", "Yes", "YES",
            "t", "T", "y", "Y",
            "on", "On",
            "ON", "1"};

    private static final Set<String> TRUE_STRINGS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(TRUE_STRINGS_ARRAY)));

    public BooleanColumnSetter(PageBuilder pageBuilder, Column column,
            DefaultValueSetter defaultValue) {
        super(pageBuilder, column, defaultValue);
    }

    @Override
    public void setNull() {
        pageBuilder.setNull(column);
    }

    @Override
    public void set(boolean v) {
        pageBuilder.setBoolean(column, v);
    }

    @Override
    public void set(long v) {
        pageBuilder.setBoolean(column, v > 0);
    }

    @Override
    public void set(double v) {
        pageBuilder.setBoolean(column, v > 0.0);
    }

    @Override
    public void set(String v) {
        if (TRUE_STRINGS.contains(v)) {
            pageBuilder.setBoolean(column, true);
        } else {
            defaultValue.setBoolean(pageBuilder, column);
        }
    }

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void set(org.embulk.spi.time.Timestamp v) {
        defaultValue.setBoolean(pageBuilder, column);
    }

    @Override
    public void set(Instant v) {
        defaultValue.setBoolean(pageBuilder, column);
    }

    @Deprecated
    @Override
    public void set(Value v) {
        defaultValue.setBoolean(pageBuilder, column);
    }

    @Override
    public void set(final JsonValue v) {
        defaultValue.setBoolean(pageBuilder, column);
    }
}
