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
import static org.embulk.guess.csv.timeformat.Parts.ZONE_ABB;
import static org.embulk.guess.csv.timeformat.Parts.ZONE_OFF;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>It is a clone from {@code embulk-util-guess}. Its Java package has just changed.
 * It is tentatively here until {@code embulk-guess-csv} starts using {@code embulk-util-*} librarires.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/time_format_guess.rb#L331-L360">time_format_guess.rb</a>
 */
final class Rfc2822Pattern implements TimeFormatPattern {
    Rfc2822Pattern() {}

    @Override
    public TimeFormatMatch match(final String text) {
        final Matcher matcher = REGEX.matcher(text);

        if (matcher.matches()) {
            final StringBuilder format = new StringBuilder();
            if (matcher.group("weekday") != null && !matcher.group("weekday").isEmpty()) {
                format.append("%a, ");
            }
            format.append("%d %b %Y");
            if (matcher.group("time") != null && !matcher.group("time").isEmpty()) {
                format.append(" %H:%M");
            }
            if (matcher.group("second") != null && !matcher.group("second").isEmpty()) {
                format.append(":%S");
            }
            if (matcher.group("zoneOff") != null && !matcher.group("zoneOff").isEmpty()) {
                if (matcher.group("zoneOff").contains(":")) {
                    format.append(" %:z");
                } else {
                    format.append(" %z");
                }
            } else if (matcher.group("zoneAbb") != null && !matcher.group("zoneAbb").isEmpty()) {
                // don't use %Z: https://github.com/jruby/jruby/issues/3702
                format.append(" %z");
            }
            return new SimpleMatch(format.toString());
        }

        return null;
    }

    @SuppressWarnings("checkstyle:LineLength")
    /**
     * The regular expression of the RFC 2822 pattern.
     *
     * <pre>{@code  # Original code in Ruby.
     *  @regexp = /^(?<weekday>#{WEEKDAY_NAME_SHORT}, )?\d\d #{MONTH_NAME_SHORT} \d\d\d\d(?<time> \d\d:\d\d(?<second>:\d\d)? (?:(?<zone_off>#{ZONE_OFF})|(?<zone_abb>#{ZONE_ABB})))?$/}</pre>
     */
    private static final Pattern REGEX = Pattern.compile(String.format(
            "^(?<weekday>%s, )?\\d\\d %s \\d\\d\\d\\d(?<time> \\d\\d:\\d\\d(?<second>:\\d\\d)? (?:(?<zoneOff>%s)|(?<zoneAbb>%s)))?$",
            WEEKDAY_NAME_SHORT, MONTH_NAME_SHORT, ZONE_OFF, ZONE_ABB));
}
