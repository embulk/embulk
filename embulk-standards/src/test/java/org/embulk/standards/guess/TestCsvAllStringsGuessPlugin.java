package org.embulk.standards.guess;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Properties;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.guess.bzip2.Bzip2GuessPlugin;
import org.embulk.guess.csv.CsvAllStringsGuessPlugin;
import org.embulk.guess.csv.CsvGuessPlugin;
import org.embulk.guess.gzip.GzipGuessPlugin;
import org.embulk.guess.json.JsonGuessPlugin;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.standards.CsvFormatterPlugin;
import org.embulk.standards.CsvParserPlugin;
import org.embulk.standards.LocalFileInputPlugin;
import org.embulk.standards.LocalFileOutputPlugin;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

public class TestCsvAllStringsGuessPlugin {
    private static final String RESOURCE_NAME_PREFIX = "org/embulk/standards/guess/csv_all_strings/test/";

    private static final EmbulkSystemProperties EMBULK_SYSTEM_PROPERTIES;

    static {
        final Properties properties = new Properties();
        properties.setProperty("default_guess_plugins", "gzip,bzip2,json,csv");
        EMBULK_SYSTEM_PROPERTIES = EmbulkSystemProperties.of(properties);
    }

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .setEmbulkSystemProperties(EMBULK_SYSTEM_PROPERTIES)
            .registerPlugin(FormatterPlugin.class, "csv", CsvFormatterPlugin.class)
            .registerPlugin(FileInputPlugin.class, "file", LocalFileInputPlugin.class)
            .registerPlugin(FileOutputPlugin.class, "file", LocalFileOutputPlugin.class)
            .registerPlugin(ParserPlugin.class, "csv", CsvParserPlugin.class)
            .registerPlugin(GuessPlugin.class, "bzip2", Bzip2GuessPlugin.class)
            .registerPlugin(GuessPlugin.class, "csv", CsvGuessPlugin.class)
            .registerPlugin(GuessPlugin.class, "csv_all_strings", CsvAllStringsGuessPlugin.class)
            .registerPlugin(GuessPlugin.class, "gzip", GzipGuessPlugin.class)
            .registerPlugin(GuessPlugin.class, "json", JsonGuessPlugin.class)
            .build();

    @Test
    public void testSimple() throws Exception {
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
