package org.embulk.standards.guess;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.test.EmbulkTests;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestCsvGuessPlugin
{
    private static final String BASIC_RESOURCE_PATH = "org/embulk/standards/guess/csv/test/";

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .build();

    @Test
    public void testSimple()
            throws Exception
    {
        assertConfigs(embulk, BASIC_RESOURCE_PATH,
                "test_simple_seed.yml", "test_simple_guessed.yml", "test_simple.csv");
    }

    @Test
    public void testTabDelimiter()
            throws Exception
    {
        assertConfigs(embulk, BASIC_RESOURCE_PATH,
                "test_tab_delimiter_seed.yml", "test_tab_delimiter_guessed.yml", "test_tab_delimiter.csv");
    }

    static void assertConfigs(TestingEmbulk embulk, String resourcePath, String seedYamlFile, String guessedYamlFile, String csvFile)
            throws IOException
    {
        Path inputPath = embulk.createTempFile("csv");
        EmbulkTests.copyResource(resourcePath + csvFile, inputPath);

        ConfigSource seed = embulk.loadYamlResource(resourcePath + seedYamlFile);
        ConfigDiff guessed = embulk.guessParser(seed, inputPath);

        assertThat(guessed.getNested("in").getNested("parser"), is((DataSource) embulk.loadYamlResource(resourcePath + guessedYamlFile)));
    }
}

