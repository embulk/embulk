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

import static org.embulk.guess.csv.timeformat.Parts.MONTH_NAME_SHORT;
import static org.embulk.guess.csv.timeformat.Parts.WEEKDAY_NAME_SHORT;
import static org.embulk.guess.csv.timeformat.Parts.ZONE_OFF;

import java.util.regex.Pattern;

/**
 * Declares expected patterns in {@code TimeFormatGuess}.
 *
 * <p>It is a clone from {@code embulk-util-guess}. Its Java package has just changed.
 * It is tentatively here until {@code embulk-guess-csv} starts using {@code embulk-util-*} librarires.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/time_format_guess.rb#L384-L389">time_format_guess.rb</a>
 */
public final class ExpectedPatterns {
    private ExpectedPatterns() {
        // No instantiation.
    }

    /**
     * <pre>{@code APACHE_CLF = /^\d\d\/#{MONTH_NAME_SHORT}\/\d\d\d\d:\d\d:\d\d:\d\d #{ZONE_OFF}?$/}</pre>
     */
    private static final Pattern APACHE_CLF = Pattern.compile(String.format(
            "^\\d\\d\\/%s\\/\\d\\d\\d\\d:\\d\\d:\\d\\d:\\d\\d %s?$", MONTH_NAME_SHORT, ZONE_OFF));

    /**
     * <pre>{@code ANSI_C_ASCTIME = /^#{WEEKDAY_NAME_SHORT} #{MONTH_NAME_SHORT} \d\d? \d\d:\d\d:\d\d \d\d\d\d$/}</pre>
     */
    private static final Pattern ANSI_C_ASCTIME = Pattern.compile(String.format(
            "^%s %s \\d\\d? \\d\\d:\\d\\d:\\d\\d \\d\\d\\d\\d$", WEEKDAY_NAME_SHORT, MONTH_NAME_SHORT));

    public static final TimeFormatPattern[] PATTERNS = {
        new GuessPattern(),

        new Rfc2822Pattern(),

        /**
         * <pre>{@code RegexpPattern.new(StandardPatterns::APACHE_CLF, "%d/%b/%Y:%H:%M:%S %z")}</pre>
         */
        new RegexpPattern(APACHE_CLF, "%d/%b/%Y:%H:%M:%S %z"),

        /**
         * <pre>{@code RegexpPattern.new(StandardPatterns::ANSI_C_ASCTIME, "%a %b %e %H:%M:%S %Y")}</pre>
         */
        new RegexpPattern(ANSI_C_ASCTIME, "%a %b %e %H:%M:%S %Y"),
    };
}
