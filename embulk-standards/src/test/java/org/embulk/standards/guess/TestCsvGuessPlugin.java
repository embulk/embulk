package org.embulk.standards.guess;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

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
    public void suggestTabAsDelimiter()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_tab_delimiter_seed.yml", "test_tab_delimiter.csv",
                "test_tab_delimiter_guessed.yml");
    }

    @Test
    public void suggestSemicolonAsDelimiter()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_semicolon_delimiter_seed.yml", "test_semicolon_delimiter.csv",
                "test_semicolon_delimiter_guessed.yml");
    }

    @Test
    public void suggestSingleQuoteAsQuote()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_single_quote_seed.yml", "test_single_quote.csv",
                "test_single_quote_guessed.yml");
    }

    @Test
    public void suggestBackslashAsEscape()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_backslash_escape_seed.yml", "test_backslash_escape.csv",
                "test_backslash_escape_guessed.yml");
    }

    static void assertGuessByResource(TestingEmbulk embulk, String seedYamlResourceName, String sourceCsvResourceName,
            String resultResourceName)
            throws IOException
    {
        ConfigSource seed = embulk.loadYamlResource(RESOURCE_NAME_PREFIX + seedYamlResourceName);

        ConfigDiff guessed =
            embulk.parserBuilder()
            .parser(seed)
            .inputResource(RESOURCE_NAME_PREFIX + sourceCsvResourceName)
            .guess();

        assertThat(guessed, is((DataSource) embulk.loadYamlResource(RESOURCE_NAME_PREFIX + resultResourceName)));
    }
}

