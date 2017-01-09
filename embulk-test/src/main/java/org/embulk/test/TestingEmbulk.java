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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedReader;
import static java.util.Locale.ENGLISH;
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

    private static final List<String> SUPPORTED_TYPES = ImmutableList.of(
        "boolean", "long", "double", "string", "timestamp", "json"
    );

    public static interface RunResult
    {
        ConfigDiff getConfigDiff();

        List<Throwable> getIgnoredExceptions();

        Schema getInputSchema();

        Schema getOutputSchema();

        List<TaskReport> getInputTaskReports();

        List<TaskReport> getOutputTaskReports();
    }

    public static ParserBuilder parserBuilder()
    {
        return new ParserBuilder();
    }

    public static class ParserBuilder
    {
        private ConfigSource parserConfig = null;
        private ConfigSource execConfig = null;
        private Path inputPath = null;

        public ParserBuilder parser(ConfigSource parserConfig)
        {
            checkNotNull(parserConfig, "parserConfig");
            this.parserConfig = parserConfig.deepCopy();
            return this;
        }

        public ParserBuilder exec(ConfigSource execConfig)
        {
            checkNotNull(execConfig, "execConfig");
            this.execConfig = execConfig.deepCopy();
            return this;
        }

        public ParserBuilder inputPath(Path inputPath)
        {
            checkNotNull(inputPath, "inputPath");
            this.inputPath = inputPath;
            return this;
        }

        public ConfigDiff guess(TestingEmbulk embulk)
        {
            if (execConfig == null) {
                execConfig = embulk.newConfig();
            }

            // generate in:
            ConfigSource inConfig = embulk.newConfig()
                    .set("type", "file")
                    .set("path_prefix", inputPath.toAbsolutePath().toString());
            if (parserConfig != null) {
                inConfig.set("parser", parserConfig);
            }

            // combine exec:, in:
            ConfigSource config = embulk.newConfig()
                    .set("exec", execConfig)
                    .set("in", inConfig);

            // embed.guess returns GuessExecutor.ConfigDiff
            return embulk.embed.guess(config);
        }
    }

    public ConfigDiff guessParser(Path inputPath)
    {
        return parserBuilder()
                .inputPath(inputPath)
                .guess(this);
    }

    public ConfigDiff guessParser(ConfigSource parserConfig, Path inputPath)
    {
        return parserBuilder()
                .parser(parserConfig)
                .inputPath(inputPath)
                .guess(this);
    }

    public ConfigDiff guessParser(ConfigSource parserConfig, Path inputPath, ConfigSource execConfig)
    {
        return parserBuilder()
                .parser(parserConfig)
                .inputPath(inputPath)
                .exec(execConfig)
                .guess(this);
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

        public ConfigDiff guess(TestingEmbulk embulk)
        {
            checkState(inConfig != null, "in config must be set");
            if (execConfig == null) {
                execConfig = embulk.newConfig();
            }

            // combine exec:, in:
            ConfigSource config = embulk.newConfig()
                    .set("exec", execConfig)
                    .set("in", inConfig);

            // embed.guess returns GuessExecutor.ConfigDiff
            return embulk.embed.guess(config);
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

    public ConfigDiff guessInput(ConfigSource inConfig)
    {
        return inputBuilder()
                .in(inConfig)
                .guess(this);
    }

    public ConfigDiff guessInput(ConfigSource inConfig, ConfigSource execConfig)
    {
        return inputBuilder()
                .exec(execConfig)
                .in(inConfig)
                .guess(this);
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

    public static OutputBuilder outputBuilder()
    {
        return new OutputBuilder();
    }

    public static class OutputBuilder
    {
        private ConfigSource outConfig;
        private ConfigSource execConfig;
        private Path inputPath;
        private SchemaConfig inputSchema;

        public OutputBuilder out(ConfigSource outConfig)
        {
            checkNotNull(outConfig, "outConfig");
            this.outConfig = outConfig;
            return this;
        }

        public OutputBuilder exec(ConfigSource execConfig)
        {
            checkNotNull(execConfig, "execConfig");
            this.execConfig = execConfig;
            return this;
        }

        public OutputBuilder inputPath(Path inputPath)
        {
            checkNotNull(inputPath, "inputPath");
            this.inputPath = inputPath;
            return this;
        }

        public OutputBuilder inputSchema(SchemaConfig inputSchema)
        {
            checkNotNull(inputSchema, "inputSchema");
            this.inputSchema = inputSchema;
            return this;
        }

        public RunResult run(TestingEmbulk embulk)
                throws IOException
        {
            checkState(outConfig != null, "out config must be set");
            checkState(inputPath != null, "inputPath must be set");
            if (execConfig == null) {
                execConfig = embulk.newConfig();
            }

            String fileName = inputPath.toAbsolutePath().toString();
            checkArgument(fileName.endsWith(".csv"), "inputPath must end with .csv");

            // exec: config
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
                    .set("columns", newSchemaConfig(embulk));
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
            String[] tuple = column.split(":", 2);
            checkArgument(tuple.length == 2, "tuple must be a pair of column name and type");
            String type = tuple[1];
            if (!SUPPORTED_TYPES.contains(type)) {
                throw new IllegalArgumentException(String.format(ENGLISH,
                            "Unknown column type %s. Supported types are boolean, long, double, string, timestamp and json: %s",
                            tuple[1], column));
            }
            return new ColumnConfig(embulk.newConfig()
                    .set("name", tuple[0])
                    .set("type", type));
        }
    }

    public RunResult runOutput(ConfigSource outConfig, Path inputPath)
            throws IOException
    {
        return outputBuilder()
                .out(outConfig)
                .inputPath(inputPath)
                .run(this);
    }

    public RunResult runOutput(ConfigSource execConfig, ConfigSource outConfig, Path inputPath)
            throws IOException
    {
        return outputBuilder()
                .exec(execConfig)
                .out(outConfig)
                .inputPath(inputPath)
                .run(this);
    }

    // TODO add runFilter(ConfigSource filterConfig, Path inputPath, Path outputPath) where inputPath is a path to
    // a CSV file whose column types can be naturally guessed using csv guess plugin.
}
