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

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.embulk.config.ConfigDiff;
import org.embulk.spi.Buffer;
import org.embulk.spi.Exec;

/**
 * Guesses a character set from {@link org.embulk.spi.Buffer}.
 *
 * <p>It reimplements {@code CharsetGuessPlugin} in {@code /embulk/guess/charset.rb}.
 *
 * <p>It is a clone from {@code embulk-util-guess}. It changed its package, to be static, and not to use {@code embulk-util-config}.
 * It is tentatively here until {@code embulk-guess-csv} starts using {@code embulk-util-*} librarires.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/charset.rb">charset.rb</a>
 */
final class CharsetGuess {
    /**
     * Guesses a character set from {@link org.embulk.spi.Buffer}.
     *
     * @param sample  the byte sequence to be guessed
     * @return {@link org.embulk.config.ConfigDiff} guessed
     */
    static ConfigDiff guess(final Buffer sample) {
        final CharsetDetector detector = new CharsetDetector();

        final int sampleLength = sample.limit();
        final byte[] sampleArray = new byte[sampleLength];
        sample.getBytes(0, sampleArray, 0, sampleLength);

        detector.setText(sampleArray);

        final CharsetMatch bestMatch = detector.detect();

        final ConfigDiff charset = Exec.newConfigDiff();
        if (bestMatch.getConfidence() < 50) {
            charset.set("charset", "UTF-8");
        } else {
            charset.set("charset", convertPredefined(bestMatch.getName()));
        }

        final ConfigDiff result = Exec.newConfigDiff();
        result.setNested("parser", charset);
        return result;
    }

    private static String convertPredefined(final String before) {
        switch (before) {
            // ISO-8859-1 means ASCII which is a subset of UTF-8 in most of cases
            // due to lack of sample data set.
            case "ISO-8859-1":
                return "UTF-8";

            // Shift_JIS is used almost only by Windows that uses "CP932" in fact.
            // And "CP932" called by Microsoft actually means "MS932" in Java.
            case "Shift_JIS":
                return "MS932";

            default:
                return before;
        }
    }
}
