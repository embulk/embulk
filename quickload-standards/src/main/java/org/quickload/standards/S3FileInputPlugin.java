package org.quickload.standards;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Function;
import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.buffer.BufferAllocator;
import org.quickload.channel.FileBufferOutput;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.spi.FileInputPlugin;
import org.quickload.spi.ProcTask;
import org.quickload.spi.ProcControl;

import javax.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.IOException;
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

        @Config("in:access_key_id")@NotNull
        public String getAccessKeyId();

        @Config("in:secret_access_key")@NotNull
        public String getSecretAccessKey();

        @Config("in:bucket")@NotNull
        public String getBucket();

        @Config("in:paths")@NotNull
        public List<String> getPaths();
    }

    @Override
    public NextConfig runFileInputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control)
    {
        PluginTask task = proc.loadConfig(config, PluginTask.class);
        proc.setProcessorCount(task.getPaths().size());

        control.run(proc.dumpTask(task));

        return new NextConfig();
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
        clientConfig.setMaxConnections(50); // SDK default: 50
        clientConfig.setMaxErrorRetry(3); // SDK default: 3
        clientConfig.setSocketTimeout(8*60*1000); // SDK default: 50*1000

        AmazonS3Client client = new AmazonS3Client(credentials, clientConfig);
        if (task.getEndpoint() != null) {
            client.setEndpoint(task.getEndpoint());
        }
        return client;
    }

    @Override
    public Report runFileInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, FileBufferOutput fileBufferOutput)
    {
        final PluginTask task = proc.loadTask(taskSource, PluginTask.class);
        final AmazonS3Client client = createS3Client(task);
        final String bucket = task.getBucket();
        final String key = task.getPaths().get(processorIndex);
        BufferAllocator bufferAllocator = proc.getBufferAllocator();

        // TODO retry if metadata might be used
        long contentLength = client.getObjectMetadata(bucket, key).getContentLength();
        Buffer buf = bufferAllocator.allocateBuffer(1024);

        Opener opener = new Opener(client, bucket, key, contentLength);
        while (true) {
            try (InputStream in = new BufferedInputStream(opener.open(buf.limit()))) {
                int len;
                while ((len = in.read(buf.get(), buf.limit(), buf.capacity() - buf.limit())) >= 0) {
                    buf.limit(buf.limit() + len);
                    if (buf.capacity() - buf.limit() < 1024) {
                        fileBufferOutput.add(buf);
                        buf = bufferAllocator.allocateBuffer(1024);
                    }
                }
                if (buf.limit() > 0) {
                    fileBufferOutput.add(buf);
                }

                break;
            } catch (RuntimeException e) {
                if (e instanceof AmazonServiceException) {
                    AmazonServiceException ase = (AmazonServiceException) e;
                    // TODO retry mechanism
                }
            } catch (IOException e) {
                // TODO
            }

            // TODO retry wait and retry limit
        }
        fileBufferOutput.addFile();

        return new Report();
    }

    public static class Opener
    {
        private AmazonS3Client client;
        private String bucket;
        private String key;
        private long contentLength;

        public Opener(AmazonS3Client client, String bucket, String key, long contentLength)
        {
            this.client = client;
            this.bucket = bucket;
            this.key = key;
            this.contentLength = contentLength;
        }

        public InputStream open(long pos)
        {
            GetObjectRequest request = new GetObjectRequest(bucket, key);
            request.setRange(pos, contentLength);
            return client.getObject(request).getObjectContent();
        }
    }
}
