package org.embulk.standards.guess;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.DataSource;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.embulk.test.EmbulkTests.copyResource;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestCsvGuessPlugin
{
    private static final String RESOURCE_NAME_PREFIX = "org/embulk/standards/guess/csv/test/";

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .build();

    @Test
    public void testSimple()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_simple_seed.yml", "test_simple.csv",
                "test_simple_guessed.yml");
    }

    @Test
    public void testTabDelimiter()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_tab_delimiter_seed.yml", "test_tab_delimiter.csv",
                "test_tab_delimiter_guessed.yml");
    }

    static void assertGuessByResource(TestingEmbulk embulk, String seedYamlResourceName, String sourceCsvResourceName,
            String resultResourceName)
            throws IOException
    {
        Path inputPath = embulk.createTempFile("csv");
        copyResource(RESOURCE_NAME_PREFIX + sourceCsvResourceName, inputPath);

        ConfigSource seed = embulk.loadYamlResource(RESOURCE_NAME_PREFIX + seedYamlResourceName);
        ConfigDiff guessed = embulk.guessParser(seed, inputPath);

        assertThat(guessed.getNested("in").getNested("parser"), is((DataSource) embulk.loadYamlResource(RESOURCE_NAME_PREFIX + resultResourceName)));
    }
}

