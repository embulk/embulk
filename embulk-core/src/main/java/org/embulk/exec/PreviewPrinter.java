package org.embulk.exec;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import org.embulk.spi.Page;
import org.embulk.spi.Schema;

public abstract class PreviewPrinter implements Closeable {
    public static final PreviewPrinter ofTable(final PrintStream out, final Schema schema) {
        return TablePreviewPrinter.of(out, schema);
    }

    public static final PreviewPrinter ofVertical(final PrintStream out, final Schema schema) {
        return VerticalPreviewPrinter.of(out, schema);
    }

    public abstract void printAllPages(List<Page> pages) throws IOException;

    @Override
    public abstract void close() throws IOException;

    public abstract void finish() throws IOException;
}
