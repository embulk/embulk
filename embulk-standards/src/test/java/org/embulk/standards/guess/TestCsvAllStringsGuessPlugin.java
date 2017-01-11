package org.embulk.standards.guess;

import com.google.common.collect.ImmutableList;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestCsvAllStringsGuessPlugin
{
    private static final String RESOURCE_NAME_PREFIX = "org/embulk/standards/guess/csv_all_strings/test/";

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .build();

    @Test
    public void testSimple()
            throws Exception
    {
        ConfigSource exec = embulk.newConfig()
                .set("guess_plugins", ImmutableList.of("csv_all_strings"))
                .set("exclude_guess_plugins", ImmutableList.of("csv"));

        ConfigDiff guessed =
            embulk.parserBuilder()
            .exec(exec)
            .inputResource(RESOURCE_NAME_PREFIX + "test_simple.csv")
            .guess();

        assertThat(guessed, is((DataSource) embulk.loadYamlResource(RESOURCE_NAME_PREFIX + "test_simple_guessed.yml")));
    }
}
