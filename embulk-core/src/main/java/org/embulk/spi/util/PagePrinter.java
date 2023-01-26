package org.embulk.spi.util;

import java.util.ArrayList;
import java.util.List;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.type.TimestampType;

public class PagePrinter {
    private final Schema schema;

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1298
    private final org.embulk.spi.time.TimestampFormatter[] timestampFormatters;

    private final ArrayList<String> record;

    // The constructor has been removed: public PagePrinter(final Schema schema, final org.joda.time.DateTimeZone timezone)

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1298
    public PagePrinter(final Schema schema, final String timeZoneId) {
        this.schema = schema;
        this.timestampFormatters = new org.embulk.spi.time.TimestampFormatter[schema.getColumnCount()];
        for (int i = 0; i < timestampFormatters.length; i++) {
            if (schema.getColumnType(i) instanceof TimestampType) {
                TimestampType type = (TimestampType) schema.getColumnType(i);
                timestampFormatters[i] = org.embulk.spi.time.TimestampFormatter.of(
                        getFormatFromTimestampTypeWithDeprecationSuppressed(type), timeZoneId);
            }
        }

        this.record = new ArrayList<String>(schema.getColumnCount());
        for (int i = 0; i < schema.getColumnCount(); i++) {
            record.add("");
        }
    }

    public String printRecord(PageReader reader, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (Column c : schema.getColumns()) {
            if (c.getIndex() != 0) {
                sb.append(delimiter);
            }
            sb.append(printColumn(reader, c));
        }
        return sb.toString();
    }

    public List<String> printRecord(PageReader reader) {
        for (Column c : schema.getColumns()) {
            record.set(c.getIndex(), printColumn(reader, c));
        }
        return record;
    }

    public String printColumn(PageReader reader, Column column) {
        if (reader.isNull(column)) {
            return "";
        }
        ToStringColumnVisitor visitor = new ToStringColumnVisitor(reader);
        column.visit(visitor);
        return visitor.string;
    }

    private class ToStringColumnVisitor implements ColumnVisitor {
        private final PageReader reader;
        String string = "";

        public ToStringColumnVisitor(PageReader reader) {
            this.reader = reader;
        }

        public void booleanColumn(Column column) {
            string = Boolean.toString(reader.getBoolean(column));
        }

        public void longColumn(Column column) {
            string = Long.toString(reader.getLong(column));
        }

        public void doubleColumn(Column column) {
            string = Double.toString(reader.getDouble(column));
        }

        public void stringColumn(Column column) {
            string = reader.getString(column);
        }

        @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
        public void timestampColumn(Column column) {
            string = timestampFormatters[column.getIndex()].format(reader.getTimestamp(column));
        }

        public void jsonColumn(Column column) {
            this.string = this.reader.getJsonValue(column).toString();
        }
    }

    // TODO: Stop using TimestampType.getFormat.
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/935
    private String getFormatFromTimestampTypeWithDeprecationSuppressed(final TimestampType timestampType) {
        return timestampType.getFormat();
    }
}
