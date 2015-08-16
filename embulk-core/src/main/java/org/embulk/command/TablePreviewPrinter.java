package org.embulk.command;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.PrintStream;
import java.io.IOException;
import org.embulk.config.ModelManager;
import org.embulk.spi.Schema;

public class TablePreviewPrinter
        extends PreviewPrinter
{
    private static final int MAX_SAMPLE_SIZE = 32000;

    private List<String[]> samples;
    private int sampleSize;
    private String format = null;
    private String border = null;

    public TablePreviewPrinter(PrintStream out, ModelManager modelManager, Schema schema)
    {
        super(out, modelManager, schema);
        this.samples = new ArrayList<String[]>();
        String[] header = new String[schema.getColumnCount()];
        for (int i=0; i < header.length; i++) {
            header[i] = schema.getColumnName(i) + ":" + schema.getColumnType(i);
        }
        samples.add(header);
    }

    @Override
    protected void printRecord(String[] values) throws IOException
    {
        if (samples == null) {
            // header is already written
            out.format(format, Arrays.copyOf(values, values.length, Object[].class));
        } else {
            // estimating size of columns
            samples.add(Arrays.copyOf(values, values.length));
            for (String v : values) {
                sampleSize += v.length();
            }
            if (sampleSize > MAX_SAMPLE_SIZE) {
                // enough number of rows to estimate size of the columns.
                flushSamples();
            }
        }
    }

    private void flushSamples()
    {
        StringBuilder borderBuilder = new StringBuilder();

        StringBuilder sb = new StringBuilder();
        sb.append("| ");
        borderBuilder.append("+-");
        for (int i=0; i < schema.getColumnCount(); i++) {
            if (i != 0) {
                sb.append(" | ");
                borderBuilder.append("-+-");
            }
            int colLen = maxLengthInColumn(i);
            sb.append("%"+colLen+"s");
            for (int b=0; b < colLen; b++) {
                borderBuilder.append("-");
            }
        }
        sb.append(" |");
        borderBuilder.append("-+");
        sb.append("\n");

        this.format = sb.toString();
        this.border = borderBuilder.toString();

        out.println(border);

        for (int i=0; i < samples.size(); i++) {
            String[] values = samples.get(i);
            out.format(format, Arrays.copyOf(values, values.length, Object[].class));
            if (i == 0) {
                // i == 0 is header. write border after the header
                out.println(border);
            }
        }

        this.samples = null;
    }

    private int maxLengthInColumn(int i)
    {
        int max = 0;
        for (String[] values : samples) {
            max = Math.max(max, values[i].length());
        }
        return max;
    }

    @Override
    public void finish() throws IOException
    {
        if (samples != null) {
            flushSamples();
        }
        out.println(border);
    }
}
