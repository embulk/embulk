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

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * <p>It is a clone from {@code embulk-util-guess}. Its Java package has just changed.
 * It is tentatively here until {@code embulk-guess-csv} starts using {@code embulk-util-*} librarires.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/time_format_guess.rb#L171-L314">time_format_guess.rb</a>
 */
final class GuessPattern implements TimeFormatPattern {
    @Override
    public TimeFormatMatch match(final String text) {
        final ArrayList<String> delimiters = new ArrayList<>();
        final ArrayList<GuessPart> parts = new ArrayList<>();
        final ArrayList<GuessOption> partOptions = new ArrayList<>();

        final GuessDatePicker datePicker = GuessDatePicker.from(text);
        if (datePicker == null) {
            return null;
        }

        final String dateDelim = datePicker.getDateDelim();

        switch (datePicker.getOrder()) {
            case YMD:
                // if dm = (/^#{YMD}(?<rest>.*?)$/.match(text) or /^#{YMD_NODELIM}(?<rest>.*?)$/.match(text))
                //   date_delim = dm["date_delim"] rescue ""
                //
                //   parts << :year
                //   part_options << nil
                //
                //   delimiters << date_delim
                //   parts << :month
                //   part_options << part_heading_option(dm["month"])
                //
                //   delimiters << date_delim
                //   parts << :day
                //   part_options << part_heading_option(dm["day"])
                parts.add(GuessPart.YEAR);
                partOptions.add(GuessOption.NIL);

                delimiters.add(dateDelim);
                parts.add(GuessPart.MONTH);
                partOptions.add(partHeadingOption(datePicker.getMonth()));

                delimiters.add(dateDelim);
                parts.add(GuessPart.DAY);
                partOptions.add(partHeadingOption(datePicker.getDay()));
                break;

            case MDY:
                // elsif dm = (/^#{MDY}(?<rest>.*?)$/.match(text) or /^#{MDY_NODELIM}(?<rest>.*?)$/.match(text))
                //   date_delim = dm["date_delim"] rescue ""
                //
                //   parts << :month
                //   part_options << part_heading_option(dm["month"])
                //
                //   delimiters << date_delim
                //   parts << :day
                //   part_options << part_heading_option(dm["day"])
                //
                //   delimiters << date_delim
                //   parts << :year
                //   part_options << nil
                parts.add(GuessPart.MONTH);
                partOptions.add(partHeadingOption(datePicker.getMonth()));

                delimiters.add(dateDelim);
                parts.add(GuessPart.DAY);
                partOptions.add(partHeadingOption(datePicker.getDay()));

                delimiters.add(dateDelim);
                parts.add(GuessPart.YEAR);
                partOptions.add(GuessOption.NIL);
                break;

            case DMY:
                // elsif dm = (/^#{DMY}(?<rest>.*?)$/.match(text) or /^#{DMY_NODELIM}(?<rest>.*?)$/.match(text))
                //   date_delim = dm["date_delim"] rescue ""
                //
                //   parts << :day
                //   part_options << part_heading_option(dm["day"])
                //
                //   delimiters << date_delim
                //   parts << :month
                //   part_options << part_heading_option(dm["month"])
                //
                //   delimiters << date_delim
                //   parts << :year
                //   part_options << nil
                parts.add(GuessPart.DAY);
                partOptions.add(partHeadingOption(datePicker.getDay()));

                delimiters.add(dateDelim);
                parts.add(GuessPart.MONTH);
                partOptions.add(partHeadingOption(datePicker.getMonth()));

                delimiters.add(dateDelim);
                parts.add(GuessPart.YEAR);
                partOptions.add(GuessOption.NIL);
                break;

            default:
                // else
                //   date_delim = ""
                //   return nil
                return null;
        }

        final String restOfDate = datePicker.getRest();

        final GuessTimePicker timePicker = GuessTimePicker.from(restOfDate, dateDelim);
        final String restOfTime;
        if (timePicker != null) {
            final String dateTimeDelim = timePicker.getDateTimeDelim();
            final String timeDelim = timePicker.getTimeDelim();

            delimiters.add(dateTimeDelim);
            parts.add(GuessPart.HOUR);
            partOptions.add(partHeadingOption(timePicker.getHour()));

            if (timePicker.getMinute() != null && !timePicker.getMinute().isEmpty()) {
                delimiters.add(timeDelim);
                parts.add(GuessPart.MINUTE);
                partOptions.add(partHeadingOption(timePicker.getMinute()));

                if (timePicker.getSecond() != null && !timePicker.getSecond().isEmpty()) {
                    delimiters.add(timeDelim);
                    parts.add(GuessPart.SECOND);
                    partOptions.add(partHeadingOption(timePicker.getSecond()));

                    if (timePicker.getFrac() != null && !timePicker.getFrac().isEmpty()) {
                        delimiters.add(timePicker.getFracDelim());
                        parts.add(GuessPart.FRAC);
                        if (timePicker.getFrac().length() <= 3) {
                            partOptions.add(GuessOption.FRAC_3);
                        } else {
                            partOptions.add(GuessOption.FRAC_N);
                        }
                    }
                }
            }
            restOfTime = timePicker.getRest();
        } else {
            restOfTime = restOfDate;
        }

        final GuessZonePicker zonePicker = GuessZonePicker.from(restOfTime);
        if (zonePicker != null) {
            delimiters.add(zonePicker.getZoneSpace());
            parts.add(GuessPart.ZONE);
            if (zonePicker.getZoneOff() != null && !zonePicker.getZoneOff().isEmpty()) {
                if (zonePicker.getZoneOff().contains(":")) {
                    partOptions.add(GuessOption.EXTENDED);
                } else {
                    partOptions.add(GuessOption.SIMPLE);
                }
            } else {
                partOptions.add(GuessOption.ABB);
            }

            return new GuessMatch(delimiters, parts, partOptions);
        }

        if (SPACES.matcher(restOfTime).matches()) {
            return new GuessMatch(delimiters, parts, partOptions);
        }

        return null;
    }

    /**
     * Returns a good corresponding head option (such as '0' of "%0H") for an example number string.
     *
     * <p>For example, it returns ZERO ('0') for "05", BLANK (' ') for " 7", and NONE ('') for "9".
     */
    private static GuessOption partHeadingOption(final String text) {
        if (text.charAt(0) == '0') {
            return GuessOption.ZERO;
        } else if (text.charAt(0) == ' ') {
            return GuessOption.BLANK;
        } else if (text.length() == 1) {
            return GuessOption.NONE;
        }
        return GuessOption.NIL;
    }

    private static final Pattern SPACES = Pattern.compile(String.format("^\\s*$"));
}
