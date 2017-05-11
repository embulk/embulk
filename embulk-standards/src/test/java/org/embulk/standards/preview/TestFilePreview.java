package org.embulk.standards.preview;

import org.embulk.config.ConfigSource;
import org.embulk.exec.PreviewResult;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.embulk.test.EmbulkTests.copyResource;
import static org.embulk.test.EmbulkTests.readFile;
import static org.embulk.test.EmbulkTests.readResource;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestFilePreview
{
    private static final String RESOURCE_NAME_PREFIX = "org/embulk/standards/preview/file/test/";

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .build();

    @Test
    public void testSimple()
            throws Exception
    {
        assertPreviewedRecords(embulk, "test_simple_load.yml", "test_simple.csv", "test_simple_previewed.csv");
    }

    @Test
    public void changePreviewSampleBufferBytes()
            throws Exception
    {
        assertPreviewedRecords(embulk, "test_sample_buffer_bytes_load.yml", "test_sample_buffer_bytes_exec.yml",
                "test_sample_buffer_bytes.csv", "test_sample_buffer_bytes_previewed.csv");
    }

    private static void assertPreviewedRecords(TestingEmbulk embulk,
            String loadYamlResourceName, String sourceCsvResourceName, String resultCsvResourceName)
            throws IOException
    {
        assertPreviewedRecords(embulk, loadYamlResourceName, null, sourceCsvResourceName, resultCsvResourceName);
    }

    private static void assertPreviewedRecords(TestingEmbulk embulk,
            String loadYamlResourceName, String execYamlResourceName, String sourceCsvResourceName, String resultCsvResourceName)
            throws IOException
    {
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

