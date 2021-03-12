/*
 * Copyright 2020 The Embulk project
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

package org.embulk.guess.csv;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Guesses a schema from sample objects.
 *
 * <p>It reimplements {@code SchemaGuess} in {@code /embulk/guess/schema_guess.rb}.
 *
 * <p>It is cloned from {@code embulk-util-guess}, and modified to call {@code typesFromListRecords} directly from {@code CsvGuessPlugin}.
 *
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/schema_guess.rb">schema_guess.rb</a>
 */
final class SchemaGuess {
    private SchemaGuess(final TimeFormatGuess timeFormatGuess) {
        this.timeFormatGuess = timeFormatGuess;
    }

    static SchemaGuess of() {
        return new SchemaGuess(TimeFormatGuess.of());
    }

    static class GuessedType implements Comparable<GuessedType> {
        private GuessedType(final String string, final String formatOrTimeValue) {
            this.string = string;
            this.formatOrTimeValue = formatOrTimeValue;
        }

        private GuessedType(final String string) {
            this(string, null);
        }

        static GuessedType timestamp(final String formatOrTimeValue) {
            return new GuessedType("timestamp", formatOrTimeValue);
        }

        boolean isTimestamp() {
            return this.string.equals("timestamp");
        }

        String getFormatOrTimeValue() {
            return this.formatOrTimeValue;
        }

        /**
         * Returns {@code true} if just its type is the same with another object's.
         *
         * <p>Note that it does not take care of {@code formatOrTimeValue}. It returns {@code true} if
         * both are {@code "timestamp"}, even if their {@code formatOrTimeValue}s are different.
         *
         * <p>It is expected to be called only from {@code mergeType} which should merge {@code "timestamp"}
         * and {@code "timestamp"} into {@code "timestamp"}, even if their {@code formatOrTimeValue}s are
         * different. Those {@code formatOrTimeValue}s are considered in {@code mergeTypes} later.
         */
        boolean typeEquals(final Object otherObject) {
            if (!(otherObject instanceof GuessedType)) {
                return false;
            }
            final GuessedType other = (GuessedType) otherObject;
            return Objects.equals(this.string, other.string);
        }

        /**
         * Returns {@code true} if its type and {@code formatOrTimeValue} are the same with another object's.
         *
         * <p>Note that it takes care of {@code formatOrTimeValue}. This equality is used out of {@code SchemaGuess},
         * in {@code CSVGuessPlugin} to compare lists of {@code GuessedType}s.
         */
        @Override
        public boolean equals(final Object otherObject) {
            if (!(otherObject instanceof GuessedType)) {
                return false;
            }
            final GuessedType other = (GuessedType) otherObject;
            return Objects.equals(this.string, other.string) && Objects.equals(this.formatOrTimeValue, other.formatOrTimeValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.string, this.formatOrTimeValue);
        }

        @Override
        public int compareTo(final GuessedType other) {
            if (this.formatOrTimeValue != null && other.formatOrTimeValue != null) {
                return this.formatOrTimeValue.compareTo(other.formatOrTimeValue);
            }
            return this.string.compareTo(other.string);
        }

        @Override
        public String toString() {
            return this.string;
        }

        static final GuessedType BOOLEAN = new GuessedType("boolean");
        static final GuessedType DOUBLE = new GuessedType("double");
        static final GuessedType JSON = new GuessedType("json");
        static final GuessedType LONG = new GuessedType("long");
        static final GuessedType STRING = new GuessedType("string");

        private final String string;
        private final String formatOrTimeValue;
    }

    List<GuessedType> typesFromListRecords(final List<List<String>> samples) {
        final int maxRecords = samples.stream().mapToInt(List::size).max().orElse(0);
        if (maxRecords <= 0) {
            return Collections.emptyList();
        }
        final ArrayList<ArrayList<GuessedType>> columnarTypes = new ArrayList<>(maxRecords);
        for (int i = 0; i < maxRecords; ++i) {
            columnarTypes.add(new ArrayList<>());
        }

        for (final List<String> record : samples) {
            for (int i = 0; i < record.size(); ++i) {
                final Object value = record.get(i);
                columnarTypes.get(i).add(this.guessType(value));
            }
        }

        return columnarTypes.stream().map(types -> this.mergeTypes(types)).collect(Collectors.toList());
    }

