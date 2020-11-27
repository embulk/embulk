package org.embulk.deps.preview;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.embulk.spi.Page;
import org.embulk.spi.Schema;

final class TablePreviewPrinter extends PreviewPrinter {
    TablePreviewPrinter(final PrintStream out, final Schema schema) {
        this.out = out;
        this.schema = schema;
        this.valueFormatter = new ValueFormatter();

        this.format = null;
        this.border = null;

        this.sampleSize = 0;

        this.samples = new ArrayList<String[]>();
        final String[] header = new String[schema.getColumnCount()];
        for (int i = 0; i < header.length; i++) {
            header[i] = schema.getColumnName(i) + ":" + schema.getColumnType(i);
        }
        this.samples.add(header);
    }

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1306
    public void printAllPages(final List<Page> pages) throws IOException {
        final List<Object[]> records = org.embulk.spi.util.Pages.toObjects(schema, pages, true);
        for (final Object[] record : records) {
            printRecord(record);
        }
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void finish() throws IOException {
        if (samples != null) {
            flushSamples();
        }
        out.println(border);
    }

    private void printRecord(final Object... values) throws IOException {
        final String[] stringValues = new String[schema.getColumnCount()];

        int min = Math.min(schema.getColumnCount(), values.length);
        for (int i = 0; i < min; i++) {
            stringValues[i] = this.valueFormatter.valueToString(values[i]);
        }
        for (int i = min; i < schema.getColumnCount(); i++) {
            stringValues[i] = this.valueFormatter.valueToString(null);
        }

        if (samples == null) {
            // header is already written
            out.format(format, Arrays.copyOf(stringValues, stringValues.length, Object[].class));
        } else {
            // estimating size of columns
            samples.add(Arrays.copyOf(stringValues, stringValues.length));
            for (String v : stringValues) {
                sampleSize += v.length();
            }
            if (sampleSize > MAX_SAMPLE_SIZE) {
                // enough number of rows to estimate size of the columns.
                flushSamples();
            }
        }
    }

    private void flushSamples() {
        StringBuilder borderBuilder = new StringBuilder();

        StringBuilder sb = new StringBuilder();
        sb.append("| ");
        borderBuilder.append("+-");
        for (int i = 0; i < schema.getColumnCount(); i++) {
            if (i != 0) {
                sb.append(" | ");
                borderBuilder.append("-+-");
            }
            int colLen = maxLengthInColumn(i);
            sb.append("%" + colLen + "s");
            for (int b = 0; b < colLen; b++) {
                borderBuilder.append("-");
            }
        }
        sb.append(" |");
        borderBuilder.append("-+");
        sb.append("\n");

        this.format = sb.toString();
        this.border = borderBuilder.toString();

        out.println(border);

        for (int i = 0; i < samples.size(); i++) {
            String[] values = samples.get(i);
            out.format(format, Arrays.copyOf(values, values.length, Object[].class));
            if (i == 0) {
                // i == 0 is header. write border after the header
                out.println(border);
            }
        }

        this.samples = null;
    }

    private int maxLengthInColumn(int i) {
        int max = 0;
        for (String[] values : samples) {
            max = Math.max(max, values[i].length());
        }
        return max;
    }

    private static final int MAX_SAMPLE_SIZE = 32000;

    private final PrintStream out;
    private final Schema schema;

    private final ValueFormatter valueFormatter;

    private List<String[]> samples;
    private int sampleSize;
    private String format;
    private String border;
}
