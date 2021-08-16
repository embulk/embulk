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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests CsvGuessPlugin.
 */
public class TestCsvGuessPlugin {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Test
    public void testLargeLong() {
        final ConfigDiff actual = guess(
                "9223372036854775806,foo",
                "9223372036854775807,bar");
        final List<Map> columnsActual = (List<Map>) actual.getNested("parser").get(List.class, "columns");
        assertEquals(2, columnsActual.size());
        assertEquals(2, columnsActual.get(0).size());
        assertEquals("c0", columnsActual.get(0).get("name"));
        assertEquals("long", columnsActual.get(0).get("type"));
        assertEquals(2, columnsActual.get(1).size());
        assertEquals("c1", columnsActual.get(1).get("name"));
        assertEquals("string", columnsActual.get(1).get("type"));
    }

    /*
  class TestDelimiter < self
    data(
      "\t" => "\t",
      "," => ",",
      "|" => "|",
    )
    def test_delimiter_detection(delim)
      actual = guess([
        ["1", "foo"].join(delim),
        ["2", "bar"].join(delim),
      ])
      assert_equal delim, actual["parser"]["delimiter"]
    end
  end
     */
    @Test
    public void testDelimiterDetection() {
        final String[] delims = { "\t", ",", "|" };
        for (final String delim : delims) {
            final ConfigDiff actual = guess(
                    "1" + delim + "foo",
                    "2" + delim + "bar");
            assertEquals(delim, actual.getNested("parser").get(String.class, "delimiter"));
        }
    }

    /*
  class TestQuote < self
    data(
      "'" => "'",
      '"' => '"',
      nil => nil,
    )
    def test_quote(quotation)
      actual = guess([
        %w(1 foo).map{|str| %Q(#{quotation}#{str}#{quotation})}.join("\t"),
        %w(2 bar).map{|str| %Q(#{quotation}#{str}#{quotation})}.join("\t"),
      ])
      assert_equal quotation, actual["parser"]["quote"]
    end
  end
    */
    @Test
    public void testQuote() {
        final Map<String, String> quotations = new LinkedHashMap<>();
        quotations.put("'", "'");
        quotations.put("\"", "\"");

        // The original Ruby test (in the comment above) looks expecting guessed["parser"]["quote"] to be nil (null)
        // for non-quote input strings "1\tfoo", and "2\tbar". See "nil => nil" in data() of the original Ruby test.
        // (Note that "#{nil}" gets to "", not like "(nil)", in Ruby.)
        //
        // The test case "nil => nil" was, however, skipped by unit-test in fact unfortunately. On the other hand,
        // CSV guess has returned "\"" in guessed["parser"]["quote"] against "1\tfoo" and "2\tbar" for a long time.
        //
        // For compatibility with the long-living behavior, here tests that non-quote input strings go to "\"".
        quotations.put("", "\"");

        for (final Map.Entry<String, String> entry : quotations.entrySet()) {
            final String quoteInString = entry.getKey();
            final String quoteExpected = entry.getValue();
            final ConfigDiff actual = guess(
                    quoteInString + "1" + quoteInString + "\t" + quoteInString + "foo" + quoteInString,
                    quoteInString + "2" + quoteInString + "\t" + quoteInString + "bar" + quoteInString);
            assertEquals(quoteExpected, actual.getNested("parser").get(String.class, "quote"));
        }
    }

    /*
  class TestEscape < self
    data(
      "\\" => "\\",
      '"' => '"',
    )
    def test_escape(char)
      actual = guess([
        %Q('1'\t'F#{char}'OO'),
        %Q('2'\t'FOOOOOOOO#{char}'OO'),
      ])
      assert_equal char, actual["parser"]["escape"]
    end
  end
    */
    @Test
    public void testEscape() {
        final String[] escapes = { "\\", "\"" };
        for (final String escape : escapes) {
            final ConfigDiff actual = guess(
                    "'1'\t'F" + escape + "'OO'",
                    "'2'\t'FOOOOOOOO" + escape + "'OO'");
            assertEquals(escape, actual.getNested("parser").get(String.class, "escape"));
        }
    }

    /*
    def test_skip_header_lines_one
      actual = guess([
        "col1\tcol2",
        "1\tfoo",
        "2\tbar",
      ])
      assert_equal 1, actual["parser"]["skip_header_lines"]
    end
    */
    @Test
    public void testSkipHeaderLinesOne() {
        final ConfigDiff actual = guess(
                "col1\tcol2",
                "1\tfoo",
                "2\tbar");
        assertEquals(1, (int) actual.getNested("parser").get(int.class, "skip_header_lines"));
    }

