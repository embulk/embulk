package org.quickload.standards;

import org.quickload.config.Config;
import org.quickload.config.Task;

public interface AmazonS3Task
        extends Task
{
    @Config("in:endpoint")
    public String getEndpoint();

    // TODO timeout, etc
}
