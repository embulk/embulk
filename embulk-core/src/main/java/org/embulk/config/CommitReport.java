package org.embulk.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class CommitReport
        extends DataSource<CommitReport>
{
    public CommitReport()
    {
        super();
    }

    /**
     * visible for DataSourceSerDe
     */
    CommitReport(ObjectNode data)
    {
        super(data);
    }

    @Override
    protected CommitReport newInstance(ObjectNode data)
    {
        return new CommitReport(data);
    }
}
