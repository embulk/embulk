package org.embulk.spi;

import org.joda.time.DateTimeZone;
import com.google.common.base.Optional;
import com.google.inject.Injector;
import com.fasterxml.jackson.databind.JsonNode;
import org.jruby.embed.ScriptingContainer;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.ModelManager;
import org.embulk.plugin.PluginManager;
import org.embulk.exec.BufferManager;
import org.embulk.buffer.BufferAllocator;
import org.embulk.record.PageAllocator;
import org.embulk.channel.BufferChannel;
import org.embulk.channel.FileBufferChannel;
import org.embulk.channel.PageChannel;
import org.embulk.time.Timestamp;
import org.embulk.time.TimestampFormatter;
import org.embulk.time.TimestampParser;
import org.embulk.time.TimestampParserTask;

public class ExecTask
        extends ExecConfig
{
    private static interface ExecTaskConfig
            extends Task
    {
        @Config("max_skip_records")
        @ConfigDefault("100")
        public int getMaxSkipRecords();  // TODO not used

        @Config("notice_message_size_limit")
        @ConfigDefault("8388608")
        public int getNoticeMessageSizeLimit();  // TODO not used

        @Config("log_priority")
        @ConfigDefault("\"INFO\"")
        public NoticeLogger.Priority getLogPriority();

        @Config("transaction_time")
        @ConfigDefault("null")
        public Optional<Timestamp> getTransactionTime();

        @Config("unique_transaction_name")
        @ConfigDefault("null")
        public Optional<String> getUniqueTransactionName();
    }

    public static ExecTask loadFromConfig(Injector injector, ConfigSource config)
    {
        ModelManager modelManager = injector.getInstance(ModelManager.class);
        ExecTaskConfig task = modelManager.readTaskConfig(config.getObjectOrSetEmpty("exec"), ExecTaskConfig.class);

        ExecTask exec = new ExecTask(injector, new NoticeLogger(
                    task.getMaxSkipRecords(), task.getNoticeMessageSizeLimit(),
                    task.getLogPriority()));

        Timestamp tranTime = task.getTransactionTime()
            .or(Timestamp.ofEpochMilli(System.currentTimeMillis()));
        exec.setTransactionTime(tranTime);

        String uniq = task.getUniqueTransactionName()
            .or(String.format("tran.%s", tranTime.toString()));
        exec.setUniqueTransactionName(uniq);

        // TODO get default time zone from config?

        return exec;
    }

    private final Injector injector;
    private final ModelManager modelManager;
    private final PluginManager pluginManager;
    private final BufferManager bufferManager;
    private final NoticeLogger noticeLogger;

    ExecTask(Injector injector, NoticeLogger noticeLogger)
    {
        super();
        this.injector = injector;
        this.noticeLogger = noticeLogger;
        this.modelManager = injector.getInstance(ModelManager.class);
        this.pluginManager = injector.getInstance(PluginManager.class);
        this.bufferManager = injector.getInstance(BufferManager.class);
    }

    public ExecTask(Injector injector, NoticeLogger noticeLogger, ExecConfig execConfig)
    {
        this(injector, noticeLogger);
        set(execConfig);
    }

    public <T extends Task> T loadConfig(ConfigSource config, Class<T> taskType)
    {
        return modelManager.readTaskConfig(config, taskType);
    }

    public <T extends Task> T loadTask(TaskSource taskSource, Class<T> taskType)
    {
        return modelManager.readObject(taskSource, taskType);
    }

    public TaskSource dumpTask(Task task)
    {
        return modelManager.writeAsTaskSource(task);
    }

    public PageAllocator getPageAllocator()
    {
        return bufferManager;
    }

    public BufferAllocator getBufferAllocator()
    {
        return bufferManager;
    }

    public Injector getInjector()
    {
        return injector;
    }

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig)
    {
        return pluginManager.newPlugin(iface, typeConfig);
    }

    public NoticeLogger notice()
    {
        return noticeLogger;
    }

    public PluginThread startPluginThread(Runnable runnable)
    {
        // TODO use cached thread pool
        // TODO inject thread pool manager
        // TODO set thread name
        return PluginThread.start(runnable);
    }

    public BufferChannel newBufferChannel()
    {
        return new BufferChannel(32*1024*1024);  // TODO configurable buffer size
    }

    public FileBufferChannel newFileBufferChannel()
    {
        return new FileBufferChannel(32*1024*1024);  // TODO configurable buffer size
    }

    public PageChannel newPageChannel()
    {
        return new PageChannel(32*1024*1024);  // TODO configurable buffer size
    }

    public TimestampFormatter newTimestampFormatter(String format, DateTimeZone timeZone)
    {
        return new TimestampFormatter(injector.getInstance(ScriptingContainer.class), format, timeZone);
    }

    public TimestampParser newTimestampParser(String format, TimestampParserTask parserTask)
    {
        return new TimestampParser(injector.getInstance(ScriptingContainer.class), format, parserTask);
    }
}
