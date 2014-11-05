package org.quickload.config;

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
}
