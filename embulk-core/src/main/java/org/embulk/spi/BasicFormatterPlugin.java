package org.embulk.spi;

import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import javax.validation.constraints.NotNull;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.channel.PageInput;
import org.embulk.channel.FileBufferOutput;
import org.embulk.channel.FileBufferChannel;

public abstract class BasicFormatterPlugin
        implements FormatterPlugin
{
    public abstract TaskSource getBasicFormatterTask(ExecTask exec, ConfigSource config);

    public abstract void runBasicFormatter(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput);

    public interface FormatterTask
            extends Task
    {
        @Config("file_encoders")
        @ConfigDefault("[]")
        @NotNull
        public List<ConfigSource> getFileEncoderConfigs();

        public List<TaskSource> getFileEncoderTasks();
        public void setFileEncoderTasks(List<TaskSource> sources);

        public TaskSource getBasicFormatterTask();
        public void setBasicFormatterTask(TaskSource source);
    }

    protected List<FileEncoderPlugin> newFileEncoderPlugins(ExecTask exec, FormatterTask task)
    {
        ImmutableList.Builder<FileEncoderPlugin> builder = ImmutableList.builder();
        for (ConfigSource fileEncoderConfig : task.getFileEncoderConfigs()) {
            builder.add(exec.newPlugin(FileEncoderPlugin.class, fileEncoderConfig.get("type")));
        }
        return builder.build();
    }

    @Override
    public TaskSource getFormatterTask(ExecTask exec, ConfigSource config)
    {
        FormatterTask task = exec.loadConfig(config, FormatterTask.class);
        ImmutableList.Builder<TaskSource> builder = ImmutableList.builder();
        List<FileEncoderPlugin> fencs = newFileEncoderPlugins(exec, task);
        for (int i=0; i < fencs.size(); i++) {
            builder.add(fencs.get(i).getFileEncoderTask(exec, task.getFileEncoderConfigs().get(i)));
        }
        task.setFileEncoderTasks(builder.build());
        task.setBasicFormatterTask(getBasicFormatterTask(exec, config));
        return exec.dumpTask(task);
    }

    @Override
    public void runFormatter(final ExecTask exec,
            TaskSource taskSource, final int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput)
    {
        final FormatterTask task = exec.loadTask(taskSource, FormatterTask.class);
        final List<FileEncoderPlugin> fencs = newFileEncoderPlugins(exec, task);
        final List<TaskSource> ftasks = task.getFileEncoderTasks();

        List<FileBufferChannel> channels = new ArrayList<FileBufferChannel>();
        List<PluginThread> threads = new ArrayList<PluginThread>();
        Throwable error = null;
        FileBufferOutput nextOutput = fileBufferOutput;
        FileBufferChannel prevChannel = null;
        try {
            for (int i=fencs.size() - 1; i >= 0; i--) {
                final FileEncoderPlugin fdec = fencs.get(i);
                final TaskSource ftask = ftasks.get(i);

                final FileBufferChannel fdecInputChannel = exec.newFileBufferChannel();
                channels.add(fdecInputChannel);

                final FileBufferOutput fdecOutput = nextOutput;
                final FileBufferChannel fdecOutputChannel = prevChannel;
                PluginThread thread = exec.startPluginThread(new Runnable() {
                    public void run()
                    {
                        try {
                            fdec.runFileEncoder(exec,
                                    ftask, processorIndex,
                                    fdecInputChannel.getInput(), fdecOutput);
                        } finally {
                            try {
                                fdecInputChannel.completeConsumer();
                            } finally {
                                if (fdecOutputChannel != null) {
                                    fdecOutputChannel.completeProducer();
                                    fdecOutputChannel.join();  // throws if channel is not fully consumed
                                }
                            }
                        }
                    }
                });
                threads.add(thread);

                prevChannel = fdecInputChannel;
                nextOutput = fdecInputChannel.getOutput();
            }

            runBasicFormatter(exec,
                    task.getBasicFormatterTask(), processorIndex,
                    pageInput, nextOutput);

            if (prevChannel != null) {
                prevChannel.completeProducer();
                prevChannel.join();
            }
            for (PluginThread thread : threads) {
                thread.join();
            }

        } catch (Throwable ex) {
            error = ex;
        } finally {
            if (prevChannel != null) {
                prevChannel.completeProducer();
            }
            PluginThread.joinAndThrowNested(threads, error);
        }
    }
}
