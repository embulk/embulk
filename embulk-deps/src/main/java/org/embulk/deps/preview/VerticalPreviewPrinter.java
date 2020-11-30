package org.embulk.deps.preview;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import org.embulk.spi.Page;
import org.embulk.spi.Schema;

final class VerticalPreviewPrinter extends PreviewPrinter {
    VerticalPreviewPrinter(final PrintStream out, final Schema schema) {
        this.out = out;
        this.schema = schema;

        this.valueFormatter = new ValueFormatter();

        this.format = "%" + maxColumnNameLength(schema) + "s (%" + maxColumnTypeNameLength(schema) + "s) : %s%n";
    }

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1306
    public final void printAllPages(final List<Page> pages) throws IOException {
        final List<Object[]> records = org.embulk.spi.util.Pages.toObjects(schema, pages, true);

        int count = 0;
        for (final Object[] record : records) {
            printRecord(count, record);
            count++;
        }
    }

    @Override
    public final void close() throws IOException {
        out.close();
    }

    @Override
    public void finish() {
    }

    private void printRecord(final int count, final Object... values) throws IOException {
        out.format("*************************** %d ***************************%n", count);
        for (int i = 0; i < schema.getColumnCount(); i++) {
            out.format(format, schema.getColumnName(i), schema.getColumnType(i), values[i]);
        }
    }

    private static int maxColumnNameLength(Schema schema) {
        int max = 0;
        for (int i = 0; i < schema.getColumnCount(); i++) {
            max = Math.max(max, schema.getColumnName(i).length());
        }
        return max;
    }

    private static int maxColumnTypeNameLength(Schema schema) {
        int max = 0;
        for (int i = 0; i < schema.getColumnCount(); i++) {
            max = Math.max(max, schema.getColumnType(i).toString().length());
        }
        return max;
    }

    private final PrintStream out;
    private final Schema schema;

    private final ValueFormatter valueFormatter;

    private final String format;
}
