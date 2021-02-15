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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

/**
 * <p>It is a clone from {@code embulk-util-guess}. Its Java package has just changed.
 * It is tentatively here until {@code embulk-guess-csv} starts using {@code embulk-util-*} librarires.
 */
final class GuessMatch implements TimeFormatMatch {
    GuessMatch(
            final List<String> delimiters,
            final List<GuessPart> parts,
            final List<GuessOption> partOptions) {
        this.delimiters = Collections.unmodifiableList(new ArrayList<>(delimiters));
        this.parts = new ArrayList<>(parts);
        this.partOptions = new ArrayList<>(partOptions);
    }

    @Override
    public String getFormat() {
        final StringBuilder format = new StringBuilder();

        for (int i = 0; i < this.parts.size(); ++i) {
            if (i != 0) {
                format.append(this.delimiters.get(i - 1));
            }
            final GuessOption option = this.partOptions.get(i);

            switch (this.parts.get(i)) {
                case YEAR:
                    format.append("%Y");
                    break;

                case MONTH:
                    switch (option) {
                        case ZERO:
                            format.append("%m");
                            break;
                        case BLANK:
                            // format.append("%_m");  // not supported
                            format.append("%m");
                            break;
                        case NONE:
                            // format.append("%-m");  // not supported
                            format.append("%m");
                            break;
                        default:
                            format.append("%m");
                    }
                    break;

                case DAY:
                    switch (option) {
                        case ZERO:
                            format.append("%d");
                            break;
                        case BLANK:
                            format.append("%e");
                            break;
                        case NONE:
                            format.append("%d");  // not supported
                            break;
                        default:
                            format.append("%d");
                    }
                    break;

                case HOUR:
                    switch (option) {
                        case ZERO:
                            format.append("%H");
                            break;
                        case BLANK:
                            format.append("%k");
                            break;
                        case NONE:
                            format.append("%k");  // not supported
                            break;
                        default:
                            format.append("%H");
                    }
                    break;

                case MINUTE:
                    // heading options are not supported
                    format.append("%M");
                    break;

                case SECOND:
                    // heading options are not supported
                    format.append("%S");
                    break;

                case FRAC:
                    switch (option) {
                        case FRAC_3:
                            format.append("%L");
                            break;
                        case FRAC_N:
                            format.append("%N");
                            break;
                        default:
                            format.append("%N");
                            break;
                    }
                    break;

                case ZONE:
                    switch (option) {
                        case EXTENDED:
                            format.append("%:z");
                            break;
                        default:
                            // :simple, :abb
                            // don't use %Z even with :abb: https://github.com/jruby/jruby/issues/3702
                            format.append("%z");
                            break;
                    }
                    break;

                default:
                    throw new RuntimeException("Unknown part: #{@parts[i]}");
            }
        }

        return format.toString();
    }

    @Override
    public String getIdentifier() {
        // MDY is mergeable with DMY.
        final OptionalInt i = findSubsequence(this.parts, DMY_SEQUENCE);

        if (i.isPresent()) {
            final int index = i.getAsInt();

            final ArrayList<GuessPart> ps = new ArrayList<>(this.parts);
            ps.set(index, GuessPart.MONTH);
            ps.set(index + 1, GuessPart.DAY);
            ps.set(index + 2, GuessPart.YEAR);
            return buildIdentifier(this.delimiters, Collections.unmodifiableList(ps));
        }

        return buildIdentifier(this.delimiters, this.parts);
    }

    /**
     * <pre>{@code  # Original code in Ruby.
     * def merge!(another_in_group)
     *   part_options = another_in_group.part_options
     *   @part_options.size.times do |i|
     *     @part_options[i] ||= part_options[i]
     *     if @part_options[i] == nil
     *       part_options[i]
     *     elsif part_options[i] == nil
     *       @part_options[i]
     *     else
     *       [@part_options[i], part_options[i]].sort.last
     *     end
     *   end

     *   # if DMY matches, MDY is likely false match of DMY.
     *   dmy = array_sequence_find(another_in_group.parts, [:day, :month, :year])
     *   mdy = array_sequence_find(@parts, [:month, :day, :year])
     *   if mdy && dmy
     *     @parts[mdy, 3] = [:day, :month, :year]
     *   end
     * end}</pre>
     */
    @Override
    public void mergeFrom(final TimeFormatMatch anotherInGroup) {
        if (!(anotherInGroup instanceof GuessMatch)) {
            return;
        }
        final GuessMatch another = (GuessMatch) anotherInGroup;
        final List<GuessOption> anotherPartOptions = another.getPartOptions();

        for (int i = 0; i < this.partOptions.size(); i++) {
            if (this.partOptions.get(i) == null) {
                this.partOptions.set(i, anotherPartOptions.get(i));
            }

            // The Ruby version had the if-else-clause below, but it should not have caused any effect.
            //
            // if @part_options[i] == nil
            //   part_options[i]
            // elsif part_options[i] == nil
            //   @part_options[i]
            // else
            //   [@part_options[i], part_options[i]].sort.last
            // end
        }

        // if DMY matches, MDY is likely false match of DMY.
        final OptionalInt dmy = findSubsequence(another.getParts(), DMY_SEQUENCE);
        final OptionalInt mdy = findSubsequence(this.parts, MDY_SEQUENCE);

        if (mdy.isPresent() && dmy.isPresent()) {
            final int index = mdy.getAsInt();
            this.parts.set(index, GuessPart.DAY);
            this.parts.set(index + 1, GuessPart.MONTH);
            this.parts.set(index + 2, GuessPart.YEAR);
        }
    }

    List<GuessPart> getParts() {
        return this.parts;
    }

    List<GuessOption> getPartOptions() {
        return this.partOptions;
    }

    @Override
    public String toString() {
        return buildIdentifier(this.delimiters, this.parts);
    }

    /**
     * Finds a subsequence {@code target} in the entire sequence {@code entire}.
     *
     * <pre>{@code
     * def array_sequence_find(array, seq)
     *   (array.size - seq.size + 1).times {|i|
     *     return i if array[i, seq.size] == seq
     *   }
     *   return nil
     * end}</pre>
     */
    private static OptionalInt findSubsequence(final List<GuessPart> entire, final List<GuessPart> target) {
        for (int i = 0; i < (entire.size() - target.size() + 1); i++) {
            if (entire.subList(i, i + target.size()).equals(target)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private static String buildIdentifier(final List<String> delimiters, final List<GuessPart> parts) {
        final StringBuilder builder = new StringBuilder();
        for (final GuessPart part : parts) {
            builder.append("$");
            builder.append(part.toString());
        }
        for (final String delimiter : delimiters) {
            builder.append("@");
            builder.append(delimiter.toString());
        }
        return String.format("GuessMatch[%s]", builder.toString());
    }

    private static final List<GuessPart> DMY_SEQUENCE = Arrays.asList(GuessPart.DAY, GuessPart.MONTH, GuessPart.YEAR);
    private static final List<GuessPart> MDY_SEQUENCE = Arrays.asList(GuessPart.MONTH, GuessPart.DAY, GuessPart.YEAR);

    private final List<String> delimiters;

    // NOTE: They are mutable to get "merged".
    private final ArrayList<GuessPart> parts;
    private final ArrayList<GuessOption> partOptions;
}
