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

package org.embulk.guess.csv.timeformat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches a string with prepared regular expressions for dates, and captures some pieces of strings from the given string.
 *
 * <p>It is a clone from {@code embulk-util-guess}. Its Java package has just changed.
 * It is tentatively here until {@code embulk-guess-csv} starts using {@code embulk-util-*} librarires.
 */
final class GuessDatePicker {
    private GuessDatePicker(
            final GuessDateOrder order,
            final String year,
            final String month,
            final String day,
            final String rest,
            final String dateDelim) {
        this.order = order;
        this.year = year;
        this.month = month;
        this.day = day;
        this.rest = rest;
        this.dateDelim = dateDelim;
    }

    static GuessDatePicker from(final String text) {
        for (final DateMatcher matcher : DATE_MATCHERS) {
            final GuessDatePicker matched = matcher.match(text);
            if (matched != null) {
                return matched;
            }
        }
        return null;
    }

    GuessDateOrder getOrder() {
        return this.order;
    }

    String getDateDelim() {
        return this.dateDelim;
    }

    String getYear() {
        return this.year;
    }

    String getMonth() {
        return this.month;
    }

    String getDay() {
        return this.day;
    }

    String getRest() {
        return this.rest;
    }

    private static class DateMatcher {
        DateMatcher(final Pattern pattern, final boolean useDateDelim, final GuessDateOrder order) {
            this.pattern = pattern;
            this.useDateDelim = useDateDelim;
            this.order = order;
        }

        GuessDatePicker match(final String text) {
            final Matcher matcher = this.pattern.matcher(text);
            if (matcher.matches()) {
                return new GuessDatePicker(
                        this.order,
                        matcher.group("year"),
                        matcher.group("month"),
                        matcher.group("day"),
                        matcher.group("rest"),
                        (this.useDateDelim ? matcher.group("dateDelim") : ""));
            }
            return null;
        }

        private final Pattern pattern;
        private final boolean useDateDelim;
        private final GuessDateOrder order;
    }

    /**
     * Possible delimiters in a date: "/", "-", and ".".
     *
     * <pre>{@code date_delims = /[\/\-\.]/}</pre>
     */
    private static final String DATE_DELIMS = "[\\/\\-\\.]";

    // yyyy-MM-dd
    private static final Pattern YMD_DELIM = Pattern.compile(String.format(
            "^(?<year>%s)(?<dateDelim>%s)(?<month>%s)\\k<dateDelim>(?<day>%s)(?<rest>.*?)$",
            Parts.YEAR, DATE_DELIMS, Parts.MONTH, Parts.DAY));
    private static final Pattern YMD_NODELIM = Pattern.compile(String.format(
            "^(?<year>%s)(?<month>%s)(?<day>%s)(?<rest>.*?)$",
            Parts.YEAR, Parts.MONTH_NODELIM, Parts.DAY_NODELIM));

    // MM/dd/yyyy
    private static final Pattern MDY_DELIM = Pattern.compile(String.format(
            "^(?<month>%s)(?<dateDelim>%s)(?<day>%s)\\k<dateDelim>(?<year>%s)(?<rest>.*?)$",
            Parts.MONTH, DATE_DELIMS, Parts.DAY, Parts.YEAR));
    private static final Pattern MDY_NODELIM = Pattern.compile(String.format(
            "^(?<month>%s)(?<day>%s)(?<year>%s)(?<rest>.*?)$",
            Parts.MONTH_NODELIM, Parts.DAY_NODELIM, Parts.YEAR));

    // dd.MM.yyyy
    private static final Pattern DMY_DELIM = Pattern.compile(String.format(
            "^(?<day>%s)(?<dateDelim>%s)(?<month>%s)\\k<dateDelim>(?<year>%s)(?<rest>.*?)$",
            Parts.DAY, DATE_DELIMS, Parts.MONTH, Parts.YEAR));
    private static final Pattern DMY_NODELIM = Pattern.compile(String.format(
            "^(?<day>%s)(?<month>%s)(?<year>%s)(?<rest>.*?)$",
            Parts.DAY_NODELIM, Parts.MONTH_NODELIM, Parts.YEAR));

    private static DateMatcher[] DATE_MATCHERS = {
        new DateMatcher(YMD_DELIM, true, GuessDateOrder.YMD),
        new DateMatcher(YMD_NODELIM, false, GuessDateOrder.YMD),
        new DateMatcher(MDY_DELIM, true, GuessDateOrder.MDY),
        new DateMatcher(MDY_NODELIM, false, GuessDateOrder.MDY),
        new DateMatcher(DMY_DELIM, true, GuessDateOrder.DMY),
        new DateMatcher(DMY_NODELIM, false, GuessDateOrder.DMY)
    };

    private final GuessDateOrder order;

    private final String dateDelim;

    private final String year;
    private final String month;
    private final String day;

    private final String rest;
}
