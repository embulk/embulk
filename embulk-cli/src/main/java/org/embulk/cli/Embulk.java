package org.embulk.cli;

import java.util.List;
import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigSources;
import org.embulk.config.NextConfig;
import org.embulk.page.Pages;
import org.embulk.spi.NoticeLogger;
//import org.embulk.exec.EmbulkService;
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

        ConfigSource systemConfig = ConfigSources.fromPropertiesYamlLiteral(System.getProperties(), "load.");

        new Embulk(systemConfig).run(args[0]);
    }

    public Embulk(ConfigSource systemConfig)
    {
        super(systemConfig);
    }

    public void run(String configPath) throws Exception
    {
        ConfigSource config = ConfigSources.fromYamlFile(new File(configPath));

        // automatic guess
        //NextConfig guessed = injector.getInstance(GuessExecutor.class).run(config);
        //config.mergeRecursively(guessed);
        //System.out.println("guessed: "+config);

        //PreviewResult preview = injector.getInstance(PreviewExecutor.class).run(config);
        //List<Object[]> records = Pages.toObjects(preview.getSchema(), preview.getPages());
        //String previewJson = new ObjectMapper().writeValueAsString(records);
        //System.out.println("preview schema: "+preview.getSchema());
        //System.out.println("preview records: "+previewJson);

        LocalExecutor exec = injector.getInstance(LocalExecutor.class);
        ExecuteResult result = exec.run(config);

        System.out.println("next config: "+result.getNextConfig());

        System.out.println("notice messages: ");
        for (NoticeLogger.Message message : result.getNoticeMessages()) {
            System.out.println("  "+message);
        }

        System.out.println("skipped records: ");
        for (NoticeLogger.SkippedRecord record : result.getSkippedRecords()) {
            System.out.println("  "+record);
        }
    }
}
