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

import java.util.regex.Pattern;

/**
 * <p>It is a clone from {@code embulk-util-guess}. Its Java package has just changed.
 * It is tentatively here until {@code embulk-guess-csv} starts using {@code embulk-util-*} librarires.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/time_format_guess.rb#L362-L375">time_format_guess.rb</a>
 */
final class RegexpPattern implements TimeFormatPattern {
    RegexpPattern(final Pattern regexp, final String formatToBe) {
        this.regexp = regexp;
        this.formatToBe = formatToBe;
    }

    @Override
    public TimeFormatMatch match(final String text) {
        if (this.regexp.matcher(text).matches()) {
            return new SimpleMatch(this.formatToBe);
        }
        return null;
    }

    private final Pattern regexp;
    private final String formatToBe;
}
