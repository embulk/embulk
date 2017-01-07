package org.embulk.standards.guess;

import com.google.common.collect.ImmutableList;
import org.embulk.config.ConfigSource;
import org.embulk.test.EmbulkTests;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestCsvAllStringsGuessPlugin
{
    private static final String RESOURCE_PATH = "org/embulk/standards/guess/csv_all_strings/test/";

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .build();

    @Test
    public void testSimple()
            throws Exception
    {
        Path inputPath = embulk.createTempFile("csv");
        EmbulkTests.copyResource(RESOURCE_PATH + "test_simple.csv", inputPath);

        ConfigSource in = embulk.newConfig()
                .set("type", "file")
                .merge(embulk.loadYamlResource(RESOURCE_PATH + "test_simple_seed.yml"))
                .set("path_prefix", inputPath.toAbsolutePath().toString());
        ConfigSource exec = embulk.newConfig()
                .set("guess_plugins", ImmutableList.of("csv_all_strings"))
                .set("exclude_guess_plugins", ImmutableList.of("csv"));
        ConfigSource guessed = (ConfigSource) embulk.runGuess(in, exec);

        assertThat(guessed.getNested("in"), is(embulk.loadYamlResource(RESOURCE_PATH + "test_simple_guessed.yml")));
    }
}
