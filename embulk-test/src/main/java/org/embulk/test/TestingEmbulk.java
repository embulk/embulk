package org.embulk.test;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.io.BufferedReader;
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
import org.embulk.config.ModelManager;
import org.embulk.config.TaskReport;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;
import org.embulk.spi.TempFileException;
import org.embulk.spi.TempFileSpace;
import org.embulk.spi.type.Type;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedReader;
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

    public static InputBuilder inputBuilder()
    {
        return new InputBuilder();
    }

    public static class InputBuilder
    {
        private ConfigSource inConfig = null;
        private ConfigSource execConfig = null;
        private Path outputPath = null;

        public InputBuilder in(ConfigSource inConfig)
        {
            checkNotNull(inConfig, "inConfig");
            this.inConfig = inConfig.deepCopy();
            return this;
        }

        public InputBuilder exec(ConfigSource execConfig)
        {
            checkNotNull(execConfig, "execConfig");
            this.execConfig = execConfig.deepCopy();
            return this;
        }

        public InputBuilder outputPath(Path outputPath)
        {
            checkNotNull(outputPath, "outputPath");
            this.outputPath = outputPath;
            return this;
        }

        public RunResult run(TestingEmbulk embulk)
                throws IOException
        {
            checkState(inConfig != null, "in config must be set");
            checkState(outputPath != null, "outputPath must be set");
            if (execConfig == null) {
                execConfig = embulk.newConfig();
            }

            String fileName = outputPath.getFileName().toString();
            checkArgument(fileName.endsWith(".csv"), "outputPath must end with .csv");
            Path dir = outputPath.getParent().resolve(fileName.substring(0, fileName.length() - 4));

            Files.createDirectories(dir);

            // exec: config
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
        return inputBuilder()
                .in(inConfig)
                .outputPath(outputPath)
                .run(this);
    }

    public RunResult runInput(ConfigSource inConfig, Path outputPath, ConfigSource execConfig)
            throws IOException
    {
        return inputBuilder()
                .exec(execConfig)
                .in(inConfig)
                .outputPath(outputPath)
                .run(this);
    }

    public static RunOutputBuilder runOutputBuilder()
    {
        return new RunOutputBuilder();
    }

    public static class RunOutputBuilder
    {
        private ConfigSource outConfig;
        private ConfigSource execConfig;
        private Path inputPath;
        private SchemaConfig inputSchema;

        public RunOutputBuilder out(ConfigSource outConfig)
        {
            this.outConfig = outConfig;
            return this;
        }

        public RunOutputBuilder exec(ConfigSource execConfig)
        {
            this.execConfig = execConfig;
            return this;
        }

        public RunOutputBuilder inputPath(Path inputPath)
        {
            this.inputPath = inputPath;
            return this;
        }

        public RunOutputBuilder inputSchema(SchemaConfig inputSchema)
        {
            this.inputSchema = inputSchema;
            return this;
        }

        public RunResult run(TestingEmbulk embulk)
                throws IOException
        {
            String fileName = inputPath.getFileName().toString();
            checkArgument(fileName.endsWith(".csv"), "inputPath must end with .csv");

            // exec: config
            if (execConfig == null) {
                execConfig = embulk.newConfig();
            }
            execConfig.set("min_output_tasks", 1);

            // in: config
            ConfigSource inConfig = embulk.newConfig()
                    .set("type", "file")
                    .set("path_prefix", fileName)
                    .set("parser", newParserConfig(embulk));

            // combine exec:, out: and in:
            ConfigSource config = embulk.newConfig()
                    .set("exec", execConfig)
                    .set("in", inConfig)
                    .set("out", outConfig);

            // embed.run returns TestingBulkLoader.TestingExecutionResult because
            return (RunResult) embulk.embed.run(config);
        }

        private ConfigSource newParserConfig(TestingEmbulk embulk)
        {
            return embulk.newConfig()
                    .set("charset", "UTF-8")
                    .set("newline", "LF")
                    .set("type", "csv")
                    .set("delimiter", ",")
                    .set("quote", "\"")
                    .set("escape", "\"")
                    .set("columns", inputSchema != null ? inputSchema : newSchemaConfig(embulk));
        }

        private SchemaConfig newSchemaConfig(TestingEmbulk embulk)
        {
            ImmutableList.Builder<ColumnConfig> schema = ImmutableList.builder();
            try (BufferedReader reader = newBufferedReader(inputPath, UTF_8)) {
                for (String column : reader.readLine().split(",")) {
                    ColumnConfig columnConfig = newColumnConfig(embulk, column);
                    if (columnConfig != null) {
                        schema.add(columnConfig);
                    }
                }
                return new SchemaConfig(schema.build());
            }
            catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        private ColumnConfig newColumnConfig(TestingEmbulk embulk, String column)
        {
            // TODO need to build column conifg
            String[] tuple = column.split(":");
            checkArgument(tuple.length == 2, "tuple must be a pair of column name and type");
            return new ColumnConfig(embulk.newConfig()
                    .set("name", tuple[0])
                    .set("type", embulk.injector().getInstance(ModelManager.class).readObject(Type.class, tuple[1]))
            );
        }
    }

    public RunResult runOutput(ConfigSource outConfig, Path inputPath)
            throws IOException
    {
        return runOutputBuilder()
                .out(outConfig)
                .inputPath(inputPath)
                .run(this);
    }

    public RunResult runOutput(ConfigSource execConfig, ConfigSource outConfig, Path inputPath)
            throws IOException
    {
        return runOutputBuilder()
                .exec(execConfig)
                .out(outConfig)
                .inputPath(inputPath)
                .run(this);
    }

    // TODO add runFilter(ConfigSource filterConfig, Path inputPath, Path outputPath) where inputPath is a path to
    // a CSV file whose column types can be naturally guessed using csv guess plugin.
}
