/*
 * Copyright 2014 The Embulk project
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

package org.embulk.parser.csv;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.Charset;
import java.util.Optional;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.text.Newline;
import org.junit.Rule;
import org.junit.Test;

public class TestCsvParserPlugin {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();

    @Test
    public void checkDefaultValues() {
        ConfigSource config = CONFIG_MAPPER_FACTORY.newConfigSource()
                .set("columns", ImmutableList.of(
                        ImmutableMap.of(
                            "name", "date_code",
                            "type", "string"))
                    );

        CsvParserPlugin.PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, CsvParserPlugin.PluginTask.class);
        assertEquals(Charset.forName("utf-8"), task.getCharset());
        assertEquals(Newline.CRLF, task.getNewline());
        assertEquals(false, task.getHeaderLine().orElse(false));
        assertEquals(",", task.getDelimiter());
        assertEquals(Optional.of(new CsvParserPlugin.QuoteCharacter('\"')), task.getQuoteChar());
        assertEquals(false, task.getAllowOptionalColumns());
        assertEquals("UTC", task.getDefaultTimeZoneId());
        assertEquals("%Y-%m-%d %H:%M:%S.%N %z", task.getDefaultTimestampFormat());
    }

    @Test(expected = ConfigException.class)
    public void checkColumnsRequired() {
        ConfigSource config = CONFIG_MAPPER_FACTORY.newConfigSource();

        CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, CsvParserPlugin.PluginTask.class);
    }

    @Test
    public void checkLoadConfig() {
        ConfigSource config = CONFIG_MAPPER_FACTORY.newConfigSource()
                .set("charset", "utf-16")
                .set("newline", "LF")
                .set("header_line", true)
                .set("delimiter", "\t")
                .set("quote", "\\")
                .set("allow_optional_columns", true)
                .set("columns", ImmutableList.of(
                            ImmutableMap.of(
                                "name", "date_code",
                                "type", "string"))
                        );

        CsvParserPlugin.PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, CsvParserPlugin.PluginTask.class);
        assertEquals(Charset.forName("utf-16"), task.getCharset());
        assertEquals(Newline.LF, task.getNewline());
        assertEquals(true, task.getHeaderLine().orElse(false));
        assertEquals("\t", task.getDelimiter());
        assertEquals(Optional.of(new CsvParserPlugin.QuoteCharacter('\\')), task.getQuoteChar());
        assertEquals(true, task.getAllowOptionalColumns());
    }
}
