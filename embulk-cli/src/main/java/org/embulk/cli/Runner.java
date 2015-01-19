package org.embulk.cli;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.io.File;
import java.io.IOException;
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
import org.embulk.spi.ExecSession;
import org.embulk.spi.Pages;
import org.embulk.exec.LocalExecutor;
import org.embulk.exec.ExecuteResult;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.PreviewExecutor;
import org.embulk.exec.PreviewResult;

public class Runner
{
    public static void main(String[] args)
    {
        // TODO Pure-java CLI is not implemented yet.
        //      See lib/embulk/command/embulk.rb
    }

    private final ConfigSource systemConfig;
    private final EmbulkService service;
    private final Injector injector;
    private final ExecSession exec;

    public Runner(String optionJson)
    {
        ModelManager bootstrapModelManager = new ModelManager(null, new ObjectMapper());
        this.systemConfig = new ConfigLoader(bootstrapModelManager).fromPropertiesYamlLiteral(System.getProperties(), "embulk.");
        this.service = new EmbulkService(systemConfig);
        this.injector = service.getInjector();
        this.exec = new ExecSession(injector);
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
        LocalExecutor local = injector.getInstance(LocalExecutor.class);
        ExecuteResult result = local.run(exec, config);

        System.out.println("next config:");
        System.out.println(dumpConfigInYaml(result.getNextConfig()));
    }

    public void guess(String partialConfigPath)
    {
        ConfigSource config = loadYamlConfig(partialConfigPath);
        GuessExecutor guess = injector.getInstance(GuessExecutor.class);
        NextConfig result = guess.guess(exec, config);
        config.merge(result);

        System.out.println(dumpConfigInYaml(config));
    }

    public void preview(String partialConfigPath)
    {
        ConfigSource config = loadYamlConfig(partialConfigPath);
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
                } else {
                    return model.writeObject(obj);
                }
            }
        };

        try {
            for (Object[] record : records) {
                printer.add(record);
            }
            printer.flush();
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

    private static class MapType extends HashMap<String, Object> {
        public MapType() { }
    };
}
