package org.embulk.command;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.io.PrintStream;
import java.io.Flushable;
import java.io.IOException;
import org.embulk.config.ModelManager;

class VerticalPrinter extends PreviewPrinter
{

    public VerticalPrinter(PrintStream out, ModelManager model, String... header)
    {
        super(out, model, header);
    }

    protected void flushSamples()
    {
        for( int i = 1 ; i < samples.size() ; i++ ) {
            out.format("******************* %d **********************\n",i);
            Object[] sample = samples.get(i);
            for( int j = 0 ; j < header.length ; j++ ) {
                out.format("%s: %s\n",valueToString(header[j]),valueToString(sample[j]));
            }
            out.println("");
        }
    }

}

