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
    public void testFor1Rows()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_1_rows_seed.yml", "test_1_rows.csv",
                "test_1_rows_guessed.yml");
    }

    @Test
    public void testFor1RowsWithTrimNeeded()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_1_rows_with_trim_needed_seed.yml", "test_1_rows_with_trim_needed.csv",
                "test_1_rows_with_trim_needed_guessed.yml");
    }

    @Test
    public void testFor1RowsAndHeader()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_1_rows_and_header_seed.yml", "test_1_rows_and_header.csv",
                "test_1_rows_and_header_guessed.yml");
    }

    @Test
    public void testFor1RowsAndHeaderWithTrimNeeded()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_1_rows_and_header_with_trim_needed_seed.yml", "test_1_rows_and_header_with_trim_needed.csv",
                "test_1_rows_and_header_with_trim_needed_guessed.yml");
    }

    @Test
    public void testFor2Rows()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_2_rows_seed.yml", "test_2_rows.csv",
                "test_2_rows_guessed.yml");
    }

    @Test
    public void testFor2RowsAndHeader()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_2_rows_and_header_seed.yml", "test_2_rows_and_header.csv",
                "test_2_rows_and_header_guessed.yml");
    }

    @Test
    public void testFor1IntSingleColumnRow()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_1_int_single_column_row_seed.yml", "test_1_int_single_column_row.csv",
                "test_1_int_single_column_row_guessed.yml");
    }

    @Test
    public void testFor1StringSingleColumnRow()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_1_string_single_column_row_seed.yml", "test_1_string_single_column_row.csv",
                "test_1_string_single_column_row_guessed.yml");
    }

    @Test
    public void testFor2StringSingleColumnRows()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_2_string_single_column_rows_seed.yml", "test_2_string_single_column_rows.csv",
                "test_2_string_single_column_rows_guessed.yml");
    }

    @Test
    public void testFor1StringSingleColumnAndHeader()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_1_string_single_column_row_and_header_seed.yml", "test_1_string_single_column_row_and_header.csv",
                "test_1_string_single_column_row_and_header_guessed.yml");
    }

    @Test
    public void testFor2IntSingleColumnRows()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_2_int_single_column_rows_seed.yml", "test_2_int_single_column_rows.csv",
                "test_2_int_single_column_rows_guessed.yml");
    }

    @Test
    public void testFor1IntSingleColumnAndHeader()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_1_int_single_column_row_and_header_seed.yml", "test_1_int_single_column_row_and_header.csv",
                "test_1_int_single_column_row_and_header_guessed.yml");
    }

    @Test
    public void testIntSingleColumnWithHeader()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_int_single_column_with_header_seed.yml", "test_int_single_column_with_header.csv",
                "test_int_single_column_with_header_guessed.yml");
    }

    @Test
    public void testIntSingleColumn()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_int_single_column_seed.yml", "test_int_single_column.csv",
                "test_int_single_column_guessed.yml");
    }

    @Test
    public void testStringSingleColumnWithHeader()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_string_single_column_with_header_seed.yml", "test_string_single_column_with_header.csv",
                "test_string_single_column_with_header_guessed.yml");
    }

    @Test
    public void testStringSingleColumn()
            throws Exception
    {
        assertGuessByResource(embulk,
                "test_string_single_column_seed.yml", "test_string_single_column.csv",
                "test_string_single_column_guessed.yml");
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
            .exec(embulk.newConfig().set("exclude_guess_plugins", ImmutableList.of("json")))
            .inputResource(RESOURCE_NAME_PREFIX + sourceCsvResourceName)
            .guess();

        assertThat(guessed, is((DataSource) embulk.loadYamlResource(RESOURCE_NAME_PREFIX + resultResourceName)));
    }
}

