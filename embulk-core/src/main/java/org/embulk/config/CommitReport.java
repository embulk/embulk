package org.embulk.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class CommitReport
        extends DataSource<CommitReport>
{
    public CommitReport(ModelManager model)
    {
        super(model);
    }

    // visible for DataSourceSerDe
    CommitReport(ModelManager model, ObjectNode data)
    {
        super(model, data);
    }

    @Override
    protected CommitReport newInstance(ModelManager model, ObjectNode data)
    {
        return new CommitReport(model, data);
    }
}
