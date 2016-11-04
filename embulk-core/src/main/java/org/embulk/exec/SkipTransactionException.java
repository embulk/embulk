package org.embulk.exec;

import org.embulk.config.ConfigDiff;

// Input/output plugins might need to stop Embulk before the transaction starts by depending
// on the conditions of input/output data sources/destinations. They can throw this exception
// if they want to do that. Embulk handles it and then stops the transaction.
public class SkipTransactionException
        extends RuntimeException
{
    private final ConfigDiff configDiff;

    public SkipTransactionException(ConfigDiff configDiff)
    {
        super();
        this.configDiff = configDiff;
    }

    public ConfigDiff getConfigDiff()
    {
        return configDiff;
    }
}
