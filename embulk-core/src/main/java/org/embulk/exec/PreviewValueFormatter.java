package org.embulk.exec;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;
import org.embulk.spi.json.JsonValue;
import org.embulk.spi.time.Instants;

final class PreviewValueFormatter {
    PreviewValueFormatter() {
        this.numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
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
        } else if (obj instanceof JsonValue) {
            return obj.toString();
        } else if (obj instanceof Boolean) {
            return obj.toString();
        } else {
            throw new IllegalStateException("Record has a value with an unexpected type: " + obj.getClass().getName());
        }
    }

    private final NumberFormat numberFormat;
}
