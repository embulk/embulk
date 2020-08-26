package org.embulk.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.embulk.spi.Page;
import org.embulk.spi.Schema;
import org.embulk.spi.time.DateTimeZoneJacksonModule;
import org.embulk.spi.time.Instants;
import org.embulk.spi.time.TimestampJacksonModule;
import org.embulk.spi.unit.LocalFileJacksonModule;
import org.embulk.spi.unit.ToStringJacksonModule;
import org.embulk.spi.unit.ToStringMapJacksonModule;
import org.embulk.spi.util.CharsetJacksonModule;
import org.embulk.spi.util.Pages;
import org.msgpack.value.Value;

public abstract class PreviewPrinter implements Closeable {
    protected final PrintStream out;
    protected final Schema schema;

    private final ObjectMapper objectMapper;
    private final String[] stringValues;
    private final NumberFormat numberFormat;

    @SuppressWarnings("deprecation")
    public PreviewPrinter(final PrintStream out, final Schema schema) {
        this.out = out;
        this.schema = schema;
        this.stringValues = new String[schema.getColumnCount()];
        this.numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);

        this.objectMapper = new ObjectMapper();

        // Those Jackson modules are registered for a while, but unnecessary ones will be swiped out.
        this.objectMapper.registerModule(new DateTimeZoneJacksonModule());  // Deprecated -- to be removed.
        this.objectMapper.registerModule(new TimestampJacksonModule());  // Deprecated. TBD to remove or not.
        this.objectMapper.registerModule(new CharsetJacksonModule());
        this.objectMapper.registerModule(new LocalFileJacksonModule());
        this.objectMapper.registerModule(new ToStringJacksonModule());
        this.objectMapper.registerModule(new ToStringMapJacksonModule());
        // PreviewPrinter would not need TypeJacksonModule, ColumnJacksonModule, and SchemaJacksonModule.
        this.objectMapper.registerModule(new GuavaModule());
        this.objectMapper.registerModule(new Jdk8Module());
        this.objectMapper.registerModule(new JodaModule());
    }

    public final void printAllPages(List<Page> pages) throws IOException {
        List<Object[]> records = Pages.toObjects(schema, pages, true);
        for (Object[] record : records) {
            printRecord(record);
        }
    }

    @Override
    public final void close() throws IOException {
        out.close();
    }

    public void finish() throws IOException {}

    protected abstract void printRecord(String[] values) throws IOException;

    private void printRecord(Object... values) throws IOException {
        int min = Math.min(schema.getColumnCount(), values.length);
        for (int i = 0; i < min; i++) {
            stringValues[i] = valueToString(values[i]);
        }
        for (int i = min; i < schema.getColumnCount(); i++) {
            stringValues[i] = valueToString(null);
        }
        printRecord(stringValues);
    }

    private String valueToString(Object obj) {
        if (obj == null) {
            return "";
        } else if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof Number) {
            if (obj instanceof Integer) {
                return numberFormat.format(((Integer) obj).longValue());
            }
            if (obj instanceof Long) {
                return numberFormat.format(((Long) obj).longValue());
            }
            return obj.toString();
        } else if (obj instanceof Instant) {
            return Instants.toString((Instant) obj);
        } else if (obj instanceof Value) {
            return obj.toString();
        } else {
            try {
                return this.objectMapper.writeValueAsString(obj);
            } catch (final JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
