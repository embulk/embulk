package org.embulk.cli;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.io.PrintStream;
import java.io.Flushable;
import java.io.IOException;
import java.text.NumberFormat;

public class TablePrinter {
    private static final int SAMPLES = 10;

    private PrintStream out;

    private String[] header;

    private List<String[]> samples;
    private String format;
    private String border;

    private NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);

    public TablePrinter(PrintStream out, String... header) {
        this.out = out;
        this.header = header;
        this.samples = new ArrayList<String[]>(SAMPLES);
        samples.add(header);
    }

    public void add(Object... values) throws IOException {
        int min = header.length < values.length ? header.length : values.length;
        String[] cols = new String[header.length];
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

        if(samples == null) {
            out.format(format, cols);
            return;
        }

        samples.add(cols);
        if(samples.size() < SAMPLES) {
            return;
        }
    }

    protected String valueToString(Object obj) {
        if(obj instanceof Number) {
            if(obj instanceof Integer) {
                return numberFormat.format(((Integer) obj).longValue());
            }
            if(obj instanceof Long) {
                return numberFormat.format(((Long) obj).longValue());
            }
        }
        return obj.toString();
    }

    private void flushSamples() {
        StringBuilder borderBuilder = new StringBuilder();

        StringBuilder sb = new StringBuilder();
        sb.append("| ");
        borderBuilder.append("+-");
        for(int i=0; i < header.length; i++) {
            if(i != 0) {
                sb.append(" | ");
                borderBuilder.append("-+-");
            }
            int colLen = maxLengthInColumn(i);
            sb.append("%"+colLen+"s");
            for(int b=0; b < colLen; b++) {
                borderBuilder.append("-");
            }
        }
        sb.append(" |");
        borderBuilder.append("-+");
        sb.append("\n");

        this.format = sb.toString();
        this.border = borderBuilder.toString();

        out.println(border);

        for(int i=0; i < samples.size(); i++) {
            out.format(format, samples.get(i));
            if(i == 0) {
                out.println(border);
            }
        }

        this.samples = null;
    }

    private int maxLengthInColumn(int i) {
        int max = 0;
        for(String[] cols : samples) {
            String s = cols[i];
            int len = (s == null) ? 0 : s.length();
            if(max < len) {
                max = len;
            }
        }
        return max;
    }

    public void flush() throws IOException {
        if(!samples.isEmpty()) {
            flushSamples();
        }
        out.println(border);
    }
}

