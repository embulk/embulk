package org.embulk.exec;

import java.util.List;
import org.embulk.record.Schema;
import org.embulk.record.Page;
import org.embulk.spi.NoticeLogger;

public class PreviewResult
{
    private final Schema schema;
    private final List<Page> pages;
    private final List<NoticeLogger.Message> noticeMessages;
    private final List<NoticeLogger.SkippedRecord> skippedRecords;

    public PreviewResult(Schema schema, List<Page> pages,
            List<NoticeLogger.Message> noticeMessages,
            List<NoticeLogger.SkippedRecord> skippedRecords)
    {
        this.schema = schema;
        this.pages = pages;
        this.noticeMessages = noticeMessages;
        this.skippedRecords = skippedRecords;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public List<Page> getPages()
    {
        return pages;
    }

    public List<NoticeLogger.Message> getNoticeMessages()
    {
        return noticeMessages;
    }

    public List<NoticeLogger.SkippedRecord> getSkippedRecords()
    {
        return skippedRecords;
    }
}
