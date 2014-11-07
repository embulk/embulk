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
    public abstract TaskSource getBasicParserTask(ProcTask proc, ConfigSource config);

    public abstract void runBasicParser(ProcTask proc,
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

    protected List<FileDecoderPlugin> newFileDecoderPlugins(ProcTask proc, ParserTask task)
    {
        ImmutableList.Builder<FileDecoderPlugin> builder = ImmutableList.builder();
        for (ConfigSource fileDecoderConfig : task.getFileDecoderConfigs()) {
            builder.add(proc.newPlugin(FileDecoderPlugin.class, fileDecoderConfig.get("type")));
        }
        return builder.build();
    }

    @Override
    public TaskSource getParserTask(ProcTask proc, ConfigSource config)
    {
        ParserTask task = proc.loadConfig(config, ParserTask.class);
        ImmutableList.Builder<TaskSource> builder = ImmutableList.builder();
        List<FileDecoderPlugin> fdecs = newFileDecoderPlugins(proc, task);
        for (int i=0; i < fdecs.size(); i++) {
            builder.add(fdecs.get(i).getFileDecoderTask(proc, task.getFileDecoderConfigs().get(i)));
        }
        task.setFileDecoderTasks(builder.build());
        task.setBasicParserTask(getBasicParserTask(proc, config));
        return proc.dumpTask(task);
    }

    @Override
    public void runParser(final ProcTask proc,
            TaskSource taskSource, final int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput)
    {
        final ParserTask task = proc.loadTask(taskSource, ParserTask.class);
        final List<FileDecoderPlugin> fdecs = newFileDecoderPlugins(proc, task);
        final List<TaskSource> ftasks = task.getFileDecoderTasks();

        List<FileBufferChannel> channels = new ArrayList<FileBufferChannel>();
        FileBufferInput nextInput = fileBufferInput;
        FileBufferChannel prevChannel = null;
        try {
            for (int i=0; i < fdecs.size(); i++) {
                final FileDecoderPlugin fdec = fdecs.get(i);
                final TaskSource ftask = ftasks.get(i);

                final FileBufferChannel fdecOutputChannel = proc.newFileBufferChannel();
                channels.add(fdecOutputChannel);

                final FileBufferInput fdecInput = nextInput;
                final FileBufferChannel fdecInputChannel = prevChannel;
                proc.startPluginThread(new PluginThread() {
                    public void run()
                    {
                        try {
                            fdec.runFileDecoder(proc,
                                    ftask, processorIndex,
                                    fdecInput, fdecOutputChannel.getOutput());
                        } finally {
                            try {
                                fdecOutputChannel.completeProducer();
                            } finally {
                                if (fdecInputChannel != null) {
                                    fdecInputChannel.completeConsumer();
                                    fdecInputChannel.join();
                                }
                            }
                        }
                    }
                });

                prevChannel = fdecOutputChannel;
                nextInput = fdecOutputChannel.getInput();
            }

            runBasicParser(proc,
                    task.getBasicParserTask(), processorIndex,
                    nextInput, pageOutput);
        } finally {
            if (prevChannel != null) {
                prevChannel.completeConsumer();
                prevChannel.join();
            }
        }
    }
}
