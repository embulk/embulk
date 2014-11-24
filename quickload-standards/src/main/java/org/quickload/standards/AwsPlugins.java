package org.quickload.standards;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.quickload.config.Task;
import org.quickload.config.Config;

public class AwsPlugins
{
    public static interface CredentialsTask
            extends Task
    {
        @Config("access_key_id")
        public String getAccessKeyId();

        @Config("secret_access_key")
        public String getSecretAccessKey();

        // TODO support more options such as STS
    }

    public static AWSCredentialsProvider getCredentialsProvider(CredentialsTask task)
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
}