    private GuessedType guessType(final Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map || value instanceof List) {
            return GuessedType.JSON;
        }
        final String str = value.toString();

        if (TRUE_STRINGS.contains(str) || FALSE_STRINGS.contains(str)) {
            return GuessedType.BOOLEAN;
        }

        if (this.timeFormatGuess.guess(Arrays.asList(str)) != null) {
            return GuessedType.timestamp(str);
        }

        try {
            if (Integer.valueOf(str).toString().equals(str)) {
                return GuessedType.LONG;
            }
        } catch (final RuntimeException ex) {
            // Pass-through.
        }

        // Introduce a regular expression to make better suggestion to double type. It refers to Guava 21.0's regular
        // expression in Doubles#fpPattern() but, there're difference as following:
        // * It intentionaly rejects float values when they start with "0" like "001.0", "010.01". "0.1" is ok.
        // * It doesn't support hexadecimal representation. It could be improved more later.
        if (DOUBLE_PATTERN.matcher(str).matches()) {
            return GuessedType.DOUBLE;
        }

        if (str.isEmpty()) {
            return null;
        }

        try {
            new ObjectMapper().readTree(str);
            return GuessedType.JSON;
        } catch (final Exception ex) {
            // Pass-through.
        }

        return GuessedType.STRING;
    }

    private GuessedType mergeTypes(final List<GuessedType> types) {
        final GuessedType t = Optional.ofNullable(types.stream().reduce(null, (final GuessedType merged, final GuessedType type) -> {
            return mergeType(merged, type);
        })).orElse(GuessedType.STRING);
        if (t.isTimestamp()) {
            final String format = this.timeFormatGuess.guess(
                    types.stream()
                            .map(type -> type.isTimestamp() ? type.getFormatOrTimeValue() : null)
                            .filter(type -> type != null)
                            .collect(Collectors.toList()));
            return GuessedType.timestamp(format);
        }
        return t;
    }

    private static GuessedType mergeType(final GuessedType type1, final GuessedType type2) {
        if (type1 == null) {
            return type2;
        } else if (type2 == null) {
            return type1;
        } else if (type1.typeEquals(type2)) {
            return type1;
        } else {
            return coalesceType(type1, type2);
        }
    }

    private static GuessedType coalesceType(final GuessedType type1, final GuessedType type2) {
        final GuessedType[] types = { type1, type2 };
        Arrays.sort(types);

        if (types[0] == GuessedType.DOUBLE && types[1] == GuessedType.LONG) {
            return GuessedType.DOUBLE;
        } else if (types[0] == GuessedType.BOOLEAN && types[1] == GuessedType.LONG) {
            return GuessedType.LONG;
        } else if (types[0] == GuessedType.LONG && types[1].isTimestamp()) {
            return GuessedType.LONG;
        }
        return GuessedType.STRING;
    }

    private static final Pattern DOUBLE_PATTERN = Pattern.compile(
            "^[+-]?(NaN|Infinity|([1-9]\\d*|0)(\\.\\d+)([eE][+-]?\\d+)?[fFdD]?)$");

    // taken from CsvParserPlugin.TRUE_STRINGS
    private static final String[] TRUE_STRINGS_ARRAY = {
        "true", "True", "TRUE",
        "yes", "Yes", "YES",
        "t", "T", "y", "Y",
        "on", "On", "ON",
    };

    private static final String[] FALSE_STRINGS_ARRAY = {
        "false", "False", "FALSE",
        "no", "No", "NO",
        "f", "F", "n", "N",
        "off", "Off", "OFF",
    };

    private static final Set<String> TRUE_STRINGS;
    private static final Set<String> FALSE_STRINGS;

    static {
        TRUE_STRINGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(TRUE_STRINGS_ARRAY)));
        FALSE_STRINGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(FALSE_STRINGS_ARRAY)));
    }

    private final TimeFormatGuess timeFormatGuess;
}
