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
import org.quickload.channel.BufferOutput;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.Report;
import org.quickload.config.NullReport;
import org.quickload.spi.FileInputPlugin;
import org.quickload.spi.ProcTask;

import javax.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class S3FileInputPlugin
        extends FileInputPlugin
{
    public interface PluginTask extends Task
    {
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
    public TaskSource getFileInputTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = config.loadTask(PluginTask.class);
        proc.setProcessorCount(task.getPaths().size());
        return config.dumpTask(task);
    }

    private AWSCredentials createAWSCredentials(PluginTask task)
    {
        return new BasicAWSCredentials(task.getAccessKeyId(),
                task.getSecretAccessKey());
    }

    private AmazonS3Client createS3Client(PluginTask task)
    {
        AWSCredentials credentials = createAWSCredentials(task);

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);
        clientConfig.setMaxConnections(50);
        clientConfig.setMaxErrorRetry(3);

        AmazonS3Client client = new AmazonS3Client(credentials, clientConfig);
        client.setEndpoint(task.getEndpoint());
        return client;
    }

    @Override
    public Report runFileInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, BufferOutput bufferOutput)
    {
        final PluginTask task = taskSource.loadTask(PluginTask.class);
        final AmazonS3Client client = createS3Client(task);
        String bucket = task.getBucket();
        String key = task.getPaths().get(processorIndex);

        // TODO retry if metadata might be used
        S3Object object = client.getObject(bucket, key);
        ObjectMetadata metadata = object.getObjectMetadata();

        try {
            ByteBuffer buf = ByteBuffer.allocate((int) metadata.getContentLength()); // TODO ad-hoc

            // TODO retry mechanism
            /**
            int len, offset = 0;
            try (InputStream in = new BufferedInputStream(object.getObjectContent())) {
                while ((len = in.read(bytes, offset, bytes.length - offset)) > 0) {
                    offset += len;
                }
                Buffer buffer = new Buffer(bytes);
                bufferOutput.add(buffer);
            }
             */
        } catch (Exception e) {
            e.printStackTrace(); // TODO
        }

        return new NullReport();
    }

    public static class StatefulObjectInputStream
    {
// implemet as function, (int index, inputstream in)
    }
}
