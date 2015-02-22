package org.embulk.command;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.io.PrintStream;
import java.io.Flushable;
import java.io.IOException;
import java.text.NumberFormat;
import org.embulk.spi.time.Timestamp;
import org.embulk.config.ModelManager;

abstract class PreviewPrinter
{
    protected static final int SAMPLES = 10;

    protected PrintStream out;

    protected String[] header;

    protected List<Object[]> samples;

    protected NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
    protected ModelManager model;

    public PreviewPrinter(PrintStream out, ModelManager model, String... header)
    {
        this.out = out;
        this.header = header;
        this.model = model;
        this.samples = new ArrayList<Object[]>(SAMPLES);
        samples.add(header);
    }

    public void add(Object... values) throws IOException
    {
        int min = header.length < values.length ? header.length : values.length;
        Object[] cols = new Object[header.length];
        for(int i=0; i < min; i++) {
            if(values[i] == null) {
                cols[i] = "";
            } else {
                cols[i] = valueToString(values[i]);
            }
        }
        for(int i=min; i < header.length; i++) {
            cols[i] = "";
        }

        samples.add(cols);
        if(samples.size() < SAMPLES) {
            return;
        }
    }

    protected String valueToString(Object obj)
    {
        if (obj instanceof String) {
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
        } else {
            return model.writeObject(obj);
        }
    }

    abstract protected void flushSamples();

    protected int maxLengthInColumn(int i)
    {
        int max = 0;
        for(Object[] cols : samples) {
            String s = (String) cols[i];
            int len = (s == null) ? 0 : s.length();
            if(max < len) {
                max = len;
            }
        }
        return max;
    }

    public void finish() throws IOException
    {
        if(!samples.isEmpty()) {
            flushSamples();
        }
    }
}

