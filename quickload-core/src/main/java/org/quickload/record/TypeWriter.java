package org.quickload.record;

abstract class TypeWriter
{
    final PageBuilder builder;
    final Column column;

    public TypeWriter(PageBuilder builder, Column column)
    {
        this.builder = builder;
        this.column = column;
    }

    abstract void callRecordWriter(RecordWriter visitor);
}
