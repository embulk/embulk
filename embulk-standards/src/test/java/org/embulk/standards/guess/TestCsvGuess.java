package org.embulk.standards.guess;

import org.embulk.config.ConfigSource;
import org.embulk.test.EmbulkTests;
import org.embulk.test.TestingEmbulk;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestCsvGuess
{
    private static final String BASIC_RESOURCE_PATH = "org/embulk/standards/guess/csv/test/";

    private static ConfigSource loadYamlResource(TestingEmbulk embulk, String fileName)
    {
        return embulk.loadYamlResource(BASIC_RESOURCE_PATH + fileName);
    }

    private static void copyResource(String fileName, Path inputPath)
            throws IOException
    {
        EmbulkTests.copyResource(BASIC_RESOURCE_PATH + fileName, inputPath);
    }

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .build();

    private ConfigSource baseConfig;

    @Before
    public void setup()
    {
        baseConfig = loadYamlResource(embulk, "common.yml");
    }

    @Test
    public void testSimple()
            throws Exception
    {
        assertConfigs("test_simple_seed.yml", "test_simple_guessed.yml", "test_simple.csv");
    }

    @Test
    public void testTabDelimiter()
            throws Exception
    {
        assertConfigs("test_tab_delimiter_seed.yml", "test_tab_delimiter_guessed.yml", "test_tab_delimiter.csv");
    }

    private void assertConfigs(String seedYamlFile, String guessedYamlFile, String csvFile)
            throws IOException
    {
        Path inputPath = embulk.createTempFile("csv");
        copyResource(csvFile, inputPath);

        ConfigSource seed = baseConfig
                .merge(loadYamlResource(embulk, seedYamlFile))
                .set("path_prefix", inputPath.toAbsolutePath().toString());
        ConfigSource guessed = (ConfigSource) embulk.runGuess(seed);

        assertThat(guessed.getNested("in"), is(loadYamlResource(embulk, guessedYamlFile)));
    }
}

