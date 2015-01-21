package org.embulk.exec;

import java.util.List;
import org.embulk.config.NextConfig;
//import org.embulk.spi.NoticeLogger;

public class ExecuteResult
{
    private final NextConfig nextConfig;
    //private final List<NoticeLogger.Message> noticeMessages;
    //private final List<NoticeLogger.SkippedRecord> skippedRecords;

    public ExecuteResult(NextConfig nextConfig/*,
            List<NoticeLogger.Message> noticeMessages,
            List<NoticeLogger.SkippedRecord> skippedRecords*/)
    {
        this.nextConfig = nextConfig;
        //this.noticeMessages = noticeMessages;
        //this.skippedRecords = skippedRecords;
    }

    public NextConfig getNextConfig()
    {
        return nextConfig;
    }

    //public List<NoticeLogger.Message> getNoticeMessages()
    //{
    //    return noticeMessages;
    //}

    //public List<NoticeLogger.SkippedRecord> getSkippedRecords()
    //{
    //    return skippedRecords;
    //}
}
