package org.embulk.spi;

import static org.junit.Assert.assertEquals;
import static org.embulk.record.PageTestUtils.newColumn;
import static org.embulk.record.PageTestUtils.newSchema;
import static org.embulk.record.Types.BOOLEAN;
import static org.embulk.record.Types.DOUBLE;
import static org.embulk.record.Types.LONG;
import static org.embulk.record.Types.STRING;
import static org.embulk.record.Types.TIMESTAMP;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.embulk.GuiceJUnitRunner;
import org.embulk.TestRuntimeBinder;
import org.embulk.TestUtilityModule;
import org.embulk.channel.PageChannel;
import org.embulk.exec.BufferManager;
import org.embulk.record.PageBuilder;
import org.embulk.record.RecordWriter;
import org.embulk.record.Schema;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ TestUtilityModule.class })
public class TestNoticeLogger
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    @Test
    public void testMessageThreshold()
    {
        NoticeLogger subject = new NoticeLogger(1024, 1024,
                NoticeLogger.Priority.DEBUG);
        assertEquals(0, subject.getMessages().size());
        addTestMessages(subject);
        assertEquals(4, subject.getMessages().size());
        assertEquals("error: 5 あ", subject.getMessages().get(0).getMessage());
        assertEquals("debug: 1", subject.getMessages().get(3).getMessage());

        subject = new NoticeLogger(10, 1024, NoticeLogger.Priority.INFO);
        addTestMessages(subject);
        assertEquals(3, subject.getMessages().size());
        assertEquals("info: 2 3", subject.getMessages().get(2).getMessage());

        subject = new NoticeLogger(10, 1024, NoticeLogger.Priority.WARN);
        addTestMessages(subject);
        assertEquals(2, subject.getMessages().size());
        assertEquals("warn: 4", subject.getMessages().get(1).getMessage());

        subject = new NoticeLogger(1024, 1024, NoticeLogger.Priority.ERROR);
        addTestMessages(subject);
        assertEquals(1, subject.getMessages().size());
        assertEquals("error: 5 あ", subject.getMessages().get(0).getMessage());
    }

    @Test
    public void testAddAllMessages()
    {
        NoticeLogger base = new NoticeLogger(1024, 1024,
                NoticeLogger.Priority.DEBUG);
        addTestMessages(base, "addAllMessage0");
        addTestMessages(base, "addAllMessage1");
        addTestMessages(base, "addAllMessage2");

        NoticeLogger subject = new NoticeLogger(1024, 1024,
                NoticeLogger.Priority.ERROR);
        subject.addAllMessagesTo(base.getMessages());
        assertEquals(3, subject.getMessages().size());
    }

    @Test
    public void testMaxMessageSize()
    {
        // max == 9 bytes
        NoticeLogger subject = new NoticeLogger(1024, 9,
                NoticeLogger.Priority.DEBUG);
        // add 3 x 3: 9 bytes
        subject.debug("123");
        subject.debug("456");
        subject.debug("789");
        assertEquals(3, subject.getMessages().size());
        // push out 1 message: 9 + 1 - 3 = 7 bytes
        subject.info("0");
        assertEquals(3, subject.getMessages().size());
        // safely added to the rest: 7 + 1 + 1 = 9 bytes
        subject.info("あ"); // multibyte char
        subject.info("い");
        assertEquals(5, subject.getMessages().size());
        // push out 4 messages: 9 + 8 - 3 - 3 - 1 - 1 = 9 bytes
        subject.warn("12345678");
        assertEquals(2, subject.getMessages().size());
        // flushes all: 9 + 10 - 1 - 8 - 10 = 0 bytes
        subject.error("1234567890");
        assertEquals(0, subject.getMessages().size());
    }

    @Test
    public void testMessagesAreSortedByPriorityAndTime()
            throws InterruptedException
    {
        NoticeLogger subject = new NoticeLogger(1024, 1024,
                NoticeLogger.Priority.DEBUG);
        addTestMessages(subject, "prioTest0");
        Thread.sleep(50);
        addTestMessages(subject, "prioTest1");
        Thread.sleep(50);
        addTestMessages(subject, "prioTest2");
        List<NoticeLogger.Message> messages = subject.getMessages();
        assertEquals("error: prioTest2", messages.get(0).getMessage());
        assertEquals("error: prioTest1", messages.get(1).getMessage());
        assertEquals("error: prioTest0", messages.get(2).getMessage());
    }

    @Test
    public void testSkippedLine()
    {
        NoticeLogger subject = new NoticeLogger(1024, 1024,
                NoticeLogger.Priority.DEBUG);
        subject.skippedLine("abc");
        subject.skippedLine("def");
        subject.skippedLine("ghi");
        assertEquals(3, subject.getSkippedRecords().size());
        assertEquals("{\"line\":\"def\"}", subject.getSkippedRecords().get(1)
                .getJson().toString());
    }
    
    @Test
    public void testSkippedPage()
    {
        NoticeLogger subject = new NoticeLogger(1024, 1024,
                NoticeLogger.Priority.DEBUG);

        PageChannel channel = new PageChannel(Integer.MAX_VALUE);
        try {
            Schema schema = newSchema(newColumn("c1", BOOLEAN),
                    newColumn("c2", LONG), newColumn("c3", DOUBLE),
                    newColumn("c4", STRING), newColumn("c5", TIMESTAMP));
            PageBuilder builder = new PageBuilder(
                    binder.getInstance(BufferManager.class), schema,
                    channel.getOutput());
            RecordWriter dummyRecordWriter = new MockRecordWriter();
            builder.addRecord(dummyRecordWriter);
            builder.addRecord(dummyRecordWriter);
            builder.addRecord(dummyRecordWriter);
            builder.close();
            channel.completeProducer();
            subject.skippedPage(schema, channel.getInput().iterator().next());
            assertEquals(3, subject.getSkippedRecords().size());
        } finally {
            channel.close();
        }
    }

    @Test
    public void testMaxSkippedRecordSize()
    {
        // 100 bytes in JSON
        NoticeLogger subject = new NoticeLogger(100, 1024,
                NoticeLogger.Priority.DEBUG);

        PageChannel channel = new PageChannel(Integer.MAX_VALUE);
        try {
            Schema schema = newSchema(newColumn("c1", BOOLEAN),
                    newColumn("c2", LONG), newColumn("c3", DOUBLE),
                    newColumn("c4", STRING), newColumn("c5", TIMESTAMP));
            PageBuilder builder = new PageBuilder(
                    binder.getInstance(BufferManager.class), schema,
                    channel.getOutput());
            RecordWriter dummyRecordWriter = new MockRecordWriter();
            builder.addRecord(dummyRecordWriter);
            builder.addRecord(dummyRecordWriter);
            builder.addRecord(dummyRecordWriter);
            builder.close();
            channel.completeProducer();
            subject.skippedPage(schema, channel.getInput().iterator().next());
            assertEquals(2, subject.getSkippedRecords().size());
        } finally {
            channel.close();
        }
    }
    
    private void addTestMessages(NoticeLogger subject)
    {
        subject.debug("debug: %d", 1);
        subject.info("info: %s %s", "2", "3");
        subject.warn("warn: %d", 4);
        subject.error("error: %s %s", "5", "あ");
    }

    private void addTestMessages(NoticeLogger subject, String format,
            Object... args)
    {
        subject.debug("debug: " + format, args);
        subject.info("info: " + format, args);
        subject.warn("warn: " + format, args);
        subject.error("error: " + format, args);
    }
}
