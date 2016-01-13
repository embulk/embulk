package org.embulk.spi.util;

import java.util.List;
import java.util.ArrayList;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.Schema;
import org.embulk.spi.Column;
import org.embulk.spi.PageReader;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.type.TimestampType;

public class PagePrinter
{
    private final Schema schema;
    private final TimestampFormatter[] timestampFormatters;
    private final ArrayList<String> record;

    public PagePrinter(Schema schema, TimestampFormatter.FormatterTask task)
    {
        this.schema = schema;
        this.timestampFormatters = new TimestampFormatter[schema.getColumnCount()];
        for (int i=0; i < timestampFormatters.length; i++) {
            if (schema.getColumnType(i) instanceof TimestampType) {
                TimestampType type = (TimestampType) schema.getColumnType(i);
                timestampFormatters[i] = new TimestampFormatter(type.getFormat(), task);
            }
        }

        this.record = new ArrayList<String>(schema.getColumnCount());
        for (int i=0; i < schema.getColumnCount(); i++) {
            record.add("");
        }
    }

    public String printRecord(PageReader reader, String delimiter)
    {
        StringBuilder sb = new StringBuilder();
        for (Column c : schema.getColumns()) {
            if (c.getIndex() != 0) {
                sb.append(delimiter);
            }
            sb.append(printColumn(reader, c));
        }
        return sb.toString();
    }

    public List<String> printRecord(PageReader reader)
    {
        for (Column c : schema.getColumns()) {
            record.set(c.getIndex(), printColumn(reader, c));
        }
        return record;
    }

    public String printColumn(PageReader reader, Column column)
    {
        if (reader.isNull(column)) {
            return "";
        }
        ToStringColumnVisitor visitor = new ToStringColumnVisitor(reader);
        column.visit(visitor);
        return visitor.string;
    }

    private class ToStringColumnVisitor
            implements ColumnVisitor
    {
        private final PageReader reader;
        String string = "";

        public ToStringColumnVisitor(PageReader reader)
        {
            this.reader = reader;
        }

        public void booleanColumn(Column column)
        {
            string = Boolean.toString(reader.getBoolean(column));
        }

        public void longColumn(Column column)
        {
            string = Long.toString(reader.getLong(column));
        }

        public void doubleColumn(Column column)
        {
            string = Double.toString(reader.getDouble(column));
        }

        public void stringColumn(Column column)
        {
            string = reader.getString(column);
        }

        public void timestampColumn(Column column)
        {
            string = timestampFormatters[column.getIndex()].format(reader.getTimestamp(column));
        }

        public void jsonColumn(Column column)
        {
            string = reader.getJson(column).toString();
        }
    }
}
