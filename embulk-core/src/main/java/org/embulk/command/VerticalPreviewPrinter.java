package org.embulk.command;

import java.io.PrintStream;
import java.io.IOException;
import org.embulk.config.ModelManager;
import org.embulk.spi.Schema;

public class VerticalPreviewPrinter
        extends PreviewPrinter
{
    private final String format;
    private int count = 0;

    public VerticalPreviewPrinter(PrintStream out, ModelManager modelManager, Schema schema)
    {
        super(out, modelManager, schema);
        this.format = "%" + maxColumnNameLength(schema) + "s (%" + maxColumnTypeNameLength(schema)+ "s) : %s%n";
    }

    private static int maxColumnNameLength(Schema schema)
    {
        int max = 0;
        for (int i=0; i < schema.getColumnCount(); i++) {
            max = Math.max(max, schema.getColumnName(i).length());
        }
        return max;
    }

    private static int maxColumnTypeNameLength(Schema schema)
    {
        int max = 0;
        for (int i=0; i < schema.getColumnCount(); i++) {
            max = Math.max(max, schema.getColumnType(i).toString().length());
        }
        return max;
    }

    @Override
    protected void printRecord(String[] values) throws IOException
    {
        count++;
        out.format("*************************** %d ***************************%n", count);
        for (int i=0; i < schema.getColumnCount(); i++) {
            out.format(format, schema.getColumnName(i), schema.getColumnType(i), values[i]);
        }
    }
}
