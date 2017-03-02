package org.embulk.standards.guess;

import com.google.common.collect.ImmutableList;
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
    public void testForAFewRows()
            throws Exception
    {
        // for only 1 row
        assertGuessByResource(embulk,
                "test_1_rows_seed.yml", "test_1_rows.csv",
                "test_1_rows_guessed.yml");

        // for only 1 row with trim needed
        assertGuessByResource(embulk,
                "test_1_rows_with_trim_needed_seed.yml", "test_1_rows_with_trim_needed.csv",
                "test_1_rows_with_trim_needed_guessed.yml");

        // for header and 1 row
        assertGuessByResource(embulk,
                "test_1_rows_and_header_seed.yml", "test_1_rows_and_header.csv",
                "test_1_rows_and_header_guessed.yml");

        // for header and 1 row with trim needed
        assertGuessByResource(embulk,
                "test_1_rows_and_header_with_trim_needed_seed.yml", "test_1_rows_and_header_with_trim_needed.csv",
                "test_1_rows_and_header_with_trim_needed_guessed.yml");

        // for 2 rows
        assertGuessByResource(embulk,
                "test_2_rows_seed.yml", "test_2_rows.csv",
                "test_2_rows_guessed.yml");

        // for header and 2 rows
        assertGuessByResource(embulk,
                "test_2_rows_and_header_seed.yml", "test_2_rows_and_header.csv",
                "test_2_rows_and_header_guessed.yml");
    }

    @Test
    public void testSingleColumnRows()
            throws Exception
    {
        // for 1 single int column row
        assertGuessByResource(embulk,
                "test_1_int_single_column_row_seed.yml", "test_1_int_single_column_row.csv",
                "test_1_int_single_column_row_guessed.yml");

        // for 1 single string column row
        assertGuessByResource(embulk,
                "test_1_string_single_column_row_seed.yml", "test_1_string_single_column_row.csv",
                "test_1_string_single_column_row_guessed.yml");

        // for header and 1 single int column row
        assertGuessByResource(embulk,
                "test_1_int_single_column_row_and_header_seed.yml", "test_1_int_single_column_row_and_header.csv",
                "test_1_int_single_column_row_and_header_guessed.yml");

        // for header and 1 single string column row
        assertGuessByResource(embulk,
                "test_1_string_single_column_row_and_header_seed.yml", "test_1_string_single_column_row_and_header.csv",
                "test_1_string_single_column_row_and_header_guessed.yml");

        // for 2 single int column rows
        assertGuessByResource(embulk,
                "test_2_int_single_column_rows_seed.yml", "test_2_int_single_column_rows.csv",
                "test_2_int_single_column_rows_guessed.yml");

        // for 2 single string column rows
        assertGuessByResource(embulk,
                "test_2_string_single_column_rows_seed.yml", "test_2_string_single_column_rows.csv",
                "test_2_string_single_column_rows_guessed.yml");

        // for header and multiple single int column rows
        assertGuessByResource(embulk,
                "test_int_single_column_with_header_seed.yml", "test_int_single_column_with_header.csv",
                "test_int_single_column_with_header_guessed.yml");

        // for header and multiple single string column rows
        assertGuessByResource(embulk,
                "test_string_single_column_with_header_seed.yml", "test_string_single_column_with_header.csv",
                "test_string_single_column_with_header_guessed.yml");

        // for multiple single int column rows
        assertGuessByResource(embulk,
                "test_int_single_column_seed.yml", "test_int_single_column.csv",
                "test_int_single_column_guessed.yml");

        // for multiple single string column rows
        assertGuessByResource(embulk,
                "test_string_single_column_seed.yml", "test_string_single_column.csv",
                "test_string_single_column_guessed.yml");
    }

    @Test
    public void suggestDelimiter()
            throws Exception
    {
        // comma
        assertGuessByResource(embulk,
                "test_comma_delimiter_seed.yml", "test_comma_delimiter.csv",
                "test_comma_delimiter_guessed.yml");

        // tab
        assertGuessByResource(embulk,
                "test_tab_delimiter_seed.yml", "test_tab_delimiter.csv",
                "test_tab_delimiter_guessed.yml");

        // vertical bar
        assertGuessByResource(embulk,
                "test_vertical_bar_delimiter_seed.yml", "test_vertical_bar_delimiter.csv",
                "test_vertical_bar_delimiter_guessed.yml");

        // semicolon
        assertGuessByResource(embulk,
                "test_semicolon_delimiter_seed.yml", "test_semicolon_delimiter.csv",
                "test_semicolon_delimiter_guessed.yml");
    }

    @Test
    public void suggestQuote()
            throws Exception
    {
        // single quote
        assertGuessByResource(embulk,
                "test_single_quote_seed.yml", "test_single_quote.csv",
                "test_single_quote_guessed.yml");

        // double quote
        assertGuessByResource(embulk,
                "test_double_quote_seed.yml", "test_double_quote.csv",
                "test_double_quote_guessed.yml");

        // nil quote
        assertGuessByResource(embulk,
                "test_nil_quote_seed.yml", "test_nil_quote.csv",
                "test_nil_quote_guessed.yml");
    }

    @Test
    public void suggestEscape()
            throws Exception
    {
        // backslash
        assertGuessByResource(embulk,
                "test_backslash_escape_seed.yml", "test_backslash_escape.csv",
                "test_backslash_escape_guessed.yml");

        // double quote
        assertGuessByResource(embulk,
                "test_double_quote_escape_seed.yml", "test_double_quote_escape.csv",
                "test_double_quote_escape_guessed.yml");
    }

    static void assertGuessByResource(TestingEmbulk embulk, String seedYamlResourceName, String sourceCsvResourceName,
            String resultResourceName)
            throws IOException
    {
        ConfigSource seed = embulk.loadYamlResource(RESOURCE_NAME_PREFIX + seedYamlResourceName);

        ConfigDiff guessed =
            embulk.parserBuilder()
            .parser(seed)
            .exec(embulk.newConfig().set("exclude_guess_plugins", ImmutableList.of("json")))
            .inputResource(RESOURCE_NAME_PREFIX + sourceCsvResourceName)
            .guess();

        assertThat(guessed, is((DataSource) embulk.loadYamlResource(RESOURCE_NAME_PREFIX + resultResourceName)));
    }
}