    /*
    def test_skip_header_lines_three
      actual = guess([
        "this is a CSV",
        "created for a test",
        "col1\tcol2",
        "1\tfoo",
        "2\tbar",
      ])
      assert_equal 3, actual["parser"]["skip_header_lines"]
    end
    */
    @Test
    public void testSkipHeaderLinesThree() {
        final ConfigDiff actual = guess(
                "this is a CSV",
                "created for a test",
                "col1\tcol2",
                "1\tfoo",
                "2\tbar");
        assertEquals(3, (int) actual.getNested("parser").get(int.class, "skip_header_lines"));
    }

    /*
  class TestNullString < self
    data(
      "\\N" => "\\N",
      "null" => "null",
      "NULL" => "NULL",
      "#N/A" => "#N/A",
      nil => nil,
    )
    def test_null_string(null)
      actual = guess([
        "1\tfoo\t#{null}",
        "2\tbar\t#{null}",
      ])
      assert_equal null, actual["parser"]["null_string"]
    end
  end
     */
    @Test
    public void testNullString() {
        final Map<String, String> nullStrings = new LinkedHashMap<>();
        nullStrings.put("\\N", "\\N");
        nullStrings.put("null", "null");
        nullStrings.put("NULL", "NULL");
        nullStrings.put("#N/A", "#N/A");

        // The test case "nil => nil" in the original Ruby test (in the comment above) was skipped by unit-test.
        // CSV guess has unset guessed["parser"]["null_string"] against input strings without any null-like piece,
        // such as "1\tfoo\t", and "2\tbar\t". (Note that "#{nil}" gets to "", not like "(nil)", in Ruby.)
        nullStrings.put("", null);

        for (final Map.Entry<String, String> entry : nullStrings.entrySet()) {
            final String nullInString = entry.getKey();
            final String nullExpected = entry.getValue();
            final ConfigDiff actual = guess(
                    "1\tfoo\t" + nullInString,
                    "2\tbar\t" + nullInString);
            if (nullExpected == null) {
                assertFalse(actual.getNested("parser").has("null_string"));
            } else {
                assertEquals(nullExpected, actual.getNested("parser").get(String.class, "null_string"));
            }
        }
    }

    /*
    def test_trim_flag_when_will_be_long_if_strip_arround_space
      actual = guess([
        "  1 \tfoo",
        "  2 \tfoo",
        "  3 \tfoo",
      ])
      assert_equal true, actual["parser"]["trim_if_not_quoted"]
    end
    */
    @Test
    public void testTrimFlagWhenWillBeLongIfStripArroundSpace() {
        final ConfigDiff actual = guess(
                "  1 \tfoo",
                "  2 \tfoo",
                "  3 \tfoo");
        assertEquals((Boolean) true, actual.getNested("parser").get(Boolean.class, "trim_if_not_quoted"));
    }

    /*
  class TestCommentLineMarker < self
    data(
      "#" => "#",
      "//" => "//",
    )
    def test_comment_line_marker(marker)
      actual = guess([
        "foo\t 1\tother",
        "#{marker} foo\t 2\tother",
        "foo\t 3\tother",
      ])
      assert_equal marker, actual["parser"]["comment_line_marker"]
    end
  end
    */
    @Test
    public void testCommentLineMarker() {
        final String[] markers = { "#", "//" };
        for (final String marker : markers) {
            final ConfigDiff actual = guess(
                    "foo\t 1\tother",
                    marker + " foo\t 2\tother",
                    "foo\t 3\tother");
            assertEquals(marker, actual.getNested("parser").get(String.class, "comment_line_marker"));
        }
    }

    /*
    def test_columns_without_header
      actual = guess([
        "1\tfoo\t2000-01-01T00:00:00+0900",
        "2\tbar\t2000-01-01T00:00:00+0900",
      ])
      expected = [
        {"name" => "c0", "type" => "long"},
        {"name" => "c1", "type" => "string"},
        {"name" => "c2", "type" => "timestamp", "format"=>"%Y-%m-%dT%H:%M:%S%z"},
      ]
      assert_equal expected, actual["parser"]["columns"]
    end
    */
    @SuppressWarnings("unchecked")
    @Test
    public void testColumnsWithoutHeader() {
        final ConfigDiff actual = guess(
                "1\tfoo\t2000-01-01T00:00:00+0900",
                "2\tbar\t2000-01-01T00:00:00+0900");
        final List<Map> columnsActual = (List<Map>) actual.getNested("parser").get(List.class, "columns");
        assertEquals(3, columnsActual.size());
        assertEquals(2, columnsActual.get(0).size());
        assertEquals("c0", columnsActual.get(0).get("name"));
        assertEquals("long", columnsActual.get(0).get("type"));
        assertEquals(2, columnsActual.get(1).size());
        assertEquals("c1", columnsActual.get(1).get("name"));
        assertEquals("string", columnsActual.get(1).get("type"));
        assertEquals(3, columnsActual.get(2).size());
        assertEquals("c2", columnsActual.get(2).get("name"));
        assertEquals("timestamp", columnsActual.get(2).get("type"));
        assertEquals("%Y-%m-%dT%H:%M:%S%z", columnsActual.get(2).get("format"));
    }

