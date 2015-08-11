package org.embulk.command;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Injector;
import org.jruby.embed.ScriptingContainer;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ModelManager;
import org.embulk.config.ConfigException;
import org.embulk.plugin.PluginType;
import org.embulk.exec.BulkLoader;
import org.embulk.exec.ExecutionResult;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.PreviewExecutor;
import org.embulk.exec.PreviewResult;
import org.embulk.exec.ResumeState;
import org.embulk.exec.PartialExecutionException;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.ExecSession;
import org.embulk.EmbulkEmbed;

public class Runner
{
    private static class Options
    {
        private String nextConfigOutputPath;
        public String getNextConfigOutputPath() { return nextConfigOutputPath; }

        private String resumeStatePath;
        public String getResumeStatePath() { return resumeStatePath; }

        private String logLevel;
        public String getLogLevel() { return logLevel; }

        private String previewOutputFormat;
        public String getPreviewOutputFormat() { return previewOutputFormat; };

        private List<PluginType> guessPlugins;
        public List<PluginType> getGuessPlugins() { return guessPlugins; }

        private boolean useGlobalRubyRuntime;
        public boolean getUseGlobalRubyRuntime() { return useGlobalRubyRuntime; }

        private Map<String, String> systemProperty;
        public Map<String, String> getSystemProperty() { return systemProperty; }
    }

    private final Options options;
    private final ConfigSource systemConfig;

    private EmbulkEmbed embed;
    private Injector injector;

    public Runner(String optionJson)
    {
        ModelManager bootstrapModelManager = new ModelManager(null, new ObjectMapper());
        this.options = bootstrapModelManager.readObject(Options.class, optionJson);

        ConfigLoader configLoader = new ConfigLoader(bootstrapModelManager);
        ConfigSource systemConfig = configLoader.fromPropertiesYamlLiteral(System.getProperties(), "embulk.");
        mergeOptionsToSystemConfig(options, configLoader, systemConfig);

        this.systemConfig = systemConfig;
    }

    @SuppressWarnings("unchecked")
    private static void mergeOptionsToSystemConfig(Options options, ConfigLoader configLoader, ConfigSource systemConfig)
    {
        systemConfig.merge(configLoader.fromPropertiesYamlLiteral(options.getSystemProperty(), ""));

        String logLevel = options.getLogLevel();
        if (logLevel != null) {
            // used by LoggerProvider
            systemConfig.set("log_level", logLevel);
        }

        List<PluginType> guessPlugins = options.getGuessPlugins();
        if (guessPlugins != null && !guessPlugins.isEmpty()) {
            // used by GuessExecutor
            List<PluginType> list = new ArrayList<PluginType>() { };
            list = systemConfig.get((Class<List<PluginType>>) list.getClass(), "guess_plugins", list);
            list.addAll(guessPlugins);
            systemConfig.set("guess_plugins", list);
        }

        if (options.getUseGlobalRubyRuntime()) {
            // used by JRubyScriptingModule
            systemConfig.set("use_global_ruby_runtime", true);
        }
    }

    public void main(String command, String[] args)
    {
        try (EmbulkEmbed embed = new EmbulkEmbed(systemConfig)) {
            this.injector = embed.getInjector();

            switch (command) {
            case "run":
                run(args[0]);
                break;
            case "cleanup":
                cleanup(args[0]);
                break;
            case "guess":
                guess(args[0]);
                break;
            case "preview":
                preview(args[0]);
                break;
            default:
                throw new RuntimeException("Unsupported command: "+command);
            }
        }
    }

    public void run(String configPath)
    {
        ConfigSource config = loadConfig(configPath);
        checkFileWritable(options.getNextConfigOutputPath());
        checkFileWritable(options.getResumeStatePath());

        // load resume state file
        ResumeState resume = null;
        String resumePath = options.getResumeStatePath();
        if (resumePath != null) {
            ConfigSource resumeConfig = null;
            try {
                resumeConfig = loadYamlConfig(resumePath);
                if (resumeConfig.isEmpty()) {
                    resumeConfig = null;
                }
            } catch (RuntimeException ex) {
                // leave resumeConfig == null
            }
            if (resumeConfig != null) {
                resume = resumeConfig.loadConfig(ResumeState.class);
            }
        }

        ExecSession exec = newExecSession(config);
        BulkLoader loader = injector.getInstance(BulkLoader.class);
        ExecutionResult result;
        try {
            if (resume != null) {
                // exec is not used here
                result = loader.resume(config, resume);
            } else {
                result = loader.run(exec, config);
            }
        } catch (PartialExecutionException partial) {
            if (options.getResumeStatePath() == null) {
                // resume state path is not set. cleanup the transaction
                exec.getLogger(Runner.class).info("Transaction partially failed. Cleaning up the intermediate data. Use -r option to make it resumable.");
                try {
                    loader.cleanup(config, partial.getResumeState());
                } catch (Throwable ex) {
                    partial.addSuppressed(ex);
                }
                try {
                    exec.cleanup();
                } catch (Throwable ex) {
                    partial.addSuppressed(ex);
                }
                throw partial;
            }
            // save the resume state
            exec.getLogger(Runner.class).info("Writing resume state to '{}'", options.getResumeStatePath());
            writeYaml(options.getResumeStatePath(), partial.getResumeState());
            exec.getLogger(Runner.class).info("Resume state is written. Run the transaction again with -r option to resume or use \"cleanup\" subcommand to delete intermediate data.");
            throw partial;
        }

        // delete resume file
        if (options.getResumeStatePath() != null) {
            boolean dontCare = new File(options.getResumeStatePath()).delete();
        }
        exec.cleanup();

        // write next config
        ConfigDiff configDiff = result.getConfigDiff();
        exec.getLogger(Runner.class).info("Committed.");
        exec.getLogger(Runner.class).info("Next config diff: {}", configDiff.toString());
        writeNextConfig(options.getNextConfigOutputPath(), config, configDiff);
    }

