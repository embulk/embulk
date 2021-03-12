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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.embulk.guess.csv.timeformat.ExpectedPatterns;
import org.embulk.guess.csv.timeformat.TimeFormatMatch;
import org.embulk.guess.csv.timeformat.TimeFormatPattern;

/**
 * Guesses a time format from objects.
 *
 * <p>It reimplements {@code TimeFormatGuess} in {@code /embulk/guess/time_format_guess.rb}.
 *
 * <p>It is a clone from {@code embulk-util-guess}. Its Java package has just changed.
 * It is tentatively here until {@code embulk-guess-csv} starts using {@code embulk-util-*} librarires.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/time_format_guess.rb">time_format_guess.rb</a>
 */
public final class TimeFormatGuess {
    private TimeFormatGuess() {
    }

    public static TimeFormatGuess of() {
        return new TimeFormatGuess();
    }

    /**
     * Guesses a time format from objects.
     *
     * @param texts  a sequence of strings used to guess
     * @return the timestamp format string guessed
     */
    public String guess(final Iterable<Object> texts) {
        final ArrayList<TimeFormatMatch> matches = new ArrayList<>();
        for (final Object textObject : texts) {
            final String text = textObject.toString();
            if (text.isEmpty()) {
                continue;
            }
            for (final TimeFormatPattern pattern : ExpectedPatterns.PATTERNS) {
                final TimeFormatMatch match = pattern.match(text);
                if (match != null) {
                    matches.add(match);
                }
            }
        }
        if (matches.isEmpty()) {
            return null;
        } else if (matches.size() == 1) {
            return matches.get(0).getFormat();
        }

        return mergeMostFrequentMatches(matches).getFormat();
    }

    /**
     * Merges all the most frequent {@code TimeFormatMatch}s whose "identifier"s are the same.
     *
     * <pre>{@code  # Original code in Ruby.
     *  match_groups = matches.group_by {|match| match.mergeable_group }.values
     *  best_match_group = match_groups.sort_by {|group| group.size }.last
     *  best_match = best_match_group.shift
     *  best_match_group.each {|m| best_match.merge!(m) }
     *  return best_match.format}</pre>
     */
    static TimeFormatMatch mergeMostFrequentMatches(final List<TimeFormatMatch> matches) {
        final Collection<List<TimeFormatMatch>> matchGroups =
                matches.stream().collect(Collectors.groupingBy(TimeFormatMatch::getIdentifier)).values();
        final Optional<List<TimeFormatMatch>> bestMatchGroup = matchGroups.stream().max(Comparator.comparing(List::size));
        if (!bestMatchGroup.isPresent() || bestMatchGroup.get().isEmpty()) {
            return null;
        }

        final TimeFormatMatch bestMatch = bestMatchGroup.get().get(0);
        for (final TimeFormatMatch match : bestMatchGroup.get()) {
            bestMatch.mergeFrom(match);
        }
        return bestMatch;
    }
}
