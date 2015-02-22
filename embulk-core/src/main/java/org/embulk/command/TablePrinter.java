package org.embulk.command;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.io.PrintStream;
import java.io.Flushable;
import java.io.IOException;
import org.embulk.config.ModelManager;

class TablePrinter extends PreviewPrinter
{

    private String format;
    private String border;

    public TablePrinter(PrintStream out, ModelManager model, String... header)
    {
        super(out, model, header);
    }

    protected void flushSamples()
    {
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

        out.println(border);

        this.samples = null;
    }

}

