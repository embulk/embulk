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
        int maxHeaderLen = maxHeaderLength() + 1;
        for( int i = 0 ; i < samples.size() ; i++ ) {
            out.format("******************* %d **********************\n",i+1);
            Object[] sample = samples.get(i);
            for( int j = 0 ; j < header.length ; j++ ) {
                out.format("%" + maxHeaderLen +"s: %s\n",header[j],valueToString(sample[j]));
            }
            out.println("");
        }
    }

    protected int maxHeaderLength()
    {
        int maxlength = 0;
        for( int i = 0 ; i < header.length ; i++ ) {
            int len = header[i].length();
            if (len > maxlength) maxlength = len;
        }
        return maxlength;
    }

}

