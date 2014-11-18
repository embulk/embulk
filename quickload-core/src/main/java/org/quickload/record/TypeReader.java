package org.quickload.record;

abstract class TypeReader
{
    final PageReader reader;
    final Column column;

    TypeReader(PageReader reader, Column column)
    {
        this.reader = reader;
        this.column = column;
    }

    abstract void callRecordReader(RecordReader visitor);
}
