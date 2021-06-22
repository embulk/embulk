package org.embulk.deps.preview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;
import org.embulk.config.CharsetJacksonModule;
import org.embulk.config.LocalFileJacksonModule;
import org.embulk.config.TimestampJacksonModule;
import org.embulk.config.ToStringJacksonModule;
import org.embulk.config.ToStringMapJacksonModule;
import org.embulk.spi.time.Instants;
import org.msgpack.value.Value;

final class ValueFormatter {
    ValueFormatter() {
        this.numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);

        this.objectMapper = new ObjectMapper();

        // Those Jackson modules are registered for a while, but unnecessary ones will be swiped out.
        this.objectMapper.registerModule(new TimestampJacksonModule());  // Deprecated. TBD to remove or not.
        this.objectMapper.registerModule(new CharsetJacksonModule());
        this.objectMapper.registerModule(new LocalFileJacksonModule());
        this.objectMapper.registerModule(new ToStringJacksonModule());
        this.objectMapper.registerModule(new ToStringMapJacksonModule());
        // PreviewPrinter would not need TypeJacksonModule, ColumnJacksonModule, and SchemaJacksonModule.
        this.objectMapper.registerModule(new GuavaModule());
        this.objectMapper.registerModule(new Jdk8Module());
    }

    String valueToString(final Object obj) {
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
                return objectMapper.writeValueAsString(obj);
            } catch (final JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private final NumberFormat numberFormat;
    private final ObjectMapper objectMapper;
}
