package org.quickload.standards;

import java.util.List;
import com.google.common.collect.ImmutableList;
import com.amazonaws.Protocol;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.ObjectListing;

public class AWSPlugins
{
    public static AWSCredentialsProvider getCredentialsProvider(AWSCredentialsTask task)
    {
        final AWSCredentials cred = new BasicAWSCredentials(
                task.getAccessKeyId(), task.getSecretAccessKey());
        return new AWSCredentialsProvider() {
            public AWSCredentials getCredentials()
            {
                return cred;
            }

            public void refresh()
            {
            }
        };
    }

    public static AmazonS3Client getS3Client(AmazonS3Task task,
            AWSCredentialsProvider credentials)
    {
        // TODO get config from AmazonS3Task
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

    public static List<String> listS3FilesByPrefix(AmazonS3Client client, String bucketName, String prefix)
    {
        ImmutableList.Builder<String> builder = ImmutableList.builder();

        String lastKey = null;
        do {
            ListObjectsRequest req = new ListObjectsRequest(bucketName, prefix, lastKey, null, 1024);
            ObjectListing ol = client.listObjects(req);
            for(S3ObjectSummary s : ol.getObjectSummaries()) {
                builder.add(s.getKey());
            }
            lastKey = ol.getNextMarker();
        } while(lastKey != null);

        return builder.build();
    }
}
