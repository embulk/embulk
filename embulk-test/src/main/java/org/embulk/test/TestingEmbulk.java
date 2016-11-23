package org.embulk.test;

import com.google.common.collect.ImmutableList;
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
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.spi.Schema;
import org.embulk.spi.TempFileException;
import org.embulk.spi.TempFileSpace;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import static com.google.common.base.Preconditions.checkArgument;
import static org.embulk.plugin.InjectedPluginSource.registerPluginTo;

public class TestingEmbulk
        implements TestRule
{
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

        public TestingEmbulk build()
        {
            return new TestingEmbulk(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    private final List<Module> modules;

    private EmbulkEmbed embed;
    private TempFileSpace tempFiles;

    TestingEmbulk(Builder builder)
    {
        this.modules = ImmutableList.copyOf(builder.modules);
        reset();
    }

    public void reset()
    {
        destroy();

        this.embed = new EmbulkEmbed.Bootstrap()
            .addModules(modules)
            .overrideModules(TestingBulkLoader.override())
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
        if (embed != null) {
            embed.destroy();
            embed = null;
        }
        if (tempFiles != null) {
            tempFiles.cleanup();
            tempFiles = null;
        }
    }

    @Override
    public Statement apply(Statement base, Description description)
    {
        return new EmbulkTestingEmbedWatcher().apply(base, description);
    }

    private class EmbulkTestingEmbedWatcher
            extends TestWatcher
    {
        @Override
        protected void starting(Description description)
        {
            reset();
        }

        @Override
        protected void finished(Description description)
        {
            destroy();
        }
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

    public static interface RunResult
    {
        ConfigDiff getConfigDiff();

        List<Throwable> getIgnoredExceptions();

        Schema getInputSchema();

        Schema getOutputSchema();

        List<TaskReport> getInputTaskReports();

        List<TaskReport> getOutputTaskReports();
    }

    public static RunInputBuilder runInputBuilder()
    {
        return new RunInputBuilder();
    }

    public static class RunInputBuilder
    {
        private ConfigSource inConfig;
        private ConfigSource execConfig;
        private Path outputPath;

        public RunInputBuilder in(ConfigSource inConfig)
        {
            this.inConfig = inConfig;
            return this;
        }

        public RunInputBuilder exec(ConfigSource execConfig)
        {
            this.execConfig = execConfig;
            return this;
        }

        public RunInputBuilder outputPath(Path outputPath)
        {
            this.outputPath = outputPath;
            return this;
        }

        public RunResult run(TestingEmbulk embulk)
                throws IOException
        {
            String fileName = outputPath.getFileName().toString();
            checkArgument(fileName.endsWith(".csv"), "outputPath must end with .csv");
            Path dir = outputPath.getParent().resolve(fileName.substring(0, fileName.length() - 4));

            Files.createDirectories(dir);

            // exec: config
            if (execConfig == null) {
                execConfig = embulk.newConfig();
            }
            execConfig.set("min_output_tasks", 1);

            // out: config
            ConfigSource outConfig = embulk.newConfig()
                    .set("type", "file")
                    .set("path_prefix", dir.resolve("fragments_").toString())
                    .set("file_ext", "csv")
                    .set("formatter", embulk.newConfig()
                            .set("type", "csv")
                            .set("header_line", false)
                            .set("newline", "LF"));

            // combine exec:, out: and in:
            ConfigSource config = embulk.newConfig()
                    .set("exec", execConfig)
                    .set("in", inConfig)
                    .set("out", outConfig);

            // embed.run returns TestingBulkLoader.TestingExecutionResult because
            RunResult result = (RunResult) embulk.embed.run(config);

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

            return result;
        }
    }

    public RunResult runInput(ConfigSource inConfig, Path outputPath)
        throws IOException
    {
        return runInputBuilder()
                .in(inConfig)
                .outputPath(outputPath)
                .run(this);
    }

    // TODO add runOutput(ConfigSource outConfig, Path inputPath) where inputPath is a path to a CSV file
    // whose column types can be naturally guessed using csv guess plugin. Callers use EmbulkTests.copyResource
    // to copy a resource file to a temp file before calling it.

    // TODO add runFilter(ConfigSource filterConfig, Path inputPath, Path outputPath) where inputPath is a path to
    // a CSV file whose column types can be naturally guessed using csv guess plugin.
}
