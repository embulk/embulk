package org.embulk.command;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ModelManager;
import org.embulk.config.ConfigException;
import org.embulk.exec.LocalExecutor;
import org.embulk.exec.ExecutionResult;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.PreviewExecutor;
import org.embulk.exec.PreviewResult;
import org.embulk.exec.ResumeState;
import org.embulk.exec.PartialExecutionException;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.ExecSession;
import org.embulk.spi.util.Pages;
import org.embulk.EmbulkService;

public class Runner
{
    private static class Options
    {
        private String nextConfigOutputPath;
        public String getNextConfigOutputPath() { return nextConfigOutputPath; }

        private String resumeStatePath;
        public String getResumeStatePath() { return resumeStatePath; }
    }

    private final Options options;
    private final ConfigSource systemConfig;
    private final EmbulkService service;
    private final Injector injector;

    public Runner(String optionJson)
    {
        ModelManager bootstrapModelManager = new ModelManager(null, new ObjectMapper());
        this.options = bootstrapModelManager.readObject(Options.class, optionJson);
        this.systemConfig = new ConfigLoader(bootstrapModelManager).fromPropertiesYamlLiteral(System.getProperties(), "embulk.");
        this.service = new EmbulkService(systemConfig);
        this.injector = service.getInjector();
    }

    public void main(String command, String[] args)
    {
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

    public void run(String configPath)
    {
        ConfigSource config = loadYamlConfig(configPath);
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
        LocalExecutor local = injector.getInstance(LocalExecutor.class);
        ExecutionResult result;
        try {
            if (resume != null) {
                result = local.resume(config, resume);
            } else {
                result = local.run(exec, config);
            }
        } catch (PartialExecutionException partial) {
            if (options.getResumeStatePath() == null) {
                // resume state path is not set. cleanup the transaction
                exec.getLogger(Runner.class).info("Transaction partially failed. Cleaning up the intermediate data. Use -r option to make it resumable.");
                try {
                    local.cleanup(config, partial.getResumeState());
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
        ConfigSource config = loadYamlConfig(configPath);
        ConfigSource resumeConfig = loadYamlConfig(resumePath);
        ResumeState resume = resumeConfig.loadConfig(ResumeState.class);

        ExecSession exec = newExecSession(config);
        LocalExecutor local = injector.getInstance(LocalExecutor.class);
        local.cleanup(config, resume);

        // delete resume file
        boolean dontCare = new File(options.getResumeStatePath()).delete();
    }

    public void guess(String partialConfigPath)
    {
        ConfigSource config = loadYamlConfig(partialConfigPath);
        checkFileWritable(options.getNextConfigOutputPath());

        ExecSession exec = newExecSession(config);
        GuessExecutor guess = injector.getInstance(GuessExecutor.class);
        ConfigDiff configDiff = guess.guess(exec, config);

        String yml = writeNextConfig(options.getNextConfigOutputPath(), config, configDiff);
        System.err.println(yml);
        if (options.getNextConfigOutputPath() == null) {
            System.out.println("Use -o PATH option to write the guessed config file to a file.");
        } else {
            System.out.println("Created "+options.getNextConfigOutputPath());
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
        ConfigSource config = loadYamlConfig(partialConfigPath);
        ExecSession exec = newExecSession(config);
        PreviewExecutor preview = injector.getInstance(PreviewExecutor.class);
        PreviewResult result = preview.preview(exec, config);
        List<Object[]> records = Pages.toObjects(result.getSchema(), result.getPages());
        final ModelManager model = injector.getInstance(ModelManager.class);

        String[] header = new String[result.getSchema().getColumnCount()];
        for (int i=0; i < header.length; i++) {
            header[i] = result.getSchema().getColumnName(i) + ":" + result.getSchema().getColumnType(i);
        }

        TablePrinter printer = new TablePrinter(System.out, header) {
            private NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);

            protected String valueToString(Object obj)
            {
                if (obj instanceof String) {
                    return (String) obj;
                } else if (obj instanceof Number) {
                    if (obj instanceof Integer) {
                        return numberFormat.format(((Integer) obj).longValue());
                    }
                    if (obj instanceof Long) {
                        return numberFormat.format(((Long) obj).longValue());
                    }
                    return obj.toString();
                } else if (obj instanceof Timestamp) {
                    return obj.toString();
                } else {
                    return model.writeObject(obj);
                }
            }
        };

        try {
            for (Object[] record : records) {
                printer.add(record);
            }
            printer.finish();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ConfigSource loadYamlConfig(String yamlPath)
    {
        try {
            return injector.getInstance(ConfigLoader.class).fromYamlFile(new File(yamlPath));

        } catch (IOException ex) {
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
        return new ExecSession(injector, config.getNestedOrSetEmpty("exec"));
    }

    private static class MapType extends HashMap<String, Object> {
        public MapType() { }
    };
}