    public void cleanup(String configPath)
    {
        String resumePath = options.getResumeStatePath();
        if (resumePath == null) {
            throw new IllegalArgumentException("Resume path is required for cleanup");
        }
        ConfigSource config = loadConfig(configPath);
        ConfigSource resumeConfig = loadYamlConfig(resumePath);
        ResumeState resume = resumeConfig.loadConfig(ResumeState.class);

        BulkLoader loader = injector.getInstance(BulkLoader.class);
        loader.cleanup(config, resume);

        // delete resume file
        boolean dontCare = new File(options.getResumeStatePath()).delete();
    }

    public void guess(String partialConfigPath)
    {
        ConfigSource config = loadConfig(partialConfigPath);
        checkFileWritable(options.getNextConfigOutputPath());

        ConfigDiff configDiff;
        {
            ExecSession exec = newExecSession(config);
            try {
                GuessExecutor guess = injector.getInstance(GuessExecutor.class);
                configDiff = guess.guess(exec, config);
            } finally {
                exec.cleanup();
            }
        }

        String yml = writeNextConfig(options.getNextConfigOutputPath(), config, configDiff);
        System.err.println(yml);
        if (options.getNextConfigOutputPath() == null) {
            System.out.println("Use -o PATH option to write the guessed config file to a file.");
        } else {
            System.out.println("Created '"+options.getNextConfigOutputPath()+"' file.");
        }
    }

    private void checkFileWritable(String path)
    {
        if (path != null) {
            try (FileOutputStream in = new FileOutputStream(path, true)) {
                // open with append mode and do nothing. just check availability of the path to not cause exceptiosn later
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private String writeNextConfig(String path, ConfigSource originalConfig, ConfigDiff configDiff)
    {
        return writeYaml(path, originalConfig.merge(configDiff));
    }

    private String writeYaml(String path, Object obj)
    {
        String yml = dumpYaml(obj);
        if (path != null) {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"))) {
                writer.write(yml);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return yml;
    }

    public void preview(String partialConfigPath)
    {
        PreviewResult result;
        {
            ConfigSource config = loadConfig(partialConfigPath);
            ExecSession exec = newExecSession(config);
            try {
                PreviewExecutor preview = injector.getInstance(PreviewExecutor.class);
                result = preview.preview(exec, config);
            } finally {
                exec.cleanup();
            }
        }
        ModelManager modelManager = injector.getInstance(ModelManager.class);

        PreviewPrinter printer;

        String format = options.getPreviewOutputFormat();
        if (format == null) {
            format = "table";
        }
        switch (format) {
        case "table":
            printer = new TablePreviewPrinter(System.out, modelManager, result.getSchema());
            break;
        case "vertical":
            printer = new VerticalPreviewPrinter(System.out, modelManager, result.getSchema());
            break;
        default:
            throw new IllegalArgumentException(String.format("Unknown preview output format '%s'. Supported formats: table, vertical", format));
        }

        try {
            printer.printAllPages(result.getPages());
            printer.finish();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ConfigSource loadConfig(String path)
    {
        if (path.endsWith(".yml") || path.endsWith(".yaml")) {
            return loadYamlConfig(path);
        }
        else if (path.endsWith(".yml.liquid") || path.endsWith(".yaml.liquid")) {
            return loadLiquidYamlConfig(path);
        }
        else {
            throw new ConfigException("Unknown file extension. Supported file extentions are .yml and .yml.liquid: "+path);
        }
    }

    private ConfigSource loadYamlConfig(String path)
    {
        try {
            return injector.getInstance(ConfigLoader.class).fromYamlFile(new File(path));
        }
        catch (IOException ex) {
            throw new ConfigException(ex);
        }
    }

    private ConfigSource loadLiquidYamlConfig(String path)
    {
        LiquidTemplate helper = (LiquidTemplate) injector.getInstance(ScriptingContainer.class).runScriptlet("Embulk::Java::LiquidTemplateHelper.new");
        try {
            String source = Files.toString(new File(path), StandardCharsets.UTF_8);
            String data = helper.render(source, ImmutableMap.<String,String>of());
            try (ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
                return injector.getInstance(ConfigLoader.class).fromYaml(in);
            }
        }
        catch (IOException ex) {
            throw new ConfigException(ex);
        }
    }

    private String dumpYaml(Object config)
    {
        ModelManager model = injector.getInstance(ModelManager.class);
        Map<String, Object> map = model.readObject(MapType.class, model.writeObject(config));
        return new Yaml().dump(map);
    }

    private ExecSession newExecSession(ConfigSource config)
    {
        ConfigSource execConfig = config.deepCopy().getNestedOrSetEmpty("exec");
        return ExecSession.builder(injector).fromExecConfig(execConfig).build();
    }

    private static class MapType extends HashMap<String, Object> {
        public MapType() { }
    };
}
