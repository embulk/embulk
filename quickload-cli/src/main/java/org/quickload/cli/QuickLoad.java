package org.quickload.cli;

import java.util.List;
import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quickload.config.ConfigSource;
import org.quickload.config.ConfigSources;
import org.quickload.config.NextConfig;
import org.quickload.record.Pages;
//import org.quickload.exec.QuickLoadService;
import org.quickload.exec.LocalExecutor;
import org.quickload.exec.ExecuteResult;
import org.quickload.exec.GuessExecutor;
import org.quickload.exec.PreviewExecutor;
import org.quickload.exec.PreviewResult;

public class QuickLoad
        extends QuickLoadService
{
    public static void main(String[] args) throws Exception
    {
        if (args.length == 0) {
            System.out.println("usage: [-Dload.systemConfigKey=value...] <config.yml> [configKey=value...]");
            return;
        }

        ConfigSource systemConfig = ConfigSources.fromPropertiesYamlLiteral(System.getProperties(), "load.");

        new QuickLoad(systemConfig).run(args[0]);
    }

    public QuickLoad(ConfigSource systemConfig)
    {
        super(systemConfig);
    }

    public void run(String configPath) throws Exception
    {
        ConfigSource config = ConfigSources.fromYamlFile(new File(configPath));

        // automatic guess
        NextConfig guessed = injector.getInstance(GuessExecutor.class).run(config);
        System.out.println("guessed: "+guessed);
        config.mergeRecursively(guessed);

        PreviewResult preview = injector.getInstance(PreviewExecutor.class).run(config);
        List<Object[]> records = Pages.toObjects(preview.getSchema(), preview.getPages());
        String previewJson = new ObjectMapper().writeValueAsString(records);
        System.out.println("preview schema: "+preview.getSchema());
        System.out.println("preview records: "+previewJson);

        LocalExecutor exec = injector.getInstance(LocalExecutor.class);
        ExecuteResult result = exec.run(config);

        System.out.println("next config: "+result.getNextConfig());
    }
}

