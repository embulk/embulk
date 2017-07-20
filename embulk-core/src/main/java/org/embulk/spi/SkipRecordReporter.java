package org.embulk.spi;

public interface SkipRecordReporter
        extends Reporter
{
    void skip(String skipped); // TODO should be changed
    //void skip(Buffer errorBufferData);
    //void skip(Record errorPageData);
}
