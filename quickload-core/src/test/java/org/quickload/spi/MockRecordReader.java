package org.quickload.spi;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.quickload.buffer.Buffer;
import org.quickload.channel.FileBufferOutput;
import org.quickload.record.Column;
import org.quickload.record.RecordReader;
import org.quickload.time.Timestamp;

public class MockRecordReader implements RecordReader
{
    private FileBufferOutput fileBufferOutput;

    private List<List<Object>> records;

    private List<Object> columns;

    public MockRecordReader()
    {
        this(null);
    }

    public MockRecordReader(FileBufferOutput fileBufferOutput)
    {
        this.fileBufferOutput = fileBufferOutput;
    }

    @Override
    public void readNull(Column column)
    {
        addColumn(null);
    }

    @Override
    public void readBoolean(Column column, boolean value)
    {
        addColumn(value);
    }

    @Override
    public void readLong(Column column, long value)
    {
        addColumn(value);
    }

    @Override
    public void readDouble(Column column, double value)
    {
        addColumn(value);
    }

    @Override
    public void readString(Column column, String value)
    {
        addColumn(value);
    }

    @Override
    public void readTimestamp(Column column, Timestamp value)
    {
        addColumn(value);
    }

    public List<List<Object>> getRecords()
    {
        return records;
    }

    public void addRecord()
    {
        if (records == null) {
            records = new ArrayList<>();
        }
        records.add(columns);
        if (fileBufferOutput != null) {
            try {
                byte[] bytes = columns.toString().getBytes("UTF-8");
                fileBufferOutput.add(Buffer.copyOf(bytes));
            } catch (UnsupportedEncodingException uee) {
                throw new IllegalStateException(uee.getMessage(), uee);
            }
        }
        columns = null;
    }

    private void addColumn(Object value)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(value);
    }
}
