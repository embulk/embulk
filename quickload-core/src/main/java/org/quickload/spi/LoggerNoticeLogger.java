package org.quickload.spi;

import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.quickload.record.Schema;
import org.quickload.record.Page;
import org.quickload.record.Pages;
import org.quickload.record.PageReader;

public class LoggerNoticeLogger
        implements NoticeLogger
{
    private final Log log;

    public LoggerNoticeLogger(Log log)
    {
        this.log = log;
    }

    public void skippedPage(Schema schema, Page page)
    {
        for (Object[] objects : Pages.toObjects(schema, page)) {
            log.error(String.format("Skipped record: %s", Arrays.asList(objects)));
        }
    }

    public void skippedRecord(PageReader record)
    {
        Object[] objects = Pages.toObjects(record);
        log.error(String.format("Skipped record: %s", Arrays.asList(objects)));
    }

    public void skippedLine(String line)
    {
        log.error(String.format("Skipped line: %s", line));
    }

    public void info(String message)
    {
        log.info(message);
    }

    public void warn(String message)
    {
        log.warn(message);
    }

    public void fatal(String message)
    {
        log.fatal(message);
    }
}
