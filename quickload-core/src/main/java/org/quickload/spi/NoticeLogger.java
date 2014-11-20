package org.quickload.spi;

import java.util.List;
import java.util.PriorityQueue;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Collection;
import java.util.Locale;
import java.io.IOException;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.quickload.time.Timestamp;
import org.quickload.config.EnumTask;
import org.quickload.record.Schema;
import org.quickload.record.Page;
import org.quickload.record.Pages;
import org.quickload.record.PageReader;

public class NoticeLogger
{
    public enum Priority
            implements EnumTask
    {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3);

        private final int prio;

        private Priority(int prio)
        {
            this.prio = prio;
        }

        int getPrio()
        {
            return prio;
        }

        @Override
        public String getName()
        {
            return toString().toLowerCase(Locale.ENGLISH);
        }
    }

    public static class Message
    {
        private final Priority priority;
        private final Timestamp timestamp;
        private final String message;

        public Message(Priority priority, Timestamp timestamp, String message)
        {
            this.priority = priority;
            this.timestamp = timestamp;
            this.message = message;
        }

        public Priority getPriority()
        {
            return priority;
        }

        public Timestamp getTimestamp()
        {
            return timestamp;
        }

        public String getMessage()
        {
            return message;
        }

        int size()
        {
            return message.length();
        }
    }

    static class MessagePriorityComparator
            implements Comparator<Message>
    {
        @Override
        public int compare(Message o1, Message o2)
        {
            int prio1 = o1.getPriority().getPrio();
            int prio2 = o1.getPriority().getPrio();
            if (prio1 == prio2) {
                return o1.getTimestamp().compareTo(o2.getTimestamp());
            }
            return prio1 - prio2;
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj != null && MessagePriorityComparator.class.equals(obj.getClass());
        }
    }

    public static class SkippedRecord
    {
        private final String json;  // TODO use msgpack

        public SkippedRecord(String json)
        {
            this.json = json;
        }

        public String getJson()
        {
            return json;
        }

        int size()
        {
            return json.length();
        }
    }

    private final ObjectMapper mapper;

    private final int maxSkippedRecordSize;
    private final PriorityQueue<Message> messages;
    private int messageSize;

    private final int messagePriorityThreshold;
    private final int maxMessageSize;
    private final ArrayDeque<SkippedRecord> skippedRecords;
    private int skippedRecordSize;

    public NoticeLogger(int maxSkippedRecordSize, int maxMessageSize,
            Priority messagePriorityThreshold)
    {
        this.maxSkippedRecordSize = maxSkippedRecordSize;
        this.maxMessageSize = maxMessageSize;
        this.messagePriorityThreshold = messagePriorityThreshold.getPrio();
        this.mapper = new ObjectMapper();
        this.messages = new PriorityQueue<Message>(1, new MessagePriorityComparator());
        this.skippedRecords = new ArrayDeque<SkippedRecord>();
    }

    public synchronized void addAllMessagesTo(Collection<Message> collection)
    {
        collection.addAll(messages);
    }

    public synchronized List<Message> getMessages()
    {
        return ImmutableList.copyOf(messages);
    }

    public synchronized void addAllSkippedRecordsTo(Collection<SkippedRecord> collection)
    {
        collection.addAll(skippedRecords);
    }

    public synchronized List<SkippedRecord> getSkippedRecords()
    {
        return ImmutableList.copyOf(skippedRecords);
    }

    public void skippedPage(Schema schema, Page page)
    {
        try (PageReader reader = new PageReader(schema, ImmutableList.of(page))) {
            while (reader.nextRecord()) {
                skippedRecord(reader);
            }
        }
    }

    public void skippedRecord(PageReader record)
    {
        try {
            String json = mapper.writeValueAsString(Pages.toObjects(record));
            skippedRecord(new SkippedRecord(json));
        } catch (Exception ex) {
        }
    }

    public void skippedLine(String line)
    {
        try {
            ObjectNode record = JsonNodeFactory.instance.objectNode();
            record.put("line", line);
            String json = mapper.writeValueAsString(record);
            skippedRecord(new SkippedRecord(json));
        } catch (Exception ex) {
        }
    }

    private synchronized void skippedRecord(SkippedRecord record)
    {
        skippedRecords.addLast(record);
        skippedRecordSize += record.size();
        while (skippedRecordSize > maxSkippedRecordSize && !skippedRecords.isEmpty()) {
            skippedRecordSize -= skippedRecords.removeFirst().size();
        }
    }

    public void debug(String messageFormat, Object... args)
    {
        if (messagePriorityThreshold <= Priority.DEBUG.getPrio()) {
            return;
        }
        addMessage(new Message(Priority.DEBUG,
                currentTimestamp(),
                String.format(messageFormat, args)));
    }

    public void info(String messageFormat, Object... args)
    {
        if (messagePriorityThreshold <= Priority.INFO.getPrio()) {
            return;
        }
        addMessage(new Message(Priority.INFO,
                currentTimestamp(),
                String.format(messageFormat, args)));
    }

    public void warn(String messageFormat, Object... args)
    {
        if (messagePriorityThreshold <= Priority.WARN.getPrio()) {
            return;
        }
        addMessage(new Message(Priority.WARN,
                currentTimestamp(),
                String.format(messageFormat, args)));
    }

    public void error(String messageFormat, Object... args)
    {
        if (messagePriorityThreshold <= Priority.ERROR.getPrio()) {
            return;
        }
        addMessage(new Message(Priority.ERROR,
                currentTimestamp(),
                String.format(messageFormat, args)));
    }

    private Timestamp currentTimestamp()
    {
        return Timestamp.ofEpochMilli(System.currentTimeMillis());
    }

    private synchronized void addMessage(Message message)
    {
        messages.add(message);
        messageSize += message.size();
        while (messageSize > maxMessageSize && !messages.isEmpty()) {
            messageSize -= messages.poll().size();
        }
    }
}
