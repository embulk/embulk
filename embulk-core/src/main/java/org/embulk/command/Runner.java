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
import org.embulk.config.NextConfig;
import org.embulk.config.ModelManager;
import org.embulk.config.ConfigException;
import org.embulk.exec.LocalExecutor;
import org.embulk.exec.ExecutionResult;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.PreviewExecutor;
import org.embulk.exec.PreviewResult;
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
        checkNextConfigOutputPath(options.getNextConfigOutputPath());

        ExecSession exec = newExecSession(config);
        LocalExecutor local = injector.getInstance(LocalExecutor.class);
        ExecutionResult result = local.run(exec, config);
        NextConfig nextConfig = result.getNextConfig();

        exec.getLogger(Runner.class).info("next config: {}", nextConfig.toString());
        writeNextConfig(options.getNextConfigOutputPath(), config, nextConfig);
    }

    public void guess(String partialConfigPath)
    {
        ConfigSource config = loadYamlConfig(partialConfigPath);
        checkNextConfigOutputPath(options.getNextConfigOutputPath());

        ExecSession exec = newExecSession(config);
        GuessExecutor guess = injector.getInstance(GuessExecutor.class);
        NextConfig nextConfig = guess.guess(exec, config);

        String yml = writeNextConfig(options.getNextConfigOutputPath(), config, nextConfig);
        System.err.println(yml);
    }

    private void checkNextConfigOutputPath(String path)
    {
        if (path != null) {
            try (FileOutputStream in = new FileOutputStream(path, true)) {
                // open with append mode and do nothing. just check availability of the path to not cause exceptiosn later
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private String writeNextConfig(String path, ConfigSource originalConfig, NextConfig nextConfigDiff)
    {
        String yml = dumpConfigInYaml(originalConfig.merge(nextConfigDiff));
        if (path != null) {
            if (path.equals("-")) {
                System.out.print(yml);
            } else {
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"))) {
                    writer.write(yml);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
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

    private String dumpConfigInYaml(DataSource config)
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
