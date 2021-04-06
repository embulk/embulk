/*
 * Copyright 2017 The Embulk project
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

package org.embulk.standards.preview;

import static org.embulk.test.EmbulkTests.copyResource;
import static org.embulk.test.EmbulkTests.readFile;
import static org.embulk.test.EmbulkTests.readResource;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.ConfigSource;
import org.embulk.exec.PreviewResult;
import org.embulk.formatter.csv.CsvFormatterPlugin;
import org.embulk.input.file.LocalFileInputPlugin;
import org.embulk.output.file.LocalFileOutputPlugin;
import org.embulk.parser.csv.CsvParserPlugin;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

public class TestFilePreview {
    private static final String RESOURCE_NAME_PREFIX = "org/embulk/standards/preview/file/test/";

    private static final EmbulkSystemProperties EMBULK_SYSTEM_PROPERTIES;

    static {
        final Properties properties = new Properties();
        properties.setProperty("default_guess_plugins", "gzip,bzip2,json,csv");
        EMBULK_SYSTEM_PROPERTIES = EmbulkSystemProperties.of(properties);
    }

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .setEmbulkSystemProperties(EMBULK_SYSTEM_PROPERTIES)
            .registerPlugin(FormatterPlugin.class, "csv", CsvFormatterPlugin.class)
            .registerPlugin(FileInputPlugin.class, "file", LocalFileInputPlugin.class)
            .registerPlugin(FileOutputPlugin.class, "file", LocalFileOutputPlugin.class)
            .registerPlugin(ParserPlugin.class, "csv", CsvParserPlugin.class)
            .build();

    @Test
    public void testSimple() throws Exception {
        assertPreviewedRecords(embulk, "test_simple_load.yml", "test_simple.csv", "test_simple_previewed.csv");
    }

    @Test
    public void changePreviewSampleBufferBytes() throws Exception {
        assertPreviewedRecords(embulk, "test_sample_buffer_bytes_load.yml", "test_sample_buffer_bytes_exec.yml",
                "test_sample_buffer_bytes.csv", "test_sample_buffer_bytes_previewed.csv");
    }

    private static void assertPreviewedRecords(TestingEmbulk embulk,
            String loadYamlResourceName, String sourceCsvResourceName, String resultCsvResourceName)
            throws IOException {
        assertPreviewedRecords(embulk, loadYamlResourceName, null, sourceCsvResourceName, resultCsvResourceName);
    }

    private static void assertPreviewedRecords(TestingEmbulk embulk,
            String loadYamlResourceName, String execYamlResourceName, String sourceCsvResourceName, String resultCsvResourceName)
            throws IOException {
        Path inputPath = embulk.createTempFile("csv");
        Path outputPath = embulk.createTempFile("csv");

        // in: config
        copyResource(RESOURCE_NAME_PREFIX + sourceCsvResourceName, inputPath);
        ConfigSource load = embulk.loadYamlResource(RESOURCE_NAME_PREFIX + loadYamlResourceName)
                .set("path_prefix", inputPath.toAbsolutePath().toString());

        // exec: config
        final TestingEmbulk.InputBuilder builder = embulk.inputBuilder();
        if (execYamlResourceName != null) {
            final ConfigSource execConfig = embulk.loadYamlResource(RESOURCE_NAME_PREFIX + execYamlResourceName);
            builder.exec(execConfig);
        }

        // execute preview
        final PreviewResult result = builder.in(load).outputPath(outputPath).preview();

        assertThat(readFile(outputPath), is(readResource(RESOURCE_NAME_PREFIX + resultCsvResourceName)));
    }
}
