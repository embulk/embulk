package org.quickload.standards;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;

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
}
