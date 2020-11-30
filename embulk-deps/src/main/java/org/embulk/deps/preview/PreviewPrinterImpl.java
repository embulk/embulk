package org.embulk.deps.preview;

import java.io.PrintStream;
import org.embulk.spi.Schema;

// It is public just to be accessed from embulk-core.
public final class PreviewPrinterImpl {
    public static final PreviewPrinter ofTable(final PrintStream out, final Schema schema) {
        return new TablePreviewPrinter(out, schema);
    }

    public static final PreviewPrinter ofVertical(final PrintStream out, final Schema schema) {
        return new VerticalPreviewPrinter(out, schema);
    }
}
