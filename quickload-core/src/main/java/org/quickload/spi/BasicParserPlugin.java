package org.quickload.spi;

import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import javax.validation.constraints.NotNull;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.channel.PageOutput;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.FileBufferChannel;

public abstract class BasicParserPlugin
        implements ParserPlugin
{
    public abstract TaskSource getBasicParserTask(ExecTask exec, ConfigSource config);

    public abstract void runBasicParser(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput);

    public interface ParserTask
            extends Task
    {
        @Config("file_decoders")
        @ConfigDefault("[]")
        @NotNull
        public List<ConfigSource> getFileDecoderConfigs();

        public List<TaskSource> getFileDecoderTasks();
        public void setFileDecoderTasks(List<TaskSource> sources);

        public TaskSource getBasicParserTask();
        public void setBasicParserTask(TaskSource source);
    }

    protected List<FileDecoderPlugin> newFileDecoderPlugins(ExecTask exec, ParserTask task)
    {
        ImmutableList.Builder<FileDecoderPlugin> builder = ImmutableList.builder();
        for (ConfigSource fileDecoderConfig : task.getFileDecoderConfigs()) {
            builder.add(exec.newPlugin(FileDecoderPlugin.class, fileDecoderConfig.get("type")));
        }
        return builder.build();
    }

    @Override
    public TaskSource getParserTask(ExecTask exec, ConfigSource config)
    {
        ParserTask task = exec.loadConfig(config, ParserTask.class);
        ImmutableList.Builder<TaskSource> builder = ImmutableList.builder();
        List<FileDecoderPlugin> fdecs = newFileDecoderPlugins(exec, task);
        for (int i=0; i < fdecs.size(); i++) {
            builder.add(fdecs.get(i).getFileDecoderTask(exec, task.getFileDecoderConfigs().get(i)));
        }
        task.setFileDecoderTasks(builder.build());
        task.setBasicParserTask(getBasicParserTask(exec, config));
        return exec.dumpTask(task);
    }

    @Override
    public void runParser(final ExecTask exec,
            TaskSource taskSource, final int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput)
    {
        final ParserTask task = exec.loadTask(taskSource, ParserTask.class);
        final List<FileDecoderPlugin> fdecs = newFileDecoderPlugins(exec, task);
        final List<TaskSource> ftasks = task.getFileDecoderTasks();

        List<FileBufferChannel> channels = new ArrayList<FileBufferChannel>();
        List<PluginThread> threads = new ArrayList<PluginThread>();
        Throwable error = null;
        FileBufferInput nextInput = fileBufferInput;
        FileBufferChannel prevChannel = null;
        try {
            for (int i=0; i < fdecs.size(); i++) {
                final FileDecoderPlugin fdec = fdecs.get(i);
                final TaskSource ftask = ftasks.get(i);

                final FileBufferChannel fdecOutputChannel = exec.newFileBufferChannel();
                channels.add(fdecOutputChannel);

                final FileBufferInput fdecInput = nextInput;
                final FileBufferChannel fdecInputChannel = prevChannel;
                PluginThread thread = exec.startPluginThread(new Runnable() {
                    public void run()
                    {
                        try {
                            fdec.runFileDecoder(exec,
                                    ftask, processorIndex,
                                    fdecInput, fdecOutputChannel.getOutput());
                        } finally {
                            try {
                                fdecOutputChannel.completeProducer();
                            } finally {
                                if (fdecInputChannel != null) {
                                    fdecInputChannel.completeConsumer();
                                    fdecInputChannel.join();  // throws if channel is not fully consumed
                                }
                            }
                        }
                    }
                });
                threads.add(thread);

                prevChannel = fdecOutputChannel;
                nextInput = fdecOutputChannel.getInput();
            }

            runBasicParser(exec,
                    task.getBasicParserTask(), processorIndex,
                    nextInput, pageOutput);

            if (prevChannel != null) {
                prevChannel.completeConsumer();
                prevChannel.join();
            }
            for (PluginThread thread : threads) {
                thread.join();
            }

        } catch (Throwable ex) {
            error = ex;
        } finally {
            if (prevChannel != null) {
                prevChannel.completeConsumer();
            }
            PluginThread.joinAndThrowNested(threads, error);
        }
    }
}
