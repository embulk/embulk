package org.embulk.cli;

import java.util.ArrayList;
import java.util.Collections;

public class Main
{
    public static void main(final String[] args)
    {
        final ArrayList<String> jrubyOptions = new ArrayList<String>();

        int i;
        for (i = 0; i < args.length; ++i) {
            if (args[i].startsWith("-R")) {
                jrubyOptions.add(args[i].substring(2));
            } else {
                break;
            }
        }

        final String[] embulkArgs = new String[args.length - i];
        for (int j = 0; i < args.length; ++i, ++j) {
            embulkArgs[j] = args[i];
        }

        EmbulkBundle.checkBundle(embulkArgs, Collections.unmodifiableList(jrubyOptions));
    }
}
