package org.embulk.spi;

public interface EventLogReporter
        extends Reporter
{
    void log(Reporter.ReportLevel level, String eventLog);
    //void log(Reporter.ReportLevel level, Map eventLog); // TODO structure event log
}
