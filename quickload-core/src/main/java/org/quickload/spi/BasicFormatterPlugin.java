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
import org.quickload.channel.PageInput;
import org.quickload.channel.FileBufferOutput;
import org.quickload.channel.FileBufferChannel;

public abstract class BasicFormatterPlugin
        implements FormatterPlugin
{
    public abstract TaskSource getBasicFormatterTask(ProcTask proc, ConfigSource config);

    public abstract void runBasicFormatter(ProcTask proc,
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

    protected List<FileEncoderPlugin> newFileEncoderPlugins(ProcTask proc, FormatterTask task)
    {
        ImmutableList.Builder<FileEncoderPlugin> builder = ImmutableList.builder();
        for (ConfigSource fileEncoderConfig : task.getFileEncoderConfigs()) {
            builder.add(proc.newPlugin(FileEncoderPlugin.class, fileEncoderConfig.get("type")));
        }
        return builder.build();
    }

    @Override
    public TaskSource getFormatterTask(ProcTask proc, ConfigSource config)
    {
        FormatterTask task = proc.loadConfig(config, FormatterTask.class);
        ImmutableList.Builder<TaskSource> builder = ImmutableList.builder();
        List<FileEncoderPlugin> fencs = newFileEncoderPlugins(proc, task);
        for (int i=0; i < fencs.size(); i++) {
            builder.add(fencs.get(i).getFileEncoderTask(proc, task.getFileEncoderConfigs().get(i)));
        }
        task.setFileEncoderTasks(builder.build());
        task.setBasicFormatterTask(getBasicFormatterTask(proc, config));
        return proc.dumpTask(task);
    }

    @Override
    public void runFormatter(final ProcTask proc,
            TaskSource taskSource, final int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput)
    {
        final FormatterTask task = proc.loadTask(taskSource, FormatterTask.class);
        final List<FileEncoderPlugin> fencs = newFileEncoderPlugins(proc, task);
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

                final FileBufferChannel fdecInputChannel = proc.newFileBufferChannel();
                channels.add(fdecInputChannel);

                final FileBufferOutput fdecOutput = nextOutput;
                final FileBufferChannel fdecOutputChannel = prevChannel;
                PluginThread thread = proc.startPluginThread(new Runnable() {
                    public void run()
                    {
                        try {
                            fdec.runFileEncoder(proc,
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

            runBasicFormatter(proc,
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
