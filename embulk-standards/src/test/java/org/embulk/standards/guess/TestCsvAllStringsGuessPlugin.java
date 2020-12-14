package org.embulk.standards.guess;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Properties;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
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
