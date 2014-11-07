package org.quickload.standards;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.constraints.NotNull;
import com.google.common.collect.ImmutableList;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
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
import org.quickload.spi.FilePlugins;
import org.quickload.spi.ProcTask;
import org.quickload.spi.ProcControl;

public class S3FileInputPlugin
        extends FileInputPlugin
{
    public interface PluginTask
            extends AWSCredentialsTask, AmazonS3Task
    {
        @Config("in:bucket")
        @NotNull
        public String getBucket();

        @Config("in:paths")
        @NotNull
        public List<String> getPathPrefixes();

        public List<String> getFiles();
        public void setFiles(List<String> files);
    }

    @Override
    public NextConfig runFileInputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control)
    {
        PluginTask task = proc.loadConfig(config, PluginTask.class);

        // list files recursively
        task.setFiles(listFiles(task));

        // number of processors is same with number of files
        proc.setProcessorCount(task.getFiles().size());

        // run
        control.run(proc.dumpTask(task));

        return new NextConfig();
    }

    private static AmazonS3Client createS3Client(PluginTask task)
    {
        AWSCredentialsProvider credentials = AWSPlugins.getCredentialsProvider(task);
        AmazonS3Client client = AWSPlugins.getS3Client(task, credentials);
        return client;
    }

    public List<String> listFiles(PluginTask task)
    {
        AmazonS3Client client = createS3Client(task);
        String bucketName = task.getBucket();

        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (String prefix : task.getPathPrefixes()) {
            // TODO format path using timestamp
            builder.addAll(AWSPlugins.listS3FilesByPrefix(client, bucketName, prefix));
        }

        return builder.build();
    }

    @Override
    public Report runFileInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, FileBufferOutput fileBufferOutput)
    {
        PluginTask task = proc.loadTask(taskSource, PluginTask.class);
        AmazonS3Client client = createS3Client(task);

        String bucket = task.getBucket();
        String key = task.getFiles().get(processorIndex);

        ResumeOpener opener = new ResumeOpener(client, bucket, key);
        try (InputStream in = opener.open(0)) {
            FilePlugins.transferInputStream(proc.getBufferAllocator(), in, fileBufferOutput);
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
