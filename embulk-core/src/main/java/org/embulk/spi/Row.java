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
        values[columnIndex].setBoolean(value);
    }

    void setLong(int columnIndex, long value)
    {
        values[columnIndex].setLong(value);
    }

    void setDouble(int columnIndex, double value)
    {
        values[columnIndex].setDouble(value);
    }

    void setString(int columnIndex, String value)
    {
        values[columnIndex].setString(value);
    }

    void setJson(int columnIndex, Value value)
    {
        values[columnIndex].setJson(value);
    }

    void setTimestamp(int columnIndex, Timestamp value)
    {
        values[columnIndex].setTimestamp(value);
    }

    void write(PageBuilder pageBuilder)
    {
        for (Val v : values) {
            v.write(pageBuilder);
        }
    }

    interface Val
    {
        void setBoolean(boolean value);

        void setLong(long value);

        void setDouble(double value);

        void setString(String value);

        void setJson(Value value);

        void setTimestamp(Timestamp value);

        void setNull();

        void write(PageBuilder pageBuilder);
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

        public void setBoolean(boolean value)
        {
            throw new IllegalStateException("Not reach here");
        }

        public void setLong(long value)
        {
            throw new IllegalStateException("Not reach here");
        }

        public void setDouble(double value)
        {
            throw new IllegalStateException("Not reach here");
        }

        public void setString(String value)
        {
            throw new IllegalStateException("Not reach here");
        }

        public void setJson(Value value)
        {
            throw new IllegalStateException("Not reach here");
        }

        public void setTimestamp(Timestamp value)
        {
            throw new IllegalStateException("Not reach here");
        }

        public void setNull()
        {
            isNull = true;
        }

        public void write(PageBuilder pageBuilder)
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
        public void setBoolean(boolean value)
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
        public void setLong(long value)
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
        public void setDouble(double value)
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
        public void setString(String value)
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
        public void setJson(Value value)
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
        public void setTimestamp(Timestamp value)
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