package org.embulk.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Report
        extends DataSource<Report>
{
    public Report()
    {
        super();
    }

    /**
     * visible for DataSourceSerDe
     */
    Report(ObjectNode data)
    {
        super(data);
    }

    @Override
    protected Report newInstance(ObjectNode data)
    {
        return new Report(data);
    }
}
