package org.embulk.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TestUtils {
    private TestUtils() {
        // No instantiation.
    }

    public static List<?> copyListOf(final Object[] original) {
        return Collections.unmodifiableList(Arrays.asList(original));
    }

    public static List<?> listOf() {
        return Collections.emptyList();
    }

    public static List<?> listOf(
            final Object e1) {
        final ArrayList<Object> inner = new ArrayList<>();
        inner.add(e1);
        return Collections.unmodifiableList(inner);
    }

    public static List<?> listOf(
            final Object e1,
            final Object e2) {
        final ArrayList<Object> inner = new ArrayList<>();
        inner.add(e1);
        inner.add(e2);
        return Collections.unmodifiableList(inner);
    }

    public static List<?> listOf(
            final Object e1,
            final Object e2,
            final Object e3) {
        final ArrayList<Object> inner = new ArrayList<>();
        inner.add(e1);
        inner.add(e2);
        inner.add(e3);
        return Collections.unmodifiableList(inner);
    }

    public static List<?> listOf(
            final Object e1,
            final Object e2,
            final Object e3,
            final Object e4) {
        final ArrayList<Object> inner = new ArrayList<>();
        inner.add(e1);
        inner.add(e2);
        inner.add(e3);
        inner.add(e4);
        return Collections.unmodifiableList(inner);
    }

    public static List<?> listOf(
            final Object e1,
            final Object e2,
            final Object e3,
            final Object e4,
            final Object e5) {
        final ArrayList<Object> inner = new ArrayList<>();
        inner.add(e1);
        inner.add(e2);
        inner.add(e3);
        inner.add(e4);
        inner.add(e5);
        return Collections.unmodifiableList(inner);
    }

    public static List<?> listOf(
            final Object e1,
            final Object e2,
            final Object e3,
            final Object e4,
            final Object e5,
            final Object e6) {
        final ArrayList<Object> inner = new ArrayList<>();
        inner.add(e1);
        inner.add(e2);
        inner.add(e3);
        inner.add(e4);
        inner.add(e5);
        inner.add(e6);
        return Collections.unmodifiableList(inner);
    }

    public static List<?> listOf(
            final Object e1,
            final Object e2,
            final Object e3,
            final Object e4,
            final Object e5,
            final Object e6,
            final Object e7) {
        final ArrayList<Object> inner = new ArrayList<>();
        inner.add(e1);
        inner.add(e2);
        inner.add(e3);
        inner.add(e4);
        inner.add(e5);
        inner.add(e6);
        inner.add(e7);
        return Collections.unmodifiableList(inner);
    }

    public static List<?> listOf(
            final Object e1,
            final Object e2,
            final Object e3,
            final Object e4,
            final Object e5,
            final Object e6,
            final Object e7,
            final Object e8) {
        final ArrayList<Object> inner = new ArrayList<>();
        inner.add(e1);
        inner.add(e2);
        inner.add(e3);
        inner.add(e4);
        inner.add(e5);
        inner.add(e6);
        inner.add(e7);
        inner.add(e8);
        return Collections.unmodifiableList(inner);
    }

    public static List<?> listOf(
            final Object e1,
            final Object e2,
            final Object e3,
            final Object e4,
            final Object e5,
            final Object e6,
            final Object e7,
            final Object e8,
            final Object e9) {
        final ArrayList<Object> inner = new ArrayList<>();
        inner.add(e1);
        inner.add(e2);
        inner.add(e3);
        inner.add(e4);
        inner.add(e5);
        inner.add(e6);
        inner.add(e7);
        inner.add(e8);
        inner.add(e9);
        return Collections.unmodifiableList(inner);
    }

    public static List<?> listOf(
            final Object e1,
            final Object e2,
            final Object e3,
            final Object e4,
            final Object e5,
            final Object e6,
            final Object e7,
            final Object e8,
            final Object e9,
            final Object e10) {
        final ArrayList<Object> inner = new ArrayList<>();
        inner.add(e1);
        inner.add(e2);
        inner.add(e3);
        inner.add(e4);
        inner.add(e5);
        inner.add(e6);
        inner.add(e7);
        inner.add(e8);
        inner.add(e9);
        inner.add(e10);
        return Collections.unmodifiableList(inner);
    }

    public static Map<String, Object> mapOf() {
        return Collections.emptyMap();
    }

    public static Map<String, Object> mapOf(
            final String k1, final Object v1) {
        final LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        putMapSafe(inner, k1, v1);
        return Collections.unmodifiableMap(inner);
    }

    public static Map<String, Object> mapOf(
            final String k1, final Object v1,
            final String k2, final Object v2) {
        final LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        putMapSafe(inner, k1, v1);
        putMapSafe(inner, k2, v2);
        return Collections.unmodifiableMap(inner);
    }

    public static Map<String, Object> mapOf(
            final String k1, final Object v1,
            final String k2, final Object v2,
            final String k3, final Object v3) {
        final LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        putMapSafe(inner, k1, v1);
        putMapSafe(inner, k2, v2);
        putMapSafe(inner, k3, v3);
        return Collections.unmodifiableMap(inner);
    }

    public static Map<String, Object> mapOf(
            final String k1, final Object v1,
            final String k2, final Object v2,
            final String k3, final Object v3,
            final String k4, final Object v4) {
        final LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        putMapSafe(inner, k1, v1);
        putMapSafe(inner, k2, v2);
        putMapSafe(inner, k3, v3);
        putMapSafe(inner, k4, v4);
        return Collections.unmodifiableMap(inner);
    }

    public static Map<String, Object> mapOf(
            final String k1, final Object v1,
            final String k2, final Object v2,
            final String k3, final Object v3,
            final String k4, final Object v4,
            final String k5, final Object v5) {
        final LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        putMapSafe(inner, k1, v1);
        putMapSafe(inner, k2, v2);
        putMapSafe(inner, k3, v3);
        putMapSafe(inner, k4, v4);
        putMapSafe(inner, k5, v5);
        return Collections.unmodifiableMap(inner);
    }

    public static Map<String, Object> mapOf(
            final String k1, final Object v1,
            final String k2, final Object v2,
            final String k3, final Object v3,
            final String k4, final Object v4,
            final String k5, final Object v5,
            final String k6, final Object v6) {
        final LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        putMapSafe(inner, k1, v1);
        putMapSafe(inner, k2, v2);
        putMapSafe(inner, k3, v3);
        putMapSafe(inner, k4, v4);
        putMapSafe(inner, k5, v5);
        putMapSafe(inner, k6, v6);
        return Collections.unmodifiableMap(inner);
    }

    public static Map<String, Object> mapOf(
            final String k1, final Object v1,
            final String k2, final Object v2,
            final String k3, final Object v3,
            final String k4, final Object v4,
            final String k5, final Object v5,
            final String k6, final Object v6,
            final String k7, final Object v7) {
        final LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        putMapSafe(inner, k1, v1);
        putMapSafe(inner, k2, v2);
        putMapSafe(inner, k3, v3);
        putMapSafe(inner, k4, v4);
        putMapSafe(inner, k5, v5);
        putMapSafe(inner, k6, v6);
        putMapSafe(inner, k7, v7);
        return Collections.unmodifiableMap(inner);
    }

    public static Map<String, Object> mapOf(
            final String k1, final Object v1,
            final String k2, final Object v2,
            final String k3, final Object v3,
            final String k4, final Object v4,
            final String k5, final Object v5,
            final String k6, final Object v6,
            final String k7, final Object v7,
            final String k8, final Object v8) {
        final LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        putMapSafe(inner, k1, v1);
        putMapSafe(inner, k2, v2);
        putMapSafe(inner, k3, v3);
        putMapSafe(inner, k4, v4);
        putMapSafe(inner, k5, v5);
        putMapSafe(inner, k6, v6);
        putMapSafe(inner, k7, v7);
        putMapSafe(inner, k8, v8);
        return Collections.unmodifiableMap(inner);
    }

    public static Map<String, Object> mapOf(
            final String k1, final Object v1,
            final String k2, final Object v2,
            final String k3, final Object v3,
            final String k4, final Object v4,
            final String k5, final Object v5,
            final String k6, final Object v6,
            final String k7, final Object v7,
            final String k8, final Object v8,
            final String k9, final Object v9) {
        final LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        putMapSafe(inner, k1, v1);
        putMapSafe(inner, k2, v2);
        putMapSafe(inner, k3, v3);
        putMapSafe(inner, k4, v4);
        putMapSafe(inner, k5, v5);
        putMapSafe(inner, k6, v6);
        putMapSafe(inner, k7, v7);
        putMapSafe(inner, k8, v8);
        putMapSafe(inner, k9, v9);
        return Collections.unmodifiableMap(inner);
    }

    public static Map<String, Object> mapOf(
            final String k1, final Object v1,
            final String k2, final Object v2,
            final String k3, final Object v3,
            final String k4, final Object v4,
            final String k5, final Object v5,
            final String k6, final Object v6,
            final String k7, final Object v7,
            final String k8, final Object v8,
            final String k9, final Object v9,
            final String k10, final Object v10) {
        final LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        putMapSafe(inner, k1, v1);
        putMapSafe(inner, k2, v2);
        putMapSafe(inner, k3, v3);
        putMapSafe(inner, k4, v4);
        putMapSafe(inner, k5, v5);
        putMapSafe(inner, k6, v6);
        putMapSafe(inner, k7, v7);
        putMapSafe(inner, k8, v8);
        putMapSafe(inner, k9, v9);
        putMapSafe(inner, k10, v10);
        return Collections.unmodifiableMap(inner);
    }

    private static void putMapSafe(final LinkedHashMap<String, Object> map, final String key, final Object value) {
        Objects.requireNonNull(key, "A key is null.");
        if (map.containsKey(key)) {
            throw new IllegalArgumentException("A key is duplicated: " + key);
        }
        map.put(key, value);
    }
}
