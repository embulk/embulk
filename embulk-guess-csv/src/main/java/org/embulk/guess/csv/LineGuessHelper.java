/*
 * Copyright 2021 The Embulk project
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Buffer;
import org.embulk.spi.util.LineDecoder;
import org.embulk.spi.util.ListFileInput;
import org.embulk.spi.util.Newline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is tentatively here until embulk-guess-csv starts using embulk-util-* librarires.
 */
final class LineGuessHelper {
    static List<String> toLines(final ConfigSource config, final Buffer sample) {
        final ConfigSource parserConfig = config.getNestedOrGetEmpty("parser");

        if (!parserConfig.has("charset")) {
            try {
                parserConfig.set("charset", guessCharset(sample));
            } catch (final IllegalArgumentException ex) {
                logger.warn(ex.getMessage(), ex);
                return null;
            }
        }

        if (!parserConfig.has("newline")) {
            try {
                parserConfig.set("newline", guessNewline(sample));
            } catch (final IllegalArgumentException ex) {
                logger.warn(ex.getMessage(), ex);
                return null;
            }
        }

        final LineDecoder.DecoderTask parserTask = parserConfig.loadConfig(LineDecoder.DecoderTask.class);

        final ArrayList<Buffer> listBuffer = new ArrayList<>();
        listBuffer.add(sample);
        final ArrayList<ArrayList<Buffer>> listListBuffer = new ArrayList<>();
        listListBuffer.add(listBuffer);
        final LineDecoder decoder = new LineDecoder(new ListFileInput(listListBuffer), parserTask);

        final boolean endsWithNewline = endsWith(sample, parserTask.getNewline());

        final ArrayList<String> sampleLines = new ArrayList<>();
        while (decoder.nextFile()) {  // TODO: Confirm decoder contains only one, and stop looping.
            while (true) {
                final String line = decoder.poll();
                if (line == null) {
                    break;
                }
                sampleLines.add(line);
            }
            if (!endsWithNewline && !sampleLines.isEmpty()) {
                sampleLines.remove(sampleLines.size() - 1);  // last line is partial
            }
        }

        return Collections.unmodifiableList(sampleLines);
    }

    private static String guessCharset(final Buffer sample) {
        final CharsetDetector detector = new CharsetDetector();

        final int sampleLength = sample.limit();
        final byte[] sampleArray = new byte[sampleLength];
        sample.getBytes(0, sampleArray, 0, sampleLength);

        detector.setText(sampleArray);

        final CharsetMatch bestMatch = detector.detect();

        if (bestMatch.getConfidence() < 50) {
            return "UTF-8";
        }
        return convertCharsetPredefined(bestMatch.getName());
    }

    private static String convertCharsetPredefined(final String before) {
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

    private static boolean endsWith(final Buffer buffer, final Newline target) {
        switch (target) {
            case CR:
            case LF:
                if (buffer.offset() + buffer.limit() - 1 >= 0) {
                    final byte[] last = new byte[1];
                    buffer.getBytes(buffer.limit() - 1, last, 0, 1);
                    return ((char) last[0]) == target.getFirstCharCode();
                }
                return false;

            case CRLF:
                if (buffer.offset() + buffer.limit() - 2 >= 0) {
                    final byte[] last = new byte[2];
                    buffer.getBytes(buffer.limit() - 2, last, 0, 2);
                    return ((char) last[0]) == target.getFirstCharCode() && ((char) last[1]) == target.getSecondCharCode();
                }
                return false;

            default:
                return false;
        }
    }

    private static String guessNewline(final Buffer sample) {
        final int sampleLength = sample.limit();
        final byte[] sampleArray = new byte[sampleLength];
        sample.getBytes(0, sampleArray, 0, sampleLength);

        final int crCount = count(sampleArray, CR);
        final int lfCount = count(sampleArray, LF);
        final int crlfCount = count(sampleArray, CRLF);

        if (crlfCount > crCount / 2 && crlfCount > lfCount / 2) {
            return "CRLF";
        } else if (crCount > lfCount / 2) {
            return "CR";
        } else {
            return "LF";
        }
    }

    private static int count(final byte[] array, final byte[] target) {
        if (target.length == 0) {
            return 0;
        }

        int count = 0;
        outer: for (int i = 0; i < array.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            count++;
        }
        return count;
    }

    private static final byte[] CRLF = { (byte) '\r', (byte) '\n' };
    private static final byte[] CR = { (byte) '\r' };
    private static final byte[] LF = { (byte) '\n' };

    private static final Logger logger = LoggerFactory.getLogger(LineGuessHelper.class);
}
