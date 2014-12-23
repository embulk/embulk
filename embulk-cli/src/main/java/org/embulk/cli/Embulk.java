package org.embulk.cli;

import java.util.List;
import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigLoader;
import org.embulk.config.NextConfig;
import org.embulk.spi.Pages;
//import org.embulk.spi.NoticeLogger;
//import org.embulk.exec.EmbulkService;
import org.embulk.config.ModelManager;
import org.embulk.spi.ExecSession;
import org.embulk.exec.LocalExecutor;
import org.embulk.exec.ExecuteResult;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.PreviewExecutor;
import org.embulk.exec.PreviewResult;

public class Embulk
        extends EmbulkService
{
    public static void main(String[] args) throws Exception
    {
        if (args.length == 0) {
            System.out.println("usage: [-Dload.systemConfigKey=value...] <config.yml> [configKey=value...]");
            return;
        }

        // TODO bootstrap model manager
        //ConfigSource systemConfig = ConfigSources.fromPropertiesYamlLiteral(System.getProperties(), "load.");
        ConfigSource systemConfig = new ConfigLoader(new ModelManager(new ObjectMapper())).fromPropertiesYamlLiteral(System.getProperties(), "load.");

        new Embulk(systemConfig).run(args[0]);
    }

    public Embulk(ConfigSource systemConfig)
    {
        super(systemConfig);
    }

    public void run(String configPath) throws Exception
    {
        ConfigLoader configLoader = injector.getInstance(ConfigLoader.class);
        ConfigSource config = configLoader.fromYamlFile(new File(configPath));

        ExecSession exec = injector.getInstance(ExecSession.class);

        // automatic guess
        NextConfig guessed = injector.getInstance(GuessExecutor.class).guess(exec, config);
        config.merge(guessed);
        System.out.println("guessed: "+config);

        PreviewResult preview = injector.getInstance(PreviewExecutor.class).preview(exec, config);
        List<Object[]> records = Pages.toObjects(preview.getSchema(), preview.getPages());
        String previewJson = injector.getInstance(ModelManager.class).writeObject(records);
        System.out.println("preview schema: "+preview.getSchema());
        System.out.println("preview records: "+previewJson);

        LocalExecutor local = injector.getInstance(LocalExecutor.class);
        ExecuteResult result = local.run(exec, config);

        System.out.println("next config: "+result.getNextConfig());

        //System.out.println("notice messages: ");
        //for (NoticeLogger.Message message : result.getNoticeMessages()) {
        //    System.out.println("  "+message);
        //}

        //System.out.println("skipped records: ");
        //for (NoticeLogger.SkippedRecord record : result.getSkippedRecords()) {
        //    System.out.println("  "+record);
        //}
    }
}
