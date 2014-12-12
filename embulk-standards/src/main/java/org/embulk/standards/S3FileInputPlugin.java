package org.embulk.standards;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import com.google.common.collect.ImmutableList;
import com.google.common.base.Optional;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import org.embulk.channel.FileBufferOutput;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.config.TaskSource;
import org.embulk.spi.ExecControl;
import org.embulk.spi.ExecTask;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FilePlugins;

public class S3FileInputPlugin
        extends FileInputPlugin
{
    public interface PluginTask
            extends AwsPlugins.CredentialsTask
    {
        @Config("bucket")
        public String getBucket();

        @Config("paths")
        public List<String> getPathPrefixes();

        @Config("endpoint")
        public Optional<String> getEndpoint();

        // TODO timeout, ssl, etc

        public List<String> getFiles();
        public void setFiles(List<String> files);
    }

    @Override
    public NextConfig runFileInputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control)
    {
        PluginTask task = exec.loadConfig(config, PluginTask.class);

        // list files recursively
        task.setFiles(listFiles(task));

        // number of processors is same with number of files
        exec.setProcessorCount(task.getFiles().size());

        // run
        control.run(exec.dumpTask(task));

        return new NextConfig();
    }

    private static AmazonS3Client newS3Client(PluginTask task)
    {
        AWSCredentialsProvider credentials = AwsPlugins.getCredentialsProvider(task);
        AmazonS3Client client = newS3Client(credentials, task.getEndpoint());
        return client;
    }

    private static AmazonS3Client newS3Client(AWSCredentialsProvider credentials,
            Optional<String> endpoint)
    {
        // TODO get config from AmazonS3Task
        ClientConfiguration clientConfig = new ClientConfiguration();
        //clientConfig.setProtocol(Protocol.HTTP);
        clientConfig.setMaxConnections(50); // SDK default: 50
        clientConfig.setMaxErrorRetry(3); // SDK default: 3
        clientConfig.setSocketTimeout(8*60*1000); // SDK default: 50*1000

        AmazonS3Client client = new AmazonS3Client(credentials, clientConfig);

        if (endpoint.isPresent()) {
            client.setEndpoint(endpoint.get());
        }

        return client;
    }

    public List<String> listFiles(PluginTask task)
    {
        AmazonS3Client client = newS3Client(task);
        String bucketName = task.getBucket();

        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (String prefix : task.getPathPrefixes()) {
            // TODO format path using timestamp
            builder.addAll(listS3FilesByPrefix(client, bucketName, prefix));
        }

        return builder.build();
    }

    /**
     * Lists S3 filenames filtered by prefix.
     *
     * The resulting list does not include the file that's size == 0.
     */
    public static List<String> listS3FilesByPrefix(AmazonS3Client client, String bucketName, String prefix)
    {
        ImmutableList.Builder<String> builder = ImmutableList.builder();

        String lastKey = null;
        do {
            ListObjectsRequest req = new ListObjectsRequest(bucketName, prefix, lastKey, null, 1024);
            ObjectListing ol = client.listObjects(req);
            for(S3ObjectSummary s : ol.getObjectSummaries()) {
                if (s.getSize() > 0) {
                    builder.add(s.getKey());
                }
            }
            lastKey = ol.getNextMarker();
        } while(lastKey != null);

        return builder.build();
    }

    @Override
    public Report runFileInput(ExecTask exec, TaskSource taskSource,
            int processorIndex, FileBufferOutput fileBufferOutput)
    {
        PluginTask task = exec.loadTask(taskSource, PluginTask.class);
        AmazonS3Client client = newS3Client(task);

        String bucket = task.getBucket();
        String key = task.getFiles().get(processorIndex);

        ResumeOpener opener = new ResumeOpener(client, bucket, key);
        try (InputStream in = opener.open(0)) {
            FilePlugins.transferInputStream(exec.getBufferAllocator(), in, fileBufferOutput);
            // TODO catch PartialTransferException and implement retry
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return new Report();
    }

    public static class ResumeOpener
    {
        private AmazonS3Client client;
        private String bucket;
        private String key;
        private long contentLength;

        public ResumeOpener(AmazonS3Client client, String bucket, String key)
        {
            this.client = client;
            this.bucket = bucket;
            this.key = key;
        }

        public InputStream open(long pos)
        {
            GetObjectRequest request = new GetObjectRequest(bucket, key);
            if (pos > 0) {
                request.setRange(pos, contentLength);
            }
            S3Object obj = client.getObject(request);
            if (pos <= 0) {
                // first call
                contentLength = obj.getObjectMetadata().getContentLength();
            }
            return obj.getObjectContent();
        }
    }
}
