package org.embulk.spi;

import org.embulk.spi.time.Timestamp;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;
import org.msgpack.value.Value;

class Row
{
    static Row newRow(Schema schema)
    {
        Val[] values = new Val[schema.getColumnCount()];
        for (Column column : schema.getColumns()) {
            values[column.getIndex()] = newValue(column);
        }
        return new Row(values);
    }

    static void writeRow(PageBuilder pageBuilder, Row row)
    {
        for (Val v : row.values) {
            v.writeValue(pageBuilder);
        }
    }

    private static Val newValue(Column column)
    {
        Type type = column.getType();
        if (type.equals(Types.BOOLEAN)) {
            return new BooleanVal(column);
        }
        else if (type.equals(Types.DOUBLE)) {
            return new DoubleVal(column);
        }
        else if (type.equals(Types.LONG)) {
            return new LongVal(column);
        }
        else if (type.equals(Types.STRING)) {
            return new StringVal(column);
        }
        else if (type.equals(Types.JSON)) {
            return new JsonVal(column);
        }
        else if (type.equals(Types.TIMESTAMP)) {
            return new TimestampVal(column);
        }
        else {
            throw new IllegalStateException("Unsupported type " + type.getName());
        }
    }

    private final Val[] values;

    private Row(Val[] values)
    {
        this.values = values;
    }

    void setNull(int columnIndex)
    {
        values[columnIndex].setNull();
    }

    void setBoolean(int columnIndex, boolean value)
    {
        values[columnIndex].set(value);
    }

    void setLong(int columnIndex, long value)
    {
        values[columnIndex].set(value);
    }

    void setDouble(int columnIndex, double value)
    {
        values[columnIndex].set(value);
    }

    void setString(int columnIndex, String value)
    {
        values[columnIndex].set(value);
    }

    void setJson(int columnIndex, Value value)
    {
        values[columnIndex].set(value);
    }

    void setTimestamp(int columnIndex, Timestamp value)
    {
        values[columnIndex].set(value);
    }

    interface Val
    {
        void set(boolean value);

        void set(long value);

        void set(double value);

        void set(String value);

        void set(Value value);

        void set(Timestamp value);

        void setNull();

        void writeValue(PageBuilder pageBuilder);
    }

    static abstract class AbstractVal
            implements Val
    {
        protected final Column column;
        protected boolean isNull;

        protected AbstractVal(Column column)
        {
            this.column = column;
        }

        public void set(boolean value)
        {
            throw new IllegalStateException();
        }

        public void set(long value)
        {
            throw new IllegalStateException();
        }

        public void set(double value)
        {
            throw new IllegalStateException();
        }

        public void set(String value)
        {
            throw new IllegalStateException();
        }

        public void set(Value value)
        {
            throw new IllegalStateException();
        }

        public void set(Timestamp value)
        {
            throw new IllegalStateException();
        }

        public void setNull()
        {
            isNull = true;
        }

        public void writeValue(PageBuilder pageBuilder)
        {
            if (!isNull) {
                writeNotNull(pageBuilder);
            }
            else {
                pageBuilder.writeNull(column.getIndex());
            }
        }

        protected abstract void writeNotNull(PageBuilder pageBuilder);
    }

    static class BooleanVal
            extends AbstractVal
    {
        private boolean value;

        BooleanVal(Column column)
        {
            super(column);
        }

        @Override
        public void set(boolean value)
        {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder)
        {
            pageBuilder.writeBoolean(column.getIndex(), value);
        }
    }

    static class LongVal
            extends AbstractVal
    {
        private long value;

        LongVal(Column column)
        {
            super(column);
        }

        @Override
        public void set(long value)
        {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder)
        {
            pageBuilder.writeLong(column.getIndex(), value);
        }
    }

    static class DoubleVal
            extends AbstractVal
    {
        private double value;

        DoubleVal(Column column)
        {
            super(column);
        }

        @Override
        public void set(double value)
        {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder)
        {
            pageBuilder.writeDouble(column.getIndex(), value);
        }
    }

    static class StringVal
            extends AbstractVal
    {
        private String value;

        StringVal(Column column)
        {
            super(column);
        }

        @Override
        public void set(String value)
        {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder)
        {
            pageBuilder.writeString(column.getIndex(), value);
        }
    }

    static class JsonVal
            extends AbstractVal
    {
        private Value value;

        JsonVal(Column column)
        {
            super(column);
        }

        @Override
        public void set(Value value)
        {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder)
        {
            pageBuilder.writeJson(column.getIndex(), value);
        }
    }

    static class TimestampVal
            extends AbstractVal
    {
        private Timestamp value;

        TimestampVal(Column column)
        {
            super(column);
        }

        @Override
        public void set(Timestamp value)
        {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder)
        {
            pageBuilder.writeTimestamp(column.getIndex(), value);
        }
    }
}