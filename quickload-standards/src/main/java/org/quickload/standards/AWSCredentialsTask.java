package org.quickload.standards;

import org.quickload.config.Config;
import org.quickload.config.Task;
import javax.validation.constraints.NotNull;

public interface AWSCredentialsTask
        extends Task
{
    @Config("access_key_id")
    @NotNull
    public String getAccessKeyId();

    @Config("secret_access_key")
    @NotNull
    public String getSecretAccessKey();

    // TODO support more options
}
