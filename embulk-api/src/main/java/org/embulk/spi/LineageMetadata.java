/*
 * Copyright 2022 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.spi;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents metadata for Data Lineage in the form of an immutable String-String map.
 *
 * @since 0.10.38
 */
public final class LineageMetadata extends AbstractMap<String, String> implements Map<String, String> {
    private LineageMetadata(final LinkedHashMap<String, String> verifiedMap) {
        this.inner = Collections.unmodifiableMap(verifiedMap);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing zero mappings.
     *
     * @return an empty {@link org.embulk.spi.LineageMetadata}
     */
    public static LineageMetadata of() {
        return EMPTY;
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing the specified single mapping.
     *
     * @param k1  the mapping's key, not null
     * @param v1  the mapping's value
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mapping
     * @throws IllegalArgumentException  if the specified key or value is invalid for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if the key is {@code null}
     */
    public static LineageMetadata of(
            final String k1, final String v1) {
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        putSafe(inner, k1, v1);
        return new LineageMetadata(inner);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing the specified two mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mappings
     * @throws IllegalArgumentException  if the keys are duplicates, or if at least one of the specified keys and values are
     *     invalid for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if any key is {@code null}
     */
    public static LineageMetadata of(
            final String k1, final String v1,
            final String k2, final String v2) {
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        putSafe(inner, k1, v1);
        putSafe(inner, k2, v2);
        return new LineageMetadata(inner);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing the specified three mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mappings
     * @throws IllegalArgumentException  if the keys are duplicates, or if at least one of the specified keys and values are
     *     invalid for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if any key is {@code null}
     */
    public static LineageMetadata of(
            final String k1, final String v1,
            final String k2, final String v2,
            final String k3, final String v3) {
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        putSafe(inner, k1, v1);
        putSafe(inner, k2, v2);
        putSafe(inner, k3, v3);
        return new LineageMetadata(inner);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing the specified four mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mappings
     * @throws IllegalArgumentException  if the keys are duplicates, or if at least one of the specified keys and values are
     *     invalid for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if any key is {@code null}
     */
    public static LineageMetadata of(
            final String k1, final String v1,
            final String k2, final String v2,
            final String k3, final String v3,
            final String k4, final String v4) {
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        putSafe(inner, k1, v1);
        putSafe(inner, k2, v2);
        putSafe(inner, k3, v3);
        putSafe(inner, k4, v4);
        return new LineageMetadata(inner);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing the specified five mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mappings
     * @throws IllegalArgumentException  if the keys are duplicates, or if at least one of the specified keys and values are
     *     invalid for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if any key is {@code null}
     */
    public static LineageMetadata of(
            final String k1, final String v1,
            final String k2, final String v2,
            final String k3, final String v3,
            final String k4, final String v4,
            final String k5, final String v5) {
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        putSafe(inner, k1, v1);
        putSafe(inner, k2, v2);
        putSafe(inner, k3, v3);
        putSafe(inner, k4, v4);
        putSafe(inner, k5, v5);
        return new LineageMetadata(inner);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing the specified six mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value
     * @param k6 the sixth mapping's key, not null
     * @param v6 the sixth mapping's value
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mappings
     * @throws IllegalArgumentException  if the keys are duplicates, or if at least one of the specified keys and values are
     *     invalid for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if any key is {@code null}
     */
    public static LineageMetadata of(
            final String k1, final String v1,
            final String k2, final String v2,
            final String k3, final String v3,
            final String k4, final String v4,
            final String k5, final String v5,
            final String k6, final String v6) {
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        putSafe(inner, k1, v1);
        putSafe(inner, k2, v2);
        putSafe(inner, k3, v3);
        putSafe(inner, k4, v4);
        putSafe(inner, k5, v5);
        putSafe(inner, k6, v6);
        return new LineageMetadata(inner);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing the specified seven mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value
     * @param k6 the sixth mapping's key, not null
     * @param v6 the sixth mapping's value
     * @param k7 the seventh mapping's key, not null
     * @param v7 the seventh mapping's value
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mappings
     * @throws IllegalArgumentException  if the keys are duplicates, or if at least one of the specified keys and values are
     *     invalid for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if any key is {@code null}
     */
    public static LineageMetadata of(
            final String k1, final String v1,
            final String k2, final String v2,
            final String k3, final String v3,
            final String k4, final String v4,
            final String k5, final String v5,
            final String k6, final String v6,
            final String k7, final String v7) {
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        putSafe(inner, k1, v1);
        putSafe(inner, k2, v2);
        putSafe(inner, k3, v3);
        putSafe(inner, k4, v4);
        putSafe(inner, k5, v5);
        putSafe(inner, k6, v6);
        putSafe(inner, k7, v7);
        return new LineageMetadata(inner);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing the specified eight mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value
     * @param k6 the sixth mapping's key, not null
     * @param v6 the sixth mapping's value
     * @param k7 the seventh mapping's key, not null
     * @param v7 the seventh mapping's value
     * @param k8 the eighth mapping's key, not null
     * @param v8 the eighth mapping's value
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mappings
     * @throws IllegalArgumentException  if the keys are duplicates, or if at least one of the specified keys and values are
     *     invalid for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if any key is {@code null}
     */
    public static LineageMetadata of(
            final String k1, final String v1,
            final String k2, final String v2,
            final String k3, final String v3,
            final String k4, final String v4,
            final String k5, final String v5,
            final String k6, final String v6,
            final String k7, final String v7,
            final String k8, final String v8) {
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        putSafe(inner, k1, v1);
        putSafe(inner, k2, v2);
        putSafe(inner, k3, v3);
        putSafe(inner, k4, v4);
        putSafe(inner, k5, v5);
        putSafe(inner, k6, v6);
        putSafe(inner, k7, v7);
        putSafe(inner, k8, v8);
        return new LineageMetadata(inner);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing the specified nine mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value
     * @param k6 the sixth mapping's key, not null
     * @param v6 the sixth mapping's value
     * @param k7 the seventh mapping's key, not null
     * @param v7 the seventh mapping's value
     * @param k8 the eighth mapping's key, not null
     * @param v8 the eighth mapping's value
     * @param k9 the ninth mapping's key, not null
     * @param v9 the ninth mapping's value
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mappings
     * @throws IllegalArgumentException  if the keys are duplicates, or if at least one of the specified keys and values are
     *     invalid for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if any key is {@code null}
     */
    public static LineageMetadata of(
            final String k1, final String v1,
            final String k2, final String v2,
            final String k3, final String v3,
            final String k4, final String v4,
            final String k5, final String v5,
            final String k6, final String v6,
            final String k7, final String v7,
            final String k8, final String v8,
            final String k9, final String v9) {
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        putSafe(inner, k1, v1);
        putSafe(inner, k2, v2);
        putSafe(inner, k3, v3);
        putSafe(inner, k4, v4);
        putSafe(inner, k5, v5);
        putSafe(inner, k6, v6);
        putSafe(inner, k7, v7);
        putSafe(inner, k8, v8);
        putSafe(inner, k9, v9);
        return new LineageMetadata(inner);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing the specified ten mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value
     * @param k6 the sixth mapping's key, not null
     * @param v6 the sixth mapping's value
     * @param k7 the seventh mapping's key, not null
     * @param v7 the seventh mapping's value
     * @param k8 the eighth mapping's key, not null
     * @param v8 the eighth mapping's value
     * @param k9 the ninth mapping's key, not null
     * @param v9 the ninth mapping's value
     * @param k10 the tenth mapping's key, not null
     * @param v10 the tenth mapping's value
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mappings
     * @throws IllegalArgumentException  if the keys are duplicates, or if at least one of the specified keys and values are
     *     invalid for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if any key is {@code null}
     */
    public static LineageMetadata of(
            final String k1, final String v1,
            final String k2, final String v2,
            final String k3, final String v3,
            final String k4, final String v4,
            final String k5, final String v5,
            final String k6, final String v6,
            final String k7, final String v7,
            final String k8, final String v8,
            final String k9, final String v9,
            final String k10, final String v10) {
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        putSafe(inner, k1, v1);
        putSafe(inner, k2, v2);
        putSafe(inner, k3, v3);
        putSafe(inner, k4, v4);
        putSafe(inner, k5, v5);
        putSafe(inner, k6, v6);
        putSafe(inner, k7, v7);
        putSafe(inner, k8, v8);
        putSafe(inner, k9, v9);
        putSafe(inner, k10, v10);
        return new LineageMetadata(inner);
    }

    /**
     * Returns a new {@link org.embulk.spi.LineageMetadata} containing keys and values extracted from the given entries.
     *
     * @param entries  {@link java.util.Map.Entry}s containing keys and values from which the map is populated, not null
     * @return a {@link org.embulk.spi.LineageMetadata} containing the specified mappings
     * @throws IllegalArgumentException  if there are any duplicate keys, or if at least one of the specified entries contain
     *     an invalid key or value for {@link org.embulk.spi.LineageMetadata}
     * @throws NullPointerException  if the array or any key is {@code null}
     */
    @SafeVarargs
    @SuppressWarnings({"varargs"})
    public static LineageMetadata ofEntries(final Map.Entry<String, String>... entries) {
        Objects.requireNonNull(entries, "The specified array is null.");
        final LinkedHashMap<String, String> inner = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entry : entries) {
            putSafe(inner, entry.getKey(), entry.getValue());
        }
        return new LineageMetadata(inner);
    }

    /**
     * Returns a {@link java.util.Set} view of the mappings contained in this {@link org.embulk.spi.LineageMetadata}.
     *
     * @return a set view of the mappings contained in this {@link org.embulk.spi.LineageMetadata}
     */
    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        return this.inner.entrySet();
    }

    private static void putSafe(final LinkedHashMap<String, String> map, final String key, final String value) {
        Objects.requireNonNull(key, "A key is null.");
        if (map.containsKey(key)) {
            throw new IllegalArgumentException("A key is duplicated: " + key);
        }
        map.put(key, value);
    }

    public static final LineageMetadata EMPTY = new LineageMetadata(new LinkedHashMap<>());

    private final Map<String, String> inner;
}