    /*
    def test_columns_with_header
      actual = guess([
        "num\tstr\ttime",
        "1\tfoo\t2000-01-01T00:00:00+0900",
        "2\tbar\t2000-01-01T00:00:00+0900",
      ])
      expected = [
        {"name" => "num", "type" => "long"},
        {"name" => "str", "type" => "string"},
        {"name" => "time", "type" => "timestamp", "format"=>"%Y-%m-%dT%H:%M:%S%z"},
      ]
      assert_equal expected, actual["parser"]["columns"]
    end
    */
    @SuppressWarnings("unchecked")
    @Test
    public void testColumnsWithHeader() {
        final ConfigDiff actual = guess(
                "num\tstr\ttime",
                "1\tfoo\t2000-01-01T00:00:00+0900",
                "2\tbar\t2000-01-01T00:00:00+0900");
        final List<Map> columnsActual = (List<Map>) actual.getNested("parser").get(List.class, "columns");
        assertEquals(3, columnsActual.size());
        assertEquals(2, columnsActual.get(0).size());
        assertEquals("num", columnsActual.get(0).get("name"));
        assertEquals("long", columnsActual.get(0).get("type"));
        assertEquals(2, columnsActual.get(1).size());
        assertEquals("str", columnsActual.get(1).get("name"));
        assertEquals("string", columnsActual.get(1).get("type"));
        assertEquals(3, columnsActual.get(2).size());
        assertEquals("time", columnsActual.get(2).get("name"));
        assertEquals("timestamp", columnsActual.get(2).get("type"));
        assertEquals("%Y-%m-%dT%H:%M:%S%z", columnsActual.get(2).get("format"));
    }

    /*
    def test_complex_line
      actual = guess([
        %Q(this is useless header),
        %Q(and more),
        %Q(num,str,quoted_num,time),
        %Q(1, "value with space "" and quote in it", "123",21150312000000Z),
        %Q(2),
        %Q(# 3, "this is commented out" ,"1",21150312000000Z),
      ])
      expected = [
        {"name" => "num", "type" => "long"},
        {"name" => "str", "type" => "string"},
        {"name" => "quoted_num", "type" => "long"},
        {"name" => "time", "type" => "timestamp", "format"=>"%Y%m%d%H%M%S%z"},
      ]
      assert_equal expected, actual["parser"]["columns"]
    end
    */
    @SuppressWarnings("unchecked")
    @Test
    public void testComplexLine() {
        final ConfigDiff actual = guess(
                "this is useless header",
                "and more",
                "num,str,quoted_num,time",
                "1, \"value with space \"\" and quote in it\", \"123\",21150312000000Z",
                "2",
                "# 3, \"this is commented out\" ,\"1\",21150312000000Z");
        final List<Map> columnsActual = (List<Map>) actual.getNested("parser").get(List.class, "columns");
        assertEquals(4, columnsActual.size());
        assertEquals(2, columnsActual.get(0).size());
        assertEquals("num", columnsActual.get(0).get("name"));
        assertEquals("long", columnsActual.get(0).get("type"));
        assertEquals(2, columnsActual.get(1).size());
        assertEquals("str", columnsActual.get(1).get("name"));
        assertEquals("string", columnsActual.get(1).get("type"));
        assertEquals(2, columnsActual.get(2).size());
        assertEquals("quoted_num", columnsActual.get(2).get("name"));
        assertEquals("long", columnsActual.get(2).get("type"));
        assertEquals(3, columnsActual.get(3).size());
        assertEquals("time", columnsActual.get(3).get("name"));
        assertEquals("timestamp", columnsActual.get(3).get("type"));
        assertEquals("%Y%m%d%H%M%S%z", columnsActual.get(3).get("format"));
    }

    private static ConfigDiff guess(final String... sampleLines) {
        final ConfigSource config = CONFIG_MAPPER_FACTORY.newConfigSource();
        final ConfigSource parserConfig = CONFIG_MAPPER_FACTORY.newConfigSource();
        parserConfig.set("type", "csv");
        config.set("parser", parserConfig);
        return new CsvGuessPlugin().guessLines(config, Arrays.asList(sampleLines));
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();
}
