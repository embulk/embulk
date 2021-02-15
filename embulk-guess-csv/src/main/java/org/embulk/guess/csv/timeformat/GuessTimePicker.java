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
 * Matches a string with prepared regular expressions for times, and captures some pieces of strings from the given string.
 *
 * <p>It is a clone from {@code embulk-util-guess}. Its Java package has just changed.
 * It is tentatively here until {@code embulk-guess-csv} starts using {@code embulk-util-*} librarires.
 */
final class GuessTimePicker {
    private GuessTimePicker(
            final String dateTimeDelim,
            final String timeDelim,
            final String fracDelim,
            final String hour,
            final String minute,
            final String second,
            final String frac,
            final String rest) {
        this.dateTimeDelim = dateTimeDelim;
        this.timeDelim = timeDelim;
        this.fracDelim = fracDelim;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.frac = frac;
        this.rest = rest;
    }

    static GuessTimePicker from(final String text, final String dateDelim) {
        for (final TimeMatcher matcher : TIME_MATCHERS) {
            final GuessTimePicker matched = matcher.match(text, dateDelim);
            if (matched != null) {
                return matched;
            }
        }
        return null;
    }

    String getDateTimeDelim() {
        return this.dateTimeDelim;
    }

    String getTimeDelim() {
        return this.timeDelim;
    }

    String getFracDelim() {
        return this.fracDelim;
    }

    String getHour() {
        return this.hour;
    }

    String getMinute() {
        return this.minute;
    }

    String getSecond() {
        return this.second;
    }

    String getFrac() {
        return this.frac;
    }

    String getRest() {
        return this.rest;
    }

    private static class TimeMatcher {
        TimeMatcher(
                final Pattern pattern,
                final boolean requireDateDelimEmpty,
                final boolean hasTimeDelim,
                final boolean hasDateTimeDelim) {
            this.pattern = pattern;
            this.requireDateDelimEmpty = requireDateDelimEmpty;
            this.hasTimeDelim = hasTimeDelim;
            this.hasDateTimeDelim = hasDateTimeDelim;
        }

        GuessTimePicker match(final String text, final String dateDelim) {
            if (this.requireDateDelimEmpty && !dateDelim.isEmpty()) {
                return null;
            }

            final Matcher matcher = this.pattern.matcher(text);
            if (matcher.matches()) {
                final String dateTimeDelim = (this.hasDateTimeDelim ? matcher.group("dateTimeDelim") : "");
                final String timeDelim = (this.hasTimeDelim ? matcher.group("timeDelim") : "");
                final String fracDelim = matcher.group("fracDelim");
                final String hour = matcher.group("hour");
                final String minute = matcher.group("minute");
                final String second = matcher.group("second");
                final String frac = matcher.group("frac");
                final String rest = matcher.group("rest");
                return new GuessTimePicker(
                        dateTimeDelim != null ? dateTimeDelim : "",
                        timeDelim != null ? timeDelim : "",
                        fracDelim != null ? fracDelim : "",
                        hour != null ? hour : "",
                        minute != null ? minute : "",
                        second != null ? second : "",
                        frac  != null ? frac : "",
                        rest != null ? rest : "");
            }
            return null;
        }

        @Override
        public String toString() {
            return this.pattern.toString();
        }

        private final Pattern pattern;
        private final boolean requireDateDelimEmpty;
        private final boolean hasTimeDelim;
        private final boolean hasDateTimeDelim;
    }

    /**
     * Possible delimiters between date and time.
     *
     * <pre>{@code date_time_delims = /(:? |_|T|\. ?)/}</pre>
     */
    private static final String DATE_TIME_DELIMS = "(:? |\\_|T|\\. ?)";

    /**
     * <pre>{@code time_delims = /[\:\-]/}</pre>
     */
    private static final String TIME_DELIMS = "[\\:\\-]";

    /**
     * <pre>{@code frac_delims = /[\.\,]/}</pre>
     */
    private static final String FRAC_DELIMS = "[\\.\\,]";

    /**
     * <pre>{@code frac = /[0-9]{1,9}/}</pre>
     */
    private static final String FRAC = "[0-9]{1,9}";

    @SuppressWarnings("checkstyle:LineLength")
    /**
     * <pre>{@code TIME         = /(?<hour>#{HOUR})(?:(?<time_delim>#{time_delims})(?<minute>#{MINUTE})(?:\k<time_delim>(?<second>#{SECOND})(?:(?<frac_delim>#{frac_delims})(?<frac>#{frac}))?)?)?/}</pre>
     */
    private static final String TIME = String.format(
            "(?<hour>%s)(?:(?<timeDelim>%s)(?<minute>%s)"
            + "(?:\\k<timeDelim>(?<second>%s)(?:(?<fracDelim>%s)(?<frac>%s))?)?)?",
            Parts.HOUR, TIME_DELIMS, Parts.MINUTE, Parts.SECOND, FRAC_DELIMS, FRAC);

    @SuppressWarnings("checkstyle:LineLength")
    /**
     * <pre>{@code TIME_NODELIM = /(?<hour>#{HOUR_NODELIM})(?:(?<minute>#{MINUTE_NODELIM})((?<second>#{SECOND_NODELIM})(?:(?<frac_delim>#{frac_delims})(?<frac>#{frac}))?)?)?/}</pre>
     */
    private static final String TIME_NODELIM = String.format(
            "(?<hour>%s)(?:(?<minute>%s)((?<second>%s)(?:(?<fracDelim>%s)(?<frac>%s))?)?)?",
            Parts.HOUR_NODELIM, Parts.MINUTE_NODELIM, Parts.SECOND_NODELIM, FRAC_DELIMS, FRAC);

    private static final Pattern TIME_DELIM_WITH_DATE_TIME_DELIM = Pattern.compile(String.format(
            "^(?<dateTimeDelim>%s)%s(?<rest>.*?)?$",
            DATE_TIME_DELIMS, TIME));

    private static final Pattern TIME_NODELIM_WITH_DATE_TIME_DELIM = Pattern.compile(String.format(
            "^(?<dateTimeDelim>%s)%s(?<rest>.*?)?$",
            DATE_TIME_DELIMS, TIME_NODELIM));

    private static final Pattern TIME_NODELIM_WITH_DATE_TIME_NODELIM = Pattern.compile(String.format(
            "^%s(?<rest>.*?)?$",
            TIME_NODELIM));

    private static TimeMatcher[] TIME_MATCHERS = {
        new TimeMatcher(TIME_DELIM_WITH_DATE_TIME_DELIM, false, true, true),
        new TimeMatcher(TIME_NODELIM_WITH_DATE_TIME_DELIM, false, false, true),
        new TimeMatcher(TIME_NODELIM_WITH_DATE_TIME_NODELIM, true, false, false),
    };

    private final String dateTimeDelim;
    private final String timeDelim;
    private final String fracDelim;

    private final String hour;
    private final String minute;
    private final String second;
    private final String frac;

    private final String rest;
}
