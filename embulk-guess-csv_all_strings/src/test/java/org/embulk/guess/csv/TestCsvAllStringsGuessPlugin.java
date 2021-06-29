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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests CsvAllStringsGuessPlugin.
 */
public class TestCsvAllStringsGuessPlugin {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    /*
    def test_columns_without_header
      actual = guess([
        "1\tfoo\t2000-01-01T00:00:00+0900",
        "2\tbar\t2000-01-01T00:00:00+0900",
      ])
      expected = [
        {"name" => "c0", "type" => "string"},
        {"name" => "c1", "type" => "string"},
        {"name" => "c2", "type" => "string"},
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
        assertEquals("string", columnsActual.get(0).get("type"));
        assertEquals(2, columnsActual.get(1).size());
        assertEquals("c1", columnsActual.get(1).get("name"));
        assertEquals("string", columnsActual.get(1).get("type"));
        assertEquals(2, columnsActual.get(2).size());
        assertEquals("c2", columnsActual.get(2).get("name"));
        assertEquals("string", columnsActual.get(2).get("type"));
    }

    /*
    def test_columns_with_header
      actual = guess([
        "num\tstr\ttime",
        "1\tfoo\t2000-01-01T00:00:00+0900",
        "2\tbar\t2000-01-01T00:00:00+0900",
      ])
      expected = [
        {"name" => "num", "type" => "string"},
        {"name" => "str", "type" => "string"},
        {"name" => "time", "type" => "string"},
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
        assertEquals("string", columnsActual.get(0).get("type"));
        assertEquals(2, columnsActual.get(1).size());
        assertEquals("str", columnsActual.get(1).get("name"));
        assertEquals("string", columnsActual.get(1).get("type"));
        assertEquals(2, columnsActual.get(2).size());
        assertEquals("time", columnsActual.get(2).get("name"));
        assertEquals("string", columnsActual.get(2).get("type"));
    }

    private static ConfigDiff guess(final String... sampleLines) {
        final ConfigSource config = CONFIG_MAPPER_FACTORY.newConfigSource();
        final ConfigSource parserConfig = CONFIG_MAPPER_FACTORY.newConfigSource();
        parserConfig.set("type", "csv");
        config.set("parser", parserConfig);
        return new CsvAllStringsGuessPlugin().guessLines(config, Arrays.asList(sampleLines));
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();
}
