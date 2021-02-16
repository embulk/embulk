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

package org.embulk.standards;

import static org.embulk.test.EmbulkTests.copyResource;
import static org.embulk.test.EmbulkTests.readResource;
import static org.embulk.test.EmbulkTests.readSortedFile;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.exec.PartialExecutionException;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

public class TestRemoveColumnsFilterPlugin {
    private static final String RESOURCE_NAME_PREFIX = "org/embulk/standards/remove_columns/test/";

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .registerPlugin(FormatterPlugin.class, "csv", CsvFormatterPlugin.class)
            .registerPlugin(FileInputPlugin.class, "file", LocalFileInputPlugin.class)
            .registerPlugin(FileOutputPlugin.class, "file", LocalFileOutputPlugin.class)
            .registerPlugin(ParserPlugin.class, "csv", CsvParserPlugin.class)
            .registerPlugin(FilterPlugin.class, "remove_columns", RemoveColumnsFilterPlugin.class)
            .build();

    @Test
    public void useKeepOption() throws Exception {
        assertRecordsByResource(embulk, "test_keep_in.yml", "test_keep_filter.yml",
                "test_keep.csv", "test_keep_expected.csv");
    }

    @Test
    public void useKeepWithAcceptUnmatched() throws Exception {
        assertRecordsByResource(embulk, "test_keep_in.yml", "test_keep_with_unmatched_filter.yml",
                "test_keep.csv", "test_keep_expected.csv");
    }

    @Test
    public void useKeepWithoutAcceptUnmatched() throws Exception {
        try {
            assertRecordsByResource(embulk, "test_keep_in.yml", "test_keep_without_unmatched_filter.yml",
                    "test_keep.csv", "test_keep_expected.csv");
            fail();
        } catch (PartialExecutionException ex) {
            assertTrue(ex.getCause() instanceof ConfigException);
        }
    }

    @Test
    public void useKeepWithDuplicatedColumnNames() throws Exception {
        assertRecordsByResource(embulk, "test_keep_with_duplicated_column_names_in.yml", "test_keep_with_duplicated_column_names.yml",
                "test_keep_with_duplicated_column_names.csv", "test_keep_with_duplicated_column_names_expected.csv");
    }

    @Test
    public void useRemove() throws Exception {
        assertRecordsByResource(embulk, "test_remove_in.yml", "test_remove_filter.yml",
                "test_remove.csv", "test_remove_expected.csv");
    }

    @Test
    public void useRemoveWithAcceptUnmatched() throws Exception {
        assertRecordsByResource(embulk, "test_remove_in.yml", "test_remove_with_unmatched_filter.yml",
                "test_remove.csv", "test_remove_expected.csv");
    }

    @Test
    public void useRemoveWithoutAcceptUnmatched() throws Exception {
        try {
            assertRecordsByResource(embulk, "test_remove_in.yml", "test_remove_without_unmatched_filter.yml",
                    "test_remove.csv", "test_remove_expected.csv");
            fail();
        } catch (PartialExecutionException ex) {
            assertTrue(ex.getCause() instanceof ConfigException);
        }
    }

    static void assertRecordsByResource(TestingEmbulk embulk,
            String inConfigYamlResourceName, String filterConfigYamlResourceName,
            String sourceCsvResourceName, String resultCsvResourceName) throws IOException {
        Path inputPath = embulk.createTempFile("csv");
        Path outputPath = embulk.createTempFile("csv");

        // in: config
        copyResource(RESOURCE_NAME_PREFIX + sourceCsvResourceName, inputPath);
        ConfigSource inConfig = embulk.loadYamlResource(RESOURCE_NAME_PREFIX + inConfigYamlResourceName)
                .set("path_prefix", inputPath.toAbsolutePath().toString());

        // remove_columns filter config
        ConfigSource filterConfig = embulk.loadYamlResource(RESOURCE_NAME_PREFIX + filterConfigYamlResourceName);

        TestingEmbulk.RunResult result = embulk.inputBuilder()
                .in(inConfig)
                .filters(ImmutableList.of(filterConfig))
                .outputPath(outputPath)
                .run();

        assertThat(readSortedFile(outputPath), is(readResource(RESOURCE_NAME_PREFIX + resultCsvResourceName)));
    }
}
