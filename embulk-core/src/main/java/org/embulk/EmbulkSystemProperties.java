package org.embulk;

import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
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
}
