package org.embulk.spi;

import org.embulk.spi.util.Reporters;

public interface Reporter
{
    void skip(String skipped); // TODO should be changed
    //void skip(Buffer errorBufferData);
    //void skip(Record errorPageData);

    void log(Reporters.ReportLevel level, String eventLog);
    //void log(Reporter.ReportLevel level, Map eventLog); // TODO structure event log

    void close(); // TODO should consider about the return type

    void cleanup(); // TODO should return TaskReport??
}
