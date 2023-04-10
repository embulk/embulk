package org.embulk;

import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Embulk's system config as {@link java.util.Properties} that do not permit any modification once created.
 *
 * <p>Plugins MUST NOT use this class. No any compatibility is guaranteed.
 */
public final class EmbulkSystemProperties extends Properties {
    private EmbulkSystemProperties(final Properties inner) {
        if (inner == null) {
            throw new NullPointerException("EmbulkSystemProperties cannot be created with null.");
        }

        synchronized (inner) {
            final Set<String> stringNames = inner.stringPropertyNames();

            int allNameCounts = 0;
            for (final Enumeration<?> allNames = inner.propertyNames(); allNames.hasMoreElements(); ) {
                allNames.nextElement();
                allNameCounts++;
            }

            if (allNameCounts != stringNames.size()) {
                throw new IllegalArgumentException("Properties contains a non-String-ish property name, or value.");
            }
            for (final String name : stringNames) {
                super.put(name, inner.getProperty(name));
            }
        }
    }

    public static EmbulkSystemProperties of(final Properties mutable) {
        return new EmbulkSystemProperties(mutable);
    }

    @SuppressWarnings("deprecation")  // For use of parseBoolean.
    public boolean getPropertyAsBoolean(final String key, final boolean defaultValue) {
        final String value = this.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return parseBoolean(value, defaultValue);
    }

    public int getPropertyAsInteger(final String key, final int defaultValue) {
        final String value = this.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return parseInteger(value);
    }

