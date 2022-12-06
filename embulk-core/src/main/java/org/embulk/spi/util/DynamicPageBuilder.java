package org.embulk.spi.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.util.dynamic.SkipColumnSetter;

public class DynamicPageBuilder implements AutoCloseable {
    private final PageBuilder pageBuilder;
    private final Schema schema;
    private final DynamicColumnSetter[] setters;
    private final Map<String, DynamicColumnSetter> columnLookup;

    public static interface BuilderTask extends Task {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public String getDefaultTimeZoneId();

        // The method has been removed: public default org.joda.time.DateTimeZone getDefaultTimeZone()

        @Config("column_options")
        @ConfigDefault("{}")
        public Map<String, ConfigSource> getColumnOptions();
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1321
    private DynamicPageBuilder(
            final DynamicColumnSetterFactory factory,
            final BufferAllocator allocator,
            final Schema schema,
            final PageOutput output) {
        this.pageBuilder = new PageBuilder(allocator, schema, output);
        this.schema = schema;
        final ArrayList<DynamicColumnSetter> setters = new ArrayList<>();
        final LinkedHashMap<String, DynamicColumnSetter> lookup = new LinkedHashMap<>();
        for (Column c : schema.getColumns()) {
            DynamicColumnSetter setter = factory.newColumnSetter(pageBuilder, c);
            setters.add(setter);
            lookup.put(c.getName(), setter);
        }
        this.setters = setters.toArray(new DynamicColumnSetter[0]);
        this.columnLookup = Collections.unmodifiableMap(lookup);
    }

    public static DynamicPageBuilder createWithTimestampMetadataFromBuilderTask(
            BuilderTask task,
            BufferAllocator allocator,
            Schema schema,
            PageOutput output) {
        // TODO configurable default value
        DynamicColumnSetterFactory factory = DynamicColumnSetterFactory.createWithTimestampMetadataFromBuilderTask(
                task, DynamicColumnSetterFactory.nullDefaultValue());
        return new DynamicPageBuilder(factory, allocator, schema, output);
    }

    public static DynamicPageBuilder createWithTimestampMetadataFromColumn(
            BuilderTask task,
            BufferAllocator allocator,
            Schema schema,
            PageOutput output) {
        // TODO configurable default value
        DynamicColumnSetterFactory factory = DynamicColumnSetterFactory.createWithTimestampMetadataFromColumn(
                task, DynamicColumnSetterFactory.nullDefaultValue());
        return new DynamicPageBuilder(factory, allocator, schema, output);
    }

    public List<Column> getColumns() {
        return schema.getColumns();
    }

    public DynamicColumnSetter column(Column c) {
        return setters[c.getIndex()];
    }

    public DynamicColumnSetter column(int index) {
        if (index < 0 || setters.length <= index) {
            throw new DynamicColumnNotFoundException("Column index '" + index + "' is not exist");
        }
        return setters[index];
    }

    public DynamicColumnSetter lookupColumn(String columnName) {
        DynamicColumnSetter setter = columnLookup.get(columnName);
        if (setter == null) {
            throw new DynamicColumnNotFoundException("Column '" + columnName + "' is not exist");
        }
        return setter;
    }

    public DynamicColumnSetter columnOrSkip(int index) {
        if (index < 0 || setters.length <= index) {
            return SkipColumnSetter.get();
        }
        return setters[index];
    }

    public DynamicColumnSetter columnOrSkip(String columnName) {
        DynamicColumnSetter setter = columnLookup.get(columnName);
        if (setter == null) {
            return SkipColumnSetter.get();
        }
        return setter;
    }

    // for jruby
    protected DynamicColumnSetter columnOrNull(int index) {
        if (index < 0 || setters.length <= index) {
            return null;
        }
        return setters[index];
    }

    // for jruby
    protected DynamicColumnSetter columnOrNull(String columnName) {
        return columnLookup.get(columnName);
    }

    public void addRecord() {
        pageBuilder.addRecord();
    }

    public void flush() {
        pageBuilder.flush();
    }

    public void finish() {
        pageBuilder.finish();
    }

    @Override
    public void close() {
        pageBuilder.close();
    }
}
