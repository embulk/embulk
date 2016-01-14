package org.embulk.command;

import java.util.List;
import java.util.Locale;
import java.io.PrintStream;
import java.io.Closeable;
import java.io.IOException;
import java.text.NumberFormat;
import org.embulk.config.ModelManager;
import org.embulk.spi.Schema;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.Page;
import org.embulk.spi.util.Pages;
import org.msgpack.value.Value;

public abstract class PreviewPrinter
        implements Closeable
{
    protected final PrintStream out;
    protected final ModelManager modelManager;
    protected final Schema schema;
    private final String[] stringValues;

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);

    public PreviewPrinter(PrintStream out, ModelManager modelManager, Schema schema)
    {
        this.out = out;
        this.modelManager = modelManager;
        this.schema = schema;
        this.stringValues = new String[schema.getColumnCount()];
    }

    public void printAllPages(List<Page> pages) throws IOException
    {
        List<Object[]> records = Pages.toObjects(schema, pages);
        for (Object[] record : records) {
            printRecord(record);
        }
    }

    public void printRecord(Object... values) throws IOException
    {
        int min = Math.min(schema.getColumnCount(), values.length);
        for (int i=0; i < min; i++) {
            stringValues[i] = valueToString(values[i]);
        }
        for (int i=min; i < schema.getColumnCount(); i++) {
            stringValues[i] = valueToString(null);
        }
        printRecord(stringValues);
    }

    protected abstract void printRecord(String[] values) throws IOException;

    protected String valueToString(Object obj)
    {
        if (obj == null) {
            return "";
        } else if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof Number) {
            if (obj instanceof Integer) {
                return numberFormat.format(((Integer) obj).longValue());
            }
            if (obj instanceof Long) {
                return numberFormat.format(((Long) obj).longValue());
            }
            return obj.toString();
        } else if (obj instanceof Timestamp) {
            return obj.toString();
        } else if (obj instanceof Value) {
            return obj.toString();
        } else {
            return modelManager.writeObject(obj);
        }
    }

    public void finish() throws IOException
    { }

    @Override
    public void close() throws IOException
    {
        out.close();
    }
}
