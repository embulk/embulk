package org.embulk.spi;

public interface ColumnVisitor
{
    void binaryColumn(Column column);

    void booleanColumn(Column column);

    void longColumn(Column column);

    void doubleColumn(Column column);

    void stringColumn(Column column);

    void timestampColumn(Column column);

    void jsonColumn(Column column);
}
