package org.embulk.test;

import com.google.common.io.ByteStreams;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.embulk.EmbulkEmbed;
import org.embulk.config.Config;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigSource;
import org.embulk.exec.ExecutionResult;
import org.embulk.spi.TempFileException;
import org.embulk.spi.TempFileSpace;
import static com.google.common.base.Preconditions.checkArgument;
import static org.embulk.plugin.InjectedPluginSource.registerPluginTo;

public class EmbulkTestingEmbed
{
    // TODO Wrap EmbulkTestingEmbed in EmbulkTestingRule that implements org.junit.rules.TestRule
    // so that EmbulkTestingEmbed.destroy is automatically called.

    public static class Builder
    {
        private List<Module> modules = new ArrayList<>();

        Builder()
        { }

        public <T> Builder registerPlugin(final Class<T> iface, final String name, final Class<?> impl)
        {
            modules.add(new Module() {
                public void configure(Binder binder)
                {
                    registerPluginTo(binder, iface, name, impl);
                }
            });
            return this;
        }

        public EmbulkTestingEmbed build()
        {
            return new EmbulkTestingEmbed(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    private final EmbulkEmbed embed;
    private final TempFileSpace tempFiles;

    EmbulkTestingEmbed(Builder builder)
    {
        this.embed = new EmbulkEmbed.Bootstrap()
            .addModules(builder.modules)
            .initializeCloseable();
        try {
            this.tempFiles = new TempFileSpace(Files.createTempDirectory("embulk-test-temp-").toFile());
        }
        catch (IOException ex) {
            throw new TempFileException(ex);
        }
    }

    public void destroy()
    {
        embed.destroy();
        tempFiles.cleanup();
    }

    public Path createTempFile(String suffix)
    {
        return tempFiles.createTempFile(suffix).toPath();
    }

    public Injector injector()
    {
        return embed.getInjector();
    }

    public ConfigLoader configLoader()
    {
        return embed.newConfigLoader();
    }

    public ConfigSource newConfig()
    {
        return configLoader().newConfigSource();
    }

    public ConfigSource loadYamlResource(String name)
    {
        return configLoader()
            .fromYamlString(EmbulkTests.readResource(name));
    }

    public static class RunResult
    {
        private final ConfigDiff configDiff;
        private final List<Throwable> ignoredExceptions;

        RunResult(ConfigDiff configDiff, List<Throwable> ignoredExceptions)
        {
            this.configDiff = configDiff;
            this.ignoredExceptions = ignoredExceptions;
        }

        public ConfigDiff getConfigDiff()
        {
            return configDiff;
        }

        public List<Throwable> getIgnoredExceptions()
        {
            return ignoredExceptions;
        }

        // TODO add RunResult.getOutputSchema() to let test cases validate schema.
        //      especially for filter plugins.
    }

    public RunResult runInput(ConfigSource inConfig, Path outputPath)
        throws IOException
    {
        String fileName = outputPath.getFileName().toString();
        checkArgument(fileName.endsWith(".csv"), "outputPath must end with .csv");
        Path dir = outputPath.getParent().resolve(fileName.substring(0, fileName.length() - 4));

        Files.createDirectories(dir);

        ConfigSource execConfig = newConfig()
            .set("min_output_tasks", 1);

        ConfigSource outConfig = newConfig()
            .set("type", "file")
            .set("path_prefix", dir.resolve("fragments_").toString())
            .set("file_ext", "csv")
            .set("formatter", newConfig()
                    .set("type", "csv")
                    .set("header_line", false)
                    .set("newline", "LF"));

        ConfigSource config = newConfig()
            .set("exec", execConfig)
            .set("in", inConfig)
            .set("out", outConfig);

        ExecutionResult result = embed.run(config);

        try (OutputStream out = Files.newOutputStream(outputPath)) {
            List<Path> fragments = new ArrayList<Path>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "fragments_*.csv")) {
                for (Path fragment : stream) {
                    fragments.add(fragment);
                }
            }
            Collections.sort(fragments);
            for (Path fragment : fragments) {
                try (InputStream in = Files.newInputStream(fragment)) {
                    ByteStreams.copy(in, out);
                }
            }
        }

        return new RunResult(
            result.getConfigDiff(),
            result.getIgnoredExceptions());
    }

    // TODO add runOutput(ConfigSource outConfig, Path inputPath) where inputPath is a path to a CSV file
    // whose column types can be naturally guessed using csv guess plugin. Callers use EmbulkTests.copyResource
    // to copy a resource file to a temp file before calling it.

    // TODO add runFilter(ConfigSource filterConfig, Path inputPath, Path outputPath) where inputPath is a path to
    // a CSV file whose column types can be naturally guessed using csv guess plugin.
}
