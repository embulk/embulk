package org.quickload.spi;

import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.quickload.record.Schema;
import org.quickload.record.Page;
import org.quickload.record.Pages;
import org.quickload.record.PageReader;

public class NullNoticeLogger
        implements NoticeLogger
{
    public void skippedPage(Schema schema, Page page) { }

    public void skippedRecord(PageReader record) { }

    public void skippedLine(String line) { }

    public void info(String message) { }

    public void warn(String message) { }

    public void fatal(String message) { }
}
