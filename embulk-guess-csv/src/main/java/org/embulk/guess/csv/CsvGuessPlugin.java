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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.parser.csv.CsvParserPlugin;
import org.embulk.parser.csv.CsvTokenizer;
import org.embulk.spi.Buffer;
import org.embulk.spi.Exec;
import org.embulk.spi.GuessPlugin;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.modules.TypeModule;
import org.embulk.util.file.ListFileInput;
import org.embulk.util.guess.CharsetGuess;
import org.embulk.util.guess.LineGuessHelper;
import org.embulk.util.guess.NewlineGuess;
import org.embulk.util.text.LineDecoder;
import org.embulk.util.text.Newline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvGuessPlugin implements GuessPlugin {
    @Override
    public ConfigDiff guess(final ConfigSource config, final Buffer sample) {
        final ConfigSource parserConfig = config.getNestedOrGetEmpty("parser");

        // If "charset" is not set yet, return only with a charset guessed.
        if (!parserConfig.has("charset")) {
            return CharsetGuess.of(CONFIG_MAPPER_FACTORY).guess(sample);
        }

        // If "newline" is not set yet, return only with a newline guessed.
        if (!parserConfig.has("newline")) {
            return NewlineGuess.of(CONFIG_MAPPER_FACTORY).guess(config, sample);
        }

        return this.guessLines(config, LineGuessHelper.of(CONFIG_MAPPER_FACTORY).toLines(config, sample));
    }

    ConfigDiff guessLines(final ConfigSource config, final List<String> sampleLines) {
        final ConfigDiff configDiff = newConfigDiff();

        // return {} unless config.fetch("parser", {}).fetch("type", "csv") == "csv"
        if (!"csv".equals(config.getNestedOrGetEmpty("parser").get(String.class, "type", "csv"))) {
            return configDiff;
        }

        final ConfigSource parserConfig = config.getNestedOrGetEmpty("parser");
        final String delim;

        // if parser_config["type"] == "csv" && parser_config["delimiter"]
        if ("csv".equals(parserConfig.get(String.class, "type", "csv")) && parserConfig.has("delimiter")) {
            delim = parserConfig.get(String.class, "delimiter");
        } else {
            delim = this.guessDelimiter(sampleLines);
        }

        final ConfigDiff parserGuessed = newConfigDiff();
        parserGuessed.merge(parserConfig);
        parserGuessed.set("type", "csv");
        parserGuessed.set("delimiter", delim);

        if (!parserGuessed.has("quote")) {
            final String quote = guessQuote(sampleLines, delim);
            if (quote == null) {  // quote may be null.
                // Setting null explicitly for compatibility. To set null explicitly, ConfigDiff#setNested is needed.
                parserGuessed.setNested("quote", null);
            } else {
                parserGuessed.set("quote", quote);
            }
        }
        // setting '' is not allowed any more. this line converts obsoleted config syntax to explicit syntax.
        if ("".equals(parserGuessed.get(String.class, "quote"))) {
            parserGuessed.set("quote", "\"");
        }

        if (!parserGuessed.has("escape")) {
            final String quote = parserGuessed.get(String.class, "quote");
            if (quote != null) {
                final String escape = guessEscape(sampleLines, delim, quote);
                if (escape == null) {  // escape may be null.
                    // Setting null explicitly for compatibility. To set null explicitly, ConfigDiff#setNested is needed.
                    parserGuessed.setNested("escape", null);
                } else {
                    parserGuessed.set("escape", escape);
                }
            } else {
                // escape does nothing if quote is disabled
            }
        }

        if (!parserGuessed.has("null_string")) {
            final String nullString = guessNullString(sampleLines, delim);
            if (nullString != null) {
                parserGuessed.set("null_string", nullString);
            }
            // don't even set null_string to avoid confusion of null and 'null' in YAML format
        }

        // guessing skip_header_lines should be before guessing guess_comment_line_marker
        // because lines supplied to CsvTokenizer already don't include skipped header lines.
        // skipping empty lines is also disabled here because skipping header lines is done by
        // CsvParser which doesn't skip empty lines automatically

        final List<List<String>> sampleRecordsBeforeSkip = splitLines(parserGuessed, false, sampleLines, delim, null);
        final int skipHeaderLines = guessSkipHeaderLines(sampleRecordsBeforeSkip);
        final List<String> skippedSampleLines = sampleLines.subList(skipHeaderLines, sampleLines.size());
        final List<List<String>> skippedSampleRecords = sampleRecordsBeforeSkip.subList(skipHeaderLines, sampleRecordsBeforeSkip.size());

        final List<String> uncommentedSampleLines;
        if (parserGuessed.has("comment_line_marker")) {
            uncommentedSampleLines = skippedSampleLines;
        } else {
            uncommentedSampleLines = guessCommentLineMarker(
                skippedSampleLines,
                delim,
                parserGuessed.get(String.class, "quote"),
                parserGuessed.get(String.class, "null_string", null),
                parserGuessed);
        }

        final List<List<String>> sampleRecords = splitLines(parserGuessed, true, uncommentedSampleLines, delim, null);

        // It should fail if CSV parser cannot parse sample_lines.
        if (sampleRecords == null || sampleRecords.isEmpty()) {
            return configDiff;
        }

        final boolean headerLine;
        final List<SchemaGuess.GuessedType> columnTypes;
        if (uncommentedSampleLines.size() == 1) {
            // The file contains only 1 line. Assume that there are no header line.
            headerLine = false;

            if (parserGuessed.has("trim_if_not_quoted")) {
                columnTypes = SCHEMA_GUESS.typesFromListRecords(sampleRecords.subList(0, 1));
            } else {
                final List<List<String>> sampleRecordsTrimmed = splitLines(parserGuessed, true, uncommentedSampleLines, delim, true);
                final List<SchemaGuess.GuessedType> columnTypesTrimmed = SCHEMA_GUESS.typesFromListRecords(sampleRecordsTrimmed);

                final List<SchemaGuess.GuessedType> columnTypesUntrimmed = SCHEMA_GUESS.typesFromListRecords(sampleRecords.subList(0, 1));
                if (columnTypesUntrimmed.equals(columnTypesTrimmed)) {
                    parserGuessed.set("trim_if_not_quoted", false);
                    columnTypes = columnTypesUntrimmed;
                } else {
                    parserGuessed.set("trim_if_not_quoted", true);
                    columnTypes = columnTypesTrimmed;
                }
            }
        } else {
            // The file contains more than 1 line. If guessed first line's column types are all strings or boolean, and the types are
            // different from the other lines, assume that the first line is column names.
            final List<SchemaGuess.GuessedType> firstTypes = SCHEMA_GUESS.typesFromListRecords(sampleRecords.subList(0, 1));
            final List<SchemaGuess.GuessedType> otherTypesUntrimmed =
                    SCHEMA_GUESS.typesFromListRecords(sampleRecords.subList(1, sampleRecords.size()));

            logger.debug("Types of the first line : {}", firstTypes);
            logger.debug("Types of the other lines (untrimmed): {}", otherTypesUntrimmed);

            final List<SchemaGuess.GuessedType> otherTypes;

            if (parserGuessed.has("trim_if_not_quoted")) {
                otherTypes = otherTypesUntrimmed;
            } else {
                final List<List<String>> sampleRecordsTrimmed = splitLines(parserGuessed, true, uncommentedSampleLines, delim, true);
                final List<SchemaGuess.GuessedType> otherTypesTrimmed =
                        SCHEMA_GUESS.typesFromListRecords(sampleRecordsTrimmed.subList(1, sampleRecordsTrimmed.size()));
                if (otherTypesUntrimmed.equals(otherTypesTrimmed)) {
                    parserGuessed.set("trim_if_not_quoted", false);
                    otherTypes = otherTypesUntrimmed;
                } else {
                    parserGuessed.set("trim_if_not_quoted", true);
                    otherTypes = otherTypesTrimmed;
                }
            }

            logger.debug("Types of the other lines: {}", otherTypes);

            headerLine = ((!firstTypes.equals(otherTypes)
                                 && firstTypes.stream().allMatch(t -> SchemaGuess.GuessedType.STRING.equals(t) || SchemaGuess.GuessedType.BOOLEAN.equals(t)))
                             || guessStringHeaderLine(sampleRecords));

            logger.debug("headerLine: {}", headerLine);

            columnTypes = otherTypes;
        }

        if (columnTypes.isEmpty()) {
            // TODO here is making the guessing failed if the file doesn't contain any columns. However,
            //      this may not be convenient for users.
            return configDiff;
        }

        if (headerLine) {
            parserGuessed.set("skip_header_lines", skipHeaderLines + 1);
        } else {
            parserGuessed.set("skip_header_lines", skipHeaderLines);
        }

        if (!parserGuessed.has("allow_extra_columns")) {
            parserGuessed.set("allow_extra_columns", false);
        }
        if (!parserGuessed.has("allow_optional_columns")) {
            parserGuessed.set("allow_optional_columns", false);
        }

        final List<String> columnNames;
        if (headerLine) {
            columnNames = sampleRecords.get(0).stream().map(String::trim).collect(Collectors.toList());
        } else {
            columnNames = IntStream.range(0, columnTypes.size()).mapToObj(i -> "c" + i).collect(Collectors.toList());
        }

        // We want Stream#zip...
        final List<ConfigDiff> schema = IntStream.range(0, Math.min(columnNames.size(), columnTypes.size()))
                .mapToObj(i -> this.newColumn(columnNames.get(i), columnTypes.get(i)))
                .collect(Collectors.toList());

        parserGuessed.set("columns", schema);
        configDiff.setNested("parser", parserGuessed);
        return configDiff;
    }

    /**
     * Creates a new column configuration appropriate for the given column name and guessed type.
     *
     * <p>It is overridden by {@code embulk-guess-csv_all_strings}.
     *
     * @param name  the column name
     * @param type  a guessed type
     * @return a new column config
     */
    protected ConfigDiff newColumn(final String name, final SchemaGuess.GuessedType type) {
        final ConfigDiff column = newConfigDiff();
        column.set("name", name);
        column.set("type", type.toString());
        if (type.isTimestamp()) {
            column.set("format", type.getFormatOrTimeValue());
        }
        return column;
    }

    protected static ConfigDiff newConfigDiff() {
        return CONFIG_MAPPER_FACTORY.newConfigDiff();
    }

    private static List<List<String>> splitLines(
            final ConfigDiff parserConfig,
            final boolean skipEmptyLines,
            final List<String> sampleLines,
            final String delim,
            final Boolean trimIfNotQuoted) {
        try {
            final String nullString = parserConfig.get(String.class, "null_string", null);
            final ConfigSource config = CONFIG_MAPPER_FACTORY.newConfigSource();
            config.merge(parserConfig);
            if (trimIfNotQuoted != null) {
                config.set("trim_if_not_quoted", (boolean) trimIfNotQuoted);
            }
            config.set("charset", "UTF-8");
            config.set("columns", new ArrayList<>());

            final CsvParserPlugin.PluginTask parserTask =
                    CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, CsvParserPlugin.PluginTask.class);

            final byte[] data = joinBytes(sampleLines, parserTask.getNewline());
            final Buffer sample = Exec.getBufferAllocator().allocate(data.length);
            sample.setBytes(0, data, 0, data.length);
            sample.limit(data.length);

            final ArrayList<Buffer> listBuffer = new ArrayList<>();
            listBuffer.add(sample);
            final ArrayList<ArrayList<Buffer>> listListBuffer = new ArrayList<>();
            listListBuffer.add(listBuffer);
            final LineDecoder decoder = LineDecoder.of(
                    new ListFileInput(listListBuffer), parserTask.getCharset(), parserTask.getLineDelimiterRecognized().orElse(null));
            final CsvTokenizer tokenizer = new CsvTokenizer(decoder, parserTask);

            final ArrayList<List<String>> rows = new ArrayList<>();
            while (tokenizer.nextFile()) {
                while (tokenizer.nextRecord(skipEmptyLines)) {
                    try {
                        final ArrayList<String> columns = new ArrayList<>();
                        while (true) {
                            try {
                                final String column = tokenizer.nextColumn();
                                final boolean quoted = tokenizer.wasQuotedColumn();
                                if (nullString != null && !quoted && nullString.equals(column)) {
                                    columns.add(null);
                                } else {
                                    columns.add(column);
                                }
                            } catch (final CsvTokenizer.TooFewColumnsException ex) {
                                rows.add(Collections.unmodifiableList(columns));
                                break;
                            }
                        }
                    } catch (final CsvTokenizer.InvalidValueException ex) {
                        // TODO warning
                        tokenizer.skipCurrentLine();
                    }
                }
            }
            return Collections.unmodifiableList(rows);
        } catch (final RuntimeException ex) {
            // TODO warning if fallback to this ad-hoc implementation
            final ArrayList<List<String>> rows = new ArrayList<>();
            for (final String line : sampleLines) {
                final String[] split = line.split(Pattern.quote(delim));
                rows.add(Collections.unmodifiableList(Arrays.asList(split)));
            }
            return Collections.unmodifiableList(rows);
        }
    }

    private String guessDelimiter(final List<String> sampleLines) {
        String selectedDelimiter = null;
        double mostWeight = 0.0;
        for (final char delimiter : DELIMITER_CANDIDATES) {
            final List<Integer> counts = StreamSupport.stream(sampleLines.spliterator(), false)
                    .map(line -> (int) (line.chars().filter(c -> c == delimiter).count()))
                    .collect(Collectors.toList());
            final int total = sumOfList(counts);
            if (total > 0) {
                final double weight = total / standardDeviationOfList(counts);
                if (weight > mostWeight) {
                    selectedDelimiter = "" + delimiter;
                    mostWeight = weight;
                }
            }
        }

        if (selectedDelimiter != null && mostWeight > 1.0) {
            return selectedDelimiter;
        }
        return "" + DELIMITER_CANDIDATES.get(0);  // assuming single column CSV
    }

    private static String guessQuote(final List<String> sampleLines, final String delim) {
        String selectedQuote = null;
        double mostWeight = 0.0;

        final String delimRegex = Pattern.quote(delim);
        for (final char q : QUOTE_CANDIDATES) {
            final String quoteRegex = Pattern.quote("" + q);
            final List<Integer> weights = new ArrayList<>();
            for (final String line : sampleLines) {
                final long count = line.chars().filter(c -> c == q).count();
                if (count > 0L) {
                    weights.add((int) count + weighQuote(line, delimRegex, quoteRegex));
                }
            }
            final double weight = (weights.isEmpty() ? 0.0 : averageOfList(weights));
            if (weight > mostWeight) {
                selectedQuote = "" + q;
                mostWeight = weight;
            }
        }
        if (mostWeight >= 10.0) {
            return selectedQuote;
        }

        if (!guessForceNoQuote(sampleLines, delim, "\"")) {
            // assuming CSV follows RFC for quoting
            return "\"";
        }
        // disable quoting (set null)
        return null;
    }

    private static boolean guessForceNoQuote(final List<String> sampleLines, final String delim, final String quoteCandidate) {
        final String delimRegex = Pattern.quote(delim);
        final String quoteRegex = Pattern.quote(quoteCandidate);
        final Pattern pattern = Pattern.compile(String.format("(?:\\A|%s)\\s*[^%s]+%s", delimRegex, quoteRegex, quoteRegex));
        for (final String line : sampleLines) {
            // quoting character appear at the middle of a non-quoted value
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    private static String guessEscape(final List<String> sampleLines, final String delim, final String quote) {
        int maxCount = 0;
        String selectedEscape = null;
        for (final String str : ESCAPE_CANDIDATES) {
            final Pattern regex = Pattern.compile(String.format(
                    "%s(?:%s|%s)", Pattern.quote(str), Pattern.quote(delim), Pattern.quote(quote)));
            final int count = StreamSupport.stream(sampleLines.spliterator(), false)
                    .mapToInt(line -> (int) (countPattern(line, regex)))
                    .sum();
            if (count > maxCount) {
                selectedEscape = str;
                maxCount = count;
            }
        }

        if (selectedEscape == null) {
            if ("\"".equals(quote)) {
                // assuming this CSV follows RFC for escaping
                return "\"";
            } else {
                // disable escaping (set null)
                return null;
            }
        }

        return selectedEscape;
    }

    private static String guessNullString(final List<String> sampleLines, final String delim) {
        int maxCount = 0;
        String selectedNullString = null;
        for (final String str : NULL_STRING_CANDIDATES) {
            final Pattern regex = Pattern.compile(String.format(
                    "(?:^|%s)%s(?:$|%s)", Pattern.quote(delim), Pattern.quote(str), Pattern.quote(delim)));
            final int count = StreamSupport.stream(sampleLines.spliterator(), false)
                    .mapToInt(line -> (int) (countPattern(line, regex)))
                    .sum();
            if (count > maxCount) {
                selectedNullString = str;
                maxCount = count;
            }
        }

        return selectedNullString;
    }

    private static int guessSkipHeaderLines(final List<List<String>> sampleRecords) {
        final List<Integer> counts = new ArrayList<>();
        for (final List<String> records : sampleRecords) {
            counts.add(records.size());
        }
        for (int i = 1; i <= Math.min(MAX_SKIP_LINES, counts.size() - 1); i++) {
            final int checkRowCount = counts.get(i - 1);
            if (counts.subList(i, Math.min(counts.size(), NO_SKIP_DETECT_LINES)).stream().allMatch(c -> c <= checkRowCount)) {
                return i - 1;
            }
        }
        return 0;
    }

    private static List<String> guessCommentLineMarker(
            final List<String> sampleLines,
            final String delim,
            final String quote,
            final String nullString,
            final ConfigDiff parserGuessed) {
        final ArrayList<Pattern> exclude = new ArrayList<>();
        if (quote != null && !quote.isEmpty()) {
            exclude.add(Pattern.compile("^" + Pattern.quote(quote)));
        }
        if (nullString != null) {
            exclude.add(Pattern.compile(String.format("^%s(?:%s|$)", Pattern.quote(nullString), Pattern.quote(delim))));
        }

        String selectedCommentLineMarker = null;
        List<String> selectedUnmatchLines = null;
        int maxMatchCount = 0;
        for (final String str : COMMENT_LINE_MARKER_CANDIDATES) {
            final Pattern regex = Pattern.compile("^" + Pattern.quote(str));
            final List<String> unmatchLines = sampleLines.stream()
                    .filter(line -> {
                        for (final Pattern ex : exclude) {
                            if (ex.matcher(line).find()) {
                                return true;
                            }
                        }
                        return !regex.matcher(line).find();
                    })
                    .collect(Collectors.toList());
            final int matchCount = sampleLines.size() - unmatchLines.size();
            if (matchCount > maxMatchCount) {
                selectedCommentLineMarker = str;
                selectedUnmatchLines = unmatchLines;
                maxMatchCount = matchCount;
            }
        }

        if (selectedCommentLineMarker != null) {
            parserGuessed.set("comment_line_marker", selectedCommentLineMarker);
            return selectedUnmatchLines;
        } else {
            return sampleLines;
        }
    }

    private static boolean guessStringHeaderLine(final List<List<String>> sampleRecords) {
        final List<String> first = sampleRecords.get(0);  // sampleRecords should contain at least 1 element.
        for (int i = 0; i < first.size(); i++) {
            final int columnIndex = i;
            // Lengths of elements at the |columnIndex| vertically in each row.
            final List<Integer> lengthsAtColumn = StreamSupport.stream(sampleRecords.spliterator(), false)
                    .map(row -> row.get(columnIndex))
                    .filter(element -> element != null)
                    .map(element -> element.length())
                    .collect(Collectors.toList());
            if (lengthsAtColumn.size() > 1) {
                final List<Integer> sub = lengthsAtColumn.subList(1, lengthsAtColumn.size());
                if (varianceOfList(sub) <= 0.2) {
                    final double avg = averageOfList(sub);
                    if (avg == 0 ? (lengthsAtColumn.get(0) > 1) : (Math.abs(avg - (double) lengthsAtColumn.get(0)) / avg > 0.7)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int sumOfList(final List<Integer> integers) {
        return StreamSupport.stream(integers.spliterator(), false).mapToInt(i -> i).sum();
    }

    private static double averageOfList(final List<Integer> integers) {
        return StreamSupport.stream(integers.spliterator(), false).mapToInt(i -> i).average().orElse(0.0);
    }

    private static double varianceOfList(final List<Integer> integers) {
        final double average = averageOfList(integers);
        return StreamSupport.stream(integers.spliterator(), false)
                .mapToDouble(i -> (((double) i) - average) * (((double) i) - average))
                .average()
                .orElse(0.0);
    }

    private static double standardDeviationOfList(final List<Integer> integers) {
        final double result = Math.sqrt(varianceOfList(integers));
        if (result < (double) 0.00000000001) {  // result must be >= 0
            return 0.000000001;
        }
        return result;
    }

    private static int weighQuote(final String line, final String delimRegex, final String quoteRegex) {
        final Pattern patternQ = Pattern.compile(String.format(
                "(?:\\A|%s)\\s*%s(?:(?!%s).)*\\s*%s(?:$|%s)",
                delimRegex, quoteRegex, quoteRegex, quoteRegex, delimRegex));

        final Pattern patternD = Pattern.compile(String.format(
                "(?:\\A|%s)\\s*%s(?:(?!%s).)*\\s*%s(?:$|%s)",
                delimRegex, quoteRegex, delimRegex, quoteRegex, delimRegex));

        return countPattern(line, patternQ) * 20 + countPattern(line, patternD) * 40;
    }

    private static int countPattern(final String string, final Pattern pattern) {
        final Matcher matcher = pattern.matcher(string);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static byte[] joinBytes(final List<String> sampleLines, final Newline newline) {
        final ByteArrayOutputStream data = new ByteArrayOutputStream();

        boolean first = true;
        for (final String line : sampleLines) {
            if (first) {
                first = false;
            } else {
                final byte[] newlineBytes = newline.getString().getBytes(StandardCharsets.UTF_8);
                data.write(newlineBytes, 0, newlineBytes.length);
            }
            final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
            data.write(bytes, 0, bytes.length);
        }

        return data.toByteArray();
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder()
            .addDefaultModules()
            .addModule(new TypeModule())
            .build();

    private static List<Character> DELIMITER_CANDIDATES = Collections.unmodifiableList(Arrays.asList(
            ',',
            '\t',
            '|',
            ';'
    ));

    private static List<Character> QUOTE_CANDIDATES = Collections.unmodifiableList(Arrays.asList(
            '\"',
            '\''
    ));

    private static List<String> ESCAPE_CANDIDATES = Collections.unmodifiableList(Arrays.asList(
            "\\",
            "\""
    ));

    private static List<String> NULL_STRING_CANDIDATES = Collections.unmodifiableList(Arrays.asList(
            "null",
            "NULL",
            "#N/A",
            "\\N"  // MySQL LOAD, Hive STORED AS TEXTFILE
    ));

    private static List<String> COMMENT_LINE_MARKER_CANDIDATES = Collections.unmodifiableList(Arrays.asList(
            "#",
            "//"
    ));

    private static SchemaGuess SCHEMA_GUESS = SchemaGuess.of();

    private static int MAX_SKIP_LINES = 10;
    private static int NO_SKIP_DETECT_LINES = 10;

    private static final Logger logger = LoggerFactory.getLogger(CsvGuessPlugin.class);
}