    public OptionalInt getPropertyAsOptionalInt(final String key) {
        final String value = this.getProperty(key);
        if (value == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(parseInteger(value));
    }

    @Override  // From Properties
    public void load(final InputStream inStream) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From Properties
    public void load(final Reader reader) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From Properties
    public void loadFromXML(final InputStream in) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From Properties
    public Object setProperty(final String key, final String value) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From Hashtable
    public void clear() {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From Hashtable
    public Object compute(
            final Object key, final BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From Hashtable
    public Object computeIfAbsent(
            final Object key, final Function<? super Object, ? extends Object> mappingFunction) {
        // TODO: Refuse only modifying operations.
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From Hashtable
    public Object computeIfPresent(
            final Object key, final BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        // TODO: Refuse only modifying operations.
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From Hashtable
    public Set<Map.Entry<Object, Object>> entrySet() {
        return Collections.unmodifiableSet(new HashSet<>(super.entrySet()));
    }

    @Override  // From Hashtable
    public void forEach(final BiConsumer<? super Object, ? super Object> action) {
        // TODO: Refuse only modifying operations.
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From HashTable
    public Set<Object> keySet() {
        return Collections.unmodifiableSet(new HashSet<>(super.keySet()));
    }

    @Override  // From HashTable
    public Object merge(
            final Object key, final Object value,
            final BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        // TODO: Refuse only modifying operations.
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From HashTable
    public Object put(final Object key, final Object value) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From HashTable
    public void putAll(final Map<? extends Object, ? extends Object> t) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From HashTable
    public Object putIfAbsent(final Object key, final Object value) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From HashTable
    public Object remove(final Object key) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From HashTable
    public boolean remove(final Object key, final Object value) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From HashTable
    public Object replace(final Object key, final Object value) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From HashTable
    public boolean replace(final Object key, final Object oldValue, final Object newValue) {
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From HashTable
    public void replaceAll(final BiFunction<? super Object, ? super Object, ? extends Object> function) {
        // TODO: Refuse only modifying operations.
        throw new UnsupportedOperationException("Modifying EmbulkSystemProperties is not permitted.");
    }

    @Override  // From Hashtable
    public Collection<Object> values() {
        return Collections.unmodifiableSet(new HashSet<>(super.values()));
    }

    /**
     * Parses String into boolean in almost the same way with the default ObjectMapper of Jackson 2.6.7.
     *
     * <p>Reimplemented based on jackson-databind 2.6.7, which has been licensed under Apache License 2.0.
     *
     * @see <a href="https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.6.7/src/main/java/com/fasterxml/jackson/databind/deser/std/NumberDeserializers.java#L161-L190">com.fasterxml.jackson.databind.deser.std.NumberDeserializers.BooleanDeserializer</a>
     * @see <a href="https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.6.7/src/main/java/com/fasterxml/jackson/databind/deser/std/StdDeserializer.java#L185-L213">com.fasterxml.jackson.databind.deser.std.StdDeserializer#_parseBoolean</a>
     * @see <a href="https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.6.7/src/main/resources/META-INF/LICENSE">LICENSE</a>
     *
     * @deprecated This method is not for plugins. It is {@code public} only for {@code EmbulkSystemPropertiesBuilder}.
     */
    @Deprecated
    public static boolean parseBoolean(final String text, final boolean defaultValue) {
        // This methods expects |text| is not null.
        final String textTrimmed = text.trim();

        if ("true".equals(textTrimmed) || "True".equals(textTrimmed)) {
            return true;
        }
        if ("false".equals(textTrimmed) || "False".equals(textTrimmed)) {
            return false;
        }
        if (textTrimmed.length() == 0) {
            return defaultValue;
        }
        if ("null".equals(textTrimmed)) {
            return defaultValue;
        }
        throw new IllegalArgumentException("Only \"true\" or \"false\" is recognized.");
    }

    /**
     * Parses String into int in the same way with the default ObjectMapper of Jackson 2.6.7.
     *
     * <p>Reimplemented based on jackson-databind 2.6.7, which has been licensed under Apache License 2.0.
     *
     * @see <a href="https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.6.7/src/main/java/com/fasterxml/jackson/databind/deser/std/NumberDeserializers.java#L287-L323">com.fasterxml.jackson.databind.deser.std.NumberDeserializers.IntegerDeserializer</a>
     * @see <a href="https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.6.7/src/main/java/com/fasterxml/jackson/databind/deser/std/StdDeserializer.java#L423-L444">com.fasterxml.jackson.databind.deser.std.StdDeserializer#_parseInteger</a>
     * @see <a href="https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.6.7/src/main/resources/META-INF/LICENSE">LICENSE</a>
     */
    private static int parseInteger(final String text) {
        // This methods expects |text| is not null.
        final String textTrimmed = text.trim();

        final int length = textTrimmed.length();
        if ("null".equals(textTrimmed)) {
            throw new NullPointerException("\"" + text + "\" is considered to be null.");
        }

        if (length > 9) {
            final long longValue = Long.parseLong(textTrimmed);
            if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
                throw new NumberFormatException("Overflow: \"" + text + "\" is out of range of int.");
            }
            return Integer.valueOf((int) longValue);
        }
        if (length == 0) {
            throw new NullPointerException("\"" + text + "\" is considered to be an empty string.");
        }
        return Integer.valueOf(parseIntegerInternal(textTrimmed));
    }

    /**
     * Parses String into int in the same with Jackson 2.6.7.
     *
     * <p>Reimplemented based on jackson-core 2.6.7, which has been licensed under Apache License 2.0.
     *
     * @see <a href="https://github.com/FasterXML/jackson-core/blob/jackson-core-2.6.7/src/main/java/com/fasterxml/jackson/core/io/NumberInput.java#L187-L223">com.fasterxml.jackson.core.io.NumberInput#parseAsInt</a>
     * @see <a href="https://github.com/FasterXML/jackson-core/blob/jackson-core-2.6.7/src/main/resources/META-INF/LICENSE">LICENSE</a>
     */
    private static int parseIntegerInternal(final String text) {
        final int length = text.length();
        final char first = text.charAt(0);
        final boolean negative = (first == '-');

        char cursor = first;
        int offset = 1;

        if (negative) {
            if (length == 1 || length > 10) {
                return Integer.parseInt(text);
            }
            cursor = text.charAt(offset++);
        } else {
            if (length > 9) {
                return Integer.parseInt(text);
            }
        }

        if (cursor > '9' || cursor < '0') {
            return Integer.parseInt(text);
        }

        int number = cursor - '0';
        if (offset < length) {
            cursor = text.charAt(offset++);
            if (cursor > '9' || cursor < '0') {
                return Integer.parseInt(text);
            }
            number = (number * 10) + (cursor - '0');
            if (offset < length) {
                cursor = text.charAt(offset++);
                if (cursor > '9' || cursor < '0') {
                    return Integer.parseInt(text);
                }
                number = (number * 10) + (cursor - '0');
                if (offset < length) {
                    do {
                        cursor = text.charAt(offset++);
                        if (cursor > '9' || cursor < '0') {
                            return Integer.parseInt(text);
                        }
                        number = (number * 10) + (cursor - '0');
                    } while (offset < length);
                }
            }
        }
        return negative ? -number : number;
    }
}
