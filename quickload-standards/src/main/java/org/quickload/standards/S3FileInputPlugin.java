package org.quickload.standards;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Function;
import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.plugin.PluginManager;
import org.quickload.spi.BufferOperator;
import org.quickload.spi.DynamicReport;
import org.quickload.spi.FileInputPlugin;
import org.quickload.spi.InputProcessor;
import org.quickload.spi.ProcConfig;
import org.quickload.spi.ProcTask;
import org.quickload.spi.ReportBuilder;

import javax.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;

public class S3FileInputPlugin extends FileInputPlugin {
    @Inject
    public S3FileInputPlugin(PluginManager pluginManager) {
        super(pluginManager);
    }

    public interface PluginTask extends Task {
        @Config("in:endpoint")
        public String getEndpoint();

        @Config("in:access_key_id")
        public String getAccessKeyId();

        @Config("in:secret_access_key")
        public String getSecretAccessKey();

        @Config("in:bucket")
        @NotNull
        public String getBucket();

        @Config("in:paths")
        @NotNull
        public List<String> getPaths();
    }

    @Override
    public TaskSource getFileInputTask(ProcConfig proc, ConfigSource config)
    {
        PluginTask task = config.loadTask(PluginTask.class);
        proc.setProcessorCount(task.getPaths().size());
        return config.dumpTask(task);
    }

    private AmazonS3Client createClient(PluginTask task)
    {
        AWSCredentials credentials = new BasicAWSCredentials(
                task.getAccessKeyId(), task.getSecretAccessKey());
        ClientConfiguration cc = new ClientConfiguration();
        cc.setProtocol(Protocol.HTTP);
        cc.setMaxConnections(50);
        cc.setMaxErrorRetry(3);
        AmazonS3Client c = new AmazonS3Client(credentials, cc);
        c.setEndpoint(task.getEndpoint());
        return c;
    }

    @Override
    public InputProcessor startFileInputProcessor(ProcTask proc,
                                                  TaskSource taskSource, final int processorIndex, final BufferOperator next) {
        final PluginTask task = taskSource.loadTask(PluginTask.class);
        final AmazonS3Client s3Client = createClient(task);
        return ThreadInputProcessor.start(next, new Function<BufferOperator, ReportBuilder>() {
            public ReportBuilder apply(BufferOperator next) {
                return readFile(task, processorIndex, s3Client, next);
            }
        });
    }

    public static ReportBuilder readFile(PluginTask task, int processorIndex,
                                         AmazonS3Client s3Client, BufferOperator next) {

        // TODO ad-hoc
        String key = task.getPaths().get(processorIndex);
        S3Object s3Object = s3Client.getObject(task.getBucket(), key);
        ObjectMetadata metadata = s3Object.getObjectMetadata();
        System.out.println("file len: " + metadata.getContentLength());

        try {
            byte[] bytes = new byte[(int) metadata.getContentLength()]; // TODO ad-hoc

            int len, offset = 0;
            try (InputStream in = new BufferedInputStream(s3Object.getObjectContent())) {
                while ((len = in.read(bytes, offset, bytes.length - offset)) > 0) {
                    offset += len;
                }
                Buffer buffer = new Buffer(bytes);
                next.addBuffer(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO
        }

        return DynamicReport.builder(); // TODO
    }
}